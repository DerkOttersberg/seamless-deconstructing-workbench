package com.seamlessdeconstructor.block.entity;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.logic.DeconstructionPlan;
import com.seamlessdeconstructor.logic.DeconstructionResolver;
import com.seamlessdeconstructor.logic.DeconstructionOutputRules;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreenHandler;
import com.seamlessdeconstructor.util.ImplementedInventory;
import com.derko.seamlessapi.DeconstructionAPI;
import com.derko.seamlessapi.api.deconstruction.DeconstructionContext;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ReverseDeconstructorBlockEntity extends BlockEntity implements ImplementedInventory, MenuProvider, WorldlyContainer {
    private static final int INPUT_SLOT = 0;
    private static final int BOOK_SLOT = 1;
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 7;
    private static final int[] INSERT_SLOTS = new int[]{INPUT_SLOT, BOOK_SLOT};
    private static final int[] EXTRACT_SLOTS = new int[]{2, 3, 4, 5, 6, 7};

    private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);

    private int progress = 0;
    private int maxProgress = 100;

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ReverseDeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ReverseDeconstructorBlockEntity blockEntity) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }

        blockEntity.maxProgress = ModConfig.processTicks();

        Optional<DeconstructionPlan> plan = blockEntity.findPlan(serverWorld);
        if (plan.isEmpty() || !blockEntity.canOutput(plan.get())) {
            blockEntity.setActiveState(serverWorld, state, false);
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                setChanged(world, pos, state);
            }
            return;
        }

        blockEntity.setActiveState(serverWorld, state, true);
        blockEntity.progress++;

        if (blockEntity.progress >= blockEntity.maxProgress) {
            blockEntity.progress = 0;
            blockEntity.process(serverWorld, plan.get());
        }

        setChanged(world, pos, state);
    }

    private Optional<DeconstructionPlan> findPlan(ServerLevel world) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        try {
            return DeconstructionResolver.resolve(world, input.getItem());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private boolean canOutput(DeconstructionPlan plan) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }

        if (!canExtractEnchantments(input)) {
            return false;
        }

        Map<Item, Integer> maxRoll = plan.maxRollPerOperation();
        for (Map.Entry<Item, Integer> entry : maxRoll.entrySet()) {
            if (!hasRoomFor(entry.getKey(), entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    private void process(ServerLevel world, DeconstructionPlan plan) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        if (!extractEnchantmentsToBook(input)) {
            return;
        }

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

        Map<Item, Integer> leftovers = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : rolledOutput.entrySet()) {
            int remaining = insertOutput(entry.getKey(), entry.getValue());
            if (remaining > 0) {
                leftovers.put(entry.getKey(), remaining);
            }
        }

        if (!leftovers.isEmpty()) {
            leftovers.forEach((item, count) -> {
                ItemStack stack = new ItemStack(item, count);
                if (world != null) {
                    BlockPos spawnPos = getBlockPos();
                    Block.popResource(world, spawnPos, stack);
                }
            });
        }

        input.shrink(1);
        if (input.isEmpty()) {
            setItem(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private Map<Item, Integer> applyApiModifiers(ItemStack input, Map<Item, Integer> output) {
        Map<String, Integer> byId = new LinkedHashMap<>();
        output.forEach((item, count) -> {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && count > 0) {
                byId.put(id.toString(), count);
            }
        });

        int maxDamage = input.getMaxDamage();
        double durabilityFraction = maxDamage > 0
                ? (maxDamage - input.getDamageValue()) / (double) maxDamage
                : 1.0D;
        var inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        DeconstructionContext context = new DeconstructionContext(
                inputId != null ? inputId.toString() : "minecraft:air",
                input.isDamageableItem(),
                durabilityFraction);

        Map<String, Integer> modified = DeconstructionAPI.applyModifiers(context, Map.copyOf(byId));
        Map<Item, Integer> result = new LinkedHashMap<>();
        if (modified == null) {
            return output;
        }

        modified.forEach((id, count) -> {
            if (count == null || count <= 0) {
                return;
            }
            try {
                Item item = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
                if (item != null) {
                    result.merge(item, count, Integer::sum);
                }
            } catch (RuntimeException ignored) {
            }
        });
        return result;
    }

    private boolean hasRoomFor(Item item, int count) {
        int remaining = count;

        for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                remaining -= Math.min(64, remaining);
            } else if (stack.is(item)) {
                remaining -= Math.min(stack.getMaxStackSize() - stack.getCount(), remaining);
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return remaining <= 0;
    }

    private int insertOutput(Item item, int amount) {
        int remaining = amount;

        for (int slot = OUTPUT_START; slot <= OUTPUT_END && remaining > 0; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                int insert = Math.min(64, remaining);
                setItem(slot, new ItemStack(item, insert));
                remaining -= insert;
                continue;
            }

            if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                int insert = Math.min(stack.getMaxStackSize() - stack.getCount(), remaining);
                stack.grow(insert);
                remaining -= insert;
            }
        }

        return remaining;
    }

    private boolean canExtractEnchantments(ItemStack input) {
        ItemEnchantments enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return true;
        }

        ItemStack book = getItem(BOOK_SLOT);
        return !book.isEmpty() && book.is(Items.BOOK) && hasRoomFor(Items.ENCHANTED_BOOK, 1);
    }

    private boolean extractEnchantmentsToBook(ItemStack input) {
        ItemEnchantments enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return true;
        }

        ItemStack book = getItem(BOOK_SLOT);
        if (book.isEmpty() || !book.is(Items.BOOK)) {
            return false;
        }

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK, 1);
        enchantedBook.set(DataComponents.STORED_ENCHANTMENTS, enchantments);

        ItemStack leftover = insertOutputStack(enchantedBook);
        if (!leftover.isEmpty()) {
            return false;
        }

        book.shrink(1);
        if (book.isEmpty()) {
            setItem(BOOK_SLOT, ItemStack.EMPTY);
        }

        return true;
    }

    private ItemStack insertOutputStack(ItemStack stackToInsert) {
        if (stackToInsert.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stackToInsert.copy();

        for (int slot = OUTPUT_START; slot <= OUTPUT_END && !remaining.isEmpty(); slot++) {
            ItemStack stack = getItem(slot);

            if (stack.isEmpty()) {
                setItem(slot, remaining.copy());
                return ItemStack.EMPTY;
            }

            if (ItemStack.isSameItemSameComponents(stack, remaining) && stack.getCount() < stack.getMaxStackSize()) {
                int insert = Math.min(stack.getMaxStackSize() - stack.getCount(), remaining.getCount());
                stack.grow(insert);
                remaining.shrink(insert);
            }
        }

        return remaining;
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
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(view, items);
        this.progress = view.getInt("Progress").orElse(0);
        this.maxProgress = view.getInt("MaxProgress").orElse(ModConfig.processTicks());
        if (this.maxProgress <= 0) {
            this.maxProgress = ModConfig.processTicks();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, items);
        view.putInt("Progress", progress);
        view.putInt("MaxProgress", maxProgress);
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
        if (this.getBlockState().hasProperty(ReverseDeconstructorBlock.ACTIVE)
                && this.getBlockState().getValue(ReverseDeconstructorBlock.ACTIVE)) {
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

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ImplementedInventory.super.removeItem(slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ImplementedInventory.super.removeItemNoUpdate(slot);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ImplementedInventory.super.setItem(slot, stack);
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? EXTRACT_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (slot == INPUT_SLOT) {
            return !stack.is(Items.BOOK);
        }
        if (slot == BOOK_SLOT) {
            return stack.is(Items.BOOK) && getItem(BOOK_SLOT).isEmpty();
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_START && slot <= OUTPUT_END;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return !stack.is(Items.BOOK);
        }
        if (slot == BOOK_SLOT) {
            return stack.is(Items.BOOK);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
}
