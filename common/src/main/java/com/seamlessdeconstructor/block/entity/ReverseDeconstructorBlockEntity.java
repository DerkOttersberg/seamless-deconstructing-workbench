package com.seamlessdeconstructor.block.entity;

import com.derko.seamlessapi.DeconstructionAPI;
import com.derko.seamlessapi.api.deconstruction.DeconstructionContext;
import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.logic.DeconstructionOutputRules;
import com.seamlessdeconstructor.logic.DeconstructionPlan;
import com.seamlessdeconstructor.logic.DeconstructionResolver;
import com.seamlessdeconstructor.logic.OutputSlotPlanner;
import com.seamlessdeconstructor.logic.PendingDeconstructionOperation;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreenHandler;
import com.seamlessdeconstructor.util.ImplementedInventory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ReverseDeconstructorBlockEntity extends BlockEntity implements ImplementedInventory, MenuProvider, WorldlyContainer {
    public static final int INPUT_SLOT = 0;
    public static final int BOOK_SLOT = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_END = 7;

    public static final int MACHINE_IDLE = 0;
    public static final int MACHINE_PROCESSING = 1;
    public static final int MACHINE_BLOCKED = 2;

    public static final int BLOCK_REASON_NONE = 0;
    public static final int BLOCK_REASON_MISSING_BOOK = 1;
    public static final int BLOCK_REASON_OUTPUT_FULL = 2;

    private static final int[] ALL_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] INSERT_SLOTS = new int[]{INPUT_SLOT, BOOK_SLOT};
    private static final int[] EXTRACT_SLOTS = new int[]{2, 3, 4, 5, 6, 7};

    private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);

    private int progress;
    private int maxProgress = 100;
    private int machineState = MACHINE_IDLE;
    private int blockReason = BLOCK_REASON_NONE;
    private PendingDeconstructionOperation pendingOperation;

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> machineState;
                case 3 -> blockReason;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> machineState = value;
                case 3 -> blockReason = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ReverseDeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ReverseDeconstructorBlockEntity blockEntity) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }

        int configuredMaxProgress = ModConfig.processTicks();
        if (blockEntity.maxProgress != configuredMaxProgress) {
            blockEntity.maxProgress = configuredMaxProgress;
            blockEntity.progress = Math.min(blockEntity.progress, configuredMaxProgress);
            blockEntity.setChanged();
        }

        if (blockEntity.pendingOperation != null
                && !blockEntity.pendingOperation.matchesInput(blockEntity.getItem(INPUT_SLOT))) {
            blockEntity.pendingOperation = null;
            blockEntity.progress = 0;
            blockEntity.machineState = MACHINE_IDLE;
            blockEntity.blockReason = BLOCK_REASON_NONE;
            blockEntity.setChanged();
        }

        if (blockEntity.pendingOperation != null) {
            blockEntity.tryCommitPending(serverWorld, state);
            return;
        }

        Optional<DeconstructionPlan> plan = blockEntity.findPlan(serverWorld);
        if (plan.isEmpty()) {
            blockEntity.stopProcessing(serverWorld, state, MACHINE_IDLE, BLOCK_REASON_NONE, true);
            return;
        }

        ItemStack input = blockEntity.getItem(INPUT_SLOT);
        if (hasEnchantments(input) && !blockEntity.hasPlainBook()) {
            blockEntity.stopProcessing(
                    serverWorld, state, MACHINE_BLOCKED, BLOCK_REASON_MISSING_BOOK, true);
            return;
        }

        blockEntity.setActiveState(serverWorld, state, true);
        blockEntity.machineState = MACHINE_PROCESSING;
        blockEntity.blockReason = BLOCK_REASON_NONE;
        blockEntity.progress = Math.min(blockEntity.progress + 1, blockEntity.maxProgress);

        if (blockEntity.progress >= blockEntity.maxProgress) {
            Optional<PendingDeconstructionOperation> pending = blockEntity.createPendingOperation(serverWorld, plan.get());
            if (pending.isEmpty()) {
                blockEntity.stopProcessing(
                        serverWorld,
                        blockEntity.getBlockState(),
                        MACHINE_IDLE,
                        BLOCK_REASON_NONE,
                        true);
                return;
            }

            blockEntity.pendingOperation = pending.get();
            blockEntity.setActiveState(serverWorld, blockEntity.getBlockState(), false);
            blockEntity.tryCommitPending(serverWorld, blockEntity.getBlockState());
            return;
        }

        blockEntity.setChanged();
    }

    private Optional<DeconstructionPlan> findPlan(ServerLevel world) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        try {
            return DeconstructionResolver.resolve(world, input.getItem());
        } catch (RuntimeException exception) {
            SeamlessDeconstructorMod.LOGGER.warn("Failed to resolve a deconstruction recipe for {}", input, exception);
            return Optional.empty();
        }
    }

    private Optional<PendingDeconstructionOperation> createPendingOperation(ServerLevel world, DeconstructionPlan plan) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        ItemEnchantments enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        boolean consumesBook = !enchantments.isEmpty();
        if (consumesBook && !hasPlainBook()) {
            return Optional.empty();
        }

        try {
            Map<Item, Integer> rolledOutput = plan.rollOutput(
                    world.getRandom(),
                    ModConfig.minLossFraction(),
                    ModConfig.maxLossFraction());
            if (plan.damageScalingEnabled()) {
                rolledOutput = DeconstructionOutputRules.applyDurability(
                        rolledOutput,
                        input.isDamageableItem(),
                        input.getMaxDamage(),
                        input.getDamageValue());
            }
            rolledOutput = applyApiModifiers(input, rolledOutput);

            List<ItemStack> exactOutputs = new ArrayList<>();
            if (consumesBook) {
                ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                enchantedBook.set(DataComponents.STORED_ENCHANTMENTS, enchantments);
                exactOutputs.add(enchantedBook);
            }
            rolledOutput.forEach((item, count) -> {
                if (count != null && count > 0 && item != Items.AIR) {
                    exactOutputs.addAll(OutputSlotPlanner.splitToMaxStackSize(new ItemStack(item, count)));
                }
            });

            return Optional.of(new PendingDeconstructionOperation(input, consumesBook, exactOutputs));
        } catch (RuntimeException exception) {
            SeamlessDeconstructorMod.LOGGER.error(
                    "Could not create an atomic deconstruction operation for {}. No inventory was changed.",
                    input,
                    exception);
            return Optional.empty();
        }
    }

    private Map<Item, Integer> applyApiModifiers(ItemStack input, Map<Item, Integer> output) {
        Map<String, Integer> byId = new LinkedHashMap<>();
        output.forEach((item, count) -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && count != null && count > 0) {
                byId.merge(id.toString(), count, Integer::sum);
            }
        });

        int maxDamage = input.getMaxDamage();
        double durabilityFraction = maxDamage > 0
                ? (maxDamage - input.getDamageValue()) / (double) maxDamage
                : 1.0D;
        Identifier inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        DeconstructionContext context = new DeconstructionContext(
                inputId != null ? inputId.toString() : "minecraft:air",
                input.isDamageableItem(),
                durabilityFraction);

        Map<String, Integer> modified = DeconstructionAPI.applyModifiers(context, Map.copyOf(byId));
        if (modified == null) {
            return output;
        }

        Map<Item, Integer> result = new LinkedHashMap<>();
        modified.forEach((id, count) -> {
            if (count == null || count <= 0) {
                return;
            }
            try {
                Identifier identifier = Identifier.parse(id);
                if (!BuiltInRegistries.ITEM.containsKey(identifier)) {
                    SeamlessDeconstructorMod.LOGGER.warn("Ignoring unknown item '{}' returned by a deconstruction modifier", id);
                    return;
                }
                Item item = BuiltInRegistries.ITEM.getValue(identifier);
                if (item != null && item != Items.AIR) {
                    result.merge(item, count, Math::addExact);
                }
            } catch (RuntimeException exception) {
                SeamlessDeconstructorMod.LOGGER.warn(
                        "Ignoring invalid output '{}' returned by a deconstruction modifier", id, exception);
            }
        });
        return result;
    }

    private void tryCommitPending(ServerLevel world, BlockState state) {
        PendingDeconstructionOperation pending = pendingOperation;
        if (pending == null) {
            return;
        }

        if (!pending.matchesInput(getItem(INPUT_SLOT))) {
            pendingOperation = null;
            stopProcessing(world, state, MACHINE_IDLE, BLOCK_REASON_NONE, true);
            return;
        }

        Optional<List<ItemStack>> plannedInventory = OutputSlotPlanner.plan(
                items, OUTPUT_START, OUTPUT_END, pending.outputs());
        boolean bookAvailable = !pending.consumesBook() || hasPlainBook();
        if (!bookAvailable || plannedInventory.isEmpty()) {
            int reason = bookAvailable ? BLOCK_REASON_OUTPUT_FULL : BLOCK_REASON_MISSING_BOOK;
            boolean changed = progress != maxProgress
                    || machineState != MACHINE_BLOCKED
                    || blockReason != reason;
            progress = maxProgress;
            machineState = MACHINE_BLOCKED;
            blockReason = reason;
            setActiveState(world, state, false);
            if (changed) {
                setChanged();
            }
            return;
        }

        List<ItemStack> planned = plannedInventory.get();
        for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
            items.set(slot, planned.get(slot));
        }

        ItemStack input = items.get(INPUT_SLOT);
        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        if (pending.consumesBook()) {
            ItemStack book = items.get(BOOK_SLOT);
            book.shrink(1);
            if (book.isEmpty()) {
                items.set(BOOK_SLOT, ItemStack.EMPTY);
            }
        }

        pendingOperation = null;
        progress = 0;
        machineState = MACHINE_IDLE;
        blockReason = BLOCK_REASON_NONE;
        setActiveState(world, state, false);
        setChanged();
    }

    private void stopProcessing(
            ServerLevel world,
            BlockState state,
            int stoppedState,
            int stoppedReason,
            boolean resetProgress) {
        boolean changed = machineState != stoppedState
                || blockReason != stoppedReason
                || (resetProgress && progress != 0);
        machineState = stoppedState;
        blockReason = stoppedReason;
        if (resetProgress) {
            progress = 0;
        }
        setActiveState(world, state, false);
        if (changed) {
            setChanged();
        }
    }

    private boolean hasPlainBook() {
        return isPlainBook(getItem(BOOK_SLOT));
    }

    private static boolean hasEnchantments(ItemStack input) {
        return !input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    public static boolean isPlainBook(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(Items.BOOK)
                && ItemStack.isSameItemSameComponents(stack, new ItemStack(Items.BOOK));
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isPlainBook(stack) ? 1 : Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    private void setActiveState(ServerLevel world, BlockState currentState, boolean active) {
        if (!currentState.hasProperty(ReverseDeconstructorBlock.ACTIVE)
                || currentState.getValue(ReverseDeconstructorBlock.ACTIVE) == active) {
            return;
        }

        world.setBlock(
                getBlockPos(),
                currentState.setValue(ReverseDeconstructorBlock.ACTIVE, active),
                Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(view, items);
        progress = Math.max(0, view.getIntOr("Progress", 0));
        maxProgress = Math.max(1, view.getIntOr("MaxProgress", ModConfig.processTicks()));
        pendingOperation = PendingDeconstructionOperation.load(view).orElse(null);
        machineState = view.getIntOr(
                "MachineState",
                pendingOperation != null ? MACHINE_BLOCKED : (progress > 0 ? MACHINE_PROCESSING : MACHINE_IDLE));
        if (machineState < MACHINE_IDLE || machineState > MACHINE_BLOCKED) {
            machineState = MACHINE_IDLE;
        }
        blockReason = view.getIntOr("BlockReason", BLOCK_REASON_NONE);
        if (blockReason < BLOCK_REASON_NONE || blockReason > BLOCK_REASON_OUTPUT_FULL) {
            blockReason = BLOCK_REASON_NONE;
        }
        if (machineState != MACHINE_BLOCKED) {
            blockReason = BLOCK_REASON_NONE;
        }
        progress = Math.min(progress, maxProgress);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, items);
        view.putInt("Progress", progress);
        view.putInt("MaxProgress", maxProgress);
        view.putInt("MachineState", machineState);
        view.putInt("BlockReason", blockReason);
        if (pendingOperation != null) {
            pendingOperation.save(view);
        } else {
            view.discard(PendingDeconstructionOperation.STORAGE_KEY);
        }
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.seamlessdeconstructor.reverse_deconstructor");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ReverseDeconstructorScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public ItemStack getRenderInputStack() {
        if (getBlockState().hasProperty(ReverseDeconstructorBlock.ACTIVE)
                && getBlockState().getValue(ReverseDeconstructorBlock.ACTIVE)) {
            return getItem(INPUT_SLOT);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getRenderOutputStack() {
        return getItem(OUTPUT_START);
    }

    public ItemStack getRenderOutputStack(int outputIndex) {
        if (outputIndex < 0 || outputIndex > (OUTPUT_END - OUTPUT_START)) {
            return ItemStack.EMPTY;
        }
        return getItem(OUTPUT_START + outputIndex);
    }

    public int getMachineState() {
        return machineState;
    }

    public int getBlockReason() {
        return blockReason;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ImplementedInventory.super.removeItem(slot, amount);
        if (!result.isEmpty()) {
            if (slot == INPUT_SLOT && pendingOperation != null && !pendingOperation.matchesInput(getItem(INPUT_SLOT))) {
                pendingOperation = null;
                progress = 0;
                machineState = MACHINE_IDLE;
                blockReason = BLOCK_REASON_NONE;
            }
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ImplementedInventory.super.removeItemNoUpdate(slot);
        if (!result.isEmpty()) {
            if (slot == INPUT_SLOT) {
                pendingOperation = null;
                progress = 0;
                machineState = MACHINE_IDLE;
                blockReason = BLOCK_REASON_NONE;
            }
            setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ImplementedInventory.super.setItem(slot, stack);
        if (slot == INPUT_SLOT && pendingOperation != null && !pendingOperation.matchesInput(getItem(INPUT_SLOT))) {
            pendingOperation = null;
            progress = 0;
            machineState = MACHINE_IDLE;
            blockReason = BLOCK_REASON_NONE;
        }
        setChanged();
    }

    @Override
    public void clearContent() {
        ImplementedInventory.super.clearContent();
        pendingOperation = null;
        progress = 0;
        machineState = MACHINE_IDLE;
        blockReason = BLOCK_REASON_NONE;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == null) {
            return ALL_SLOTS;
        }
        return side == Direction.DOWN ? EXTRACT_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (slot == INPUT_SLOT) {
            return !stack.is(Items.BOOK);
        }
        if (slot == BOOK_SLOT) {
            return isPlainBook(stack) && getItem(BOOK_SLOT).isEmpty();
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= OUTPUT_START && slot <= OUTPUT_END;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return !stack.is(Items.BOOK);
        }
        if (slot == BOOK_SLOT) {
            return isPlainBook(stack) && getItem(BOOK_SLOT).isEmpty();
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
}
