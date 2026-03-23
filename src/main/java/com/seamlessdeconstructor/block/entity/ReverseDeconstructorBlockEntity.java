package com.seamlessdeconstructor.block.entity;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.logic.DeconstructionPlan;
import com.seamlessdeconstructor.logic.DeconstructionResolver;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreenHandler;
import com.seamlessdeconstructor.util.ImplementedInventory;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ReverseDeconstructorBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory, SidedInventory {
    private static final int INPUT_SLOT = 0;
    private static final int BOOK_SLOT = 1;
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 7;
    private static final int[] INSERT_SLOTS = new int[]{INPUT_SLOT, BOOK_SLOT};
    private static final int[] EXTRACT_SLOTS = new int[]{2, 3, 4, 5, 6, 7};

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(8, ItemStack.EMPTY);

    private int progress = 0;
    private int maxProgress = 100;

    private final PropertyDelegate propertyDelegate = new ArrayPropertyDelegate(2) {
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
        public int size() {
            return 2;
        }
    };

    public ReverseDeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ReverseDeconstructorBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        blockEntity.maxProgress = ModConfig.processTicks();

        Optional<DeconstructionPlan> plan = blockEntity.findPlan(serverWorld);
        if (plan.isEmpty() || !blockEntity.canOutput(plan.get())) {
            blockEntity.setActiveState(serverWorld, state, false);
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                markDirty(world, pos, state);
            }
            return;
        }

        blockEntity.setActiveState(serverWorld, state, true);
        blockEntity.progress++;

        if (blockEntity.progress >= blockEntity.maxProgress) {
            blockEntity.progress = 0;
            blockEntity.process(serverWorld, plan.get());
        }

        markDirty(world, pos, state);
    }

    private Optional<DeconstructionPlan> findPlan(ServerWorld world) {
        ItemStack input = getStack(INPUT_SLOT);
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
        ItemStack input = getStack(INPUT_SLOT);
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

    private void process(ServerWorld world, DeconstructionPlan plan) {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        if (!extractEnchantmentsToBook(input)) {
            return;
        }

        Map<Item, Integer> rolledOutput = applyDurabilityOutputRules(plan.maxRollPerOperation(), input);

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
                    BlockPos spawnPos = getPos();
                    Block.dropStack(world, spawnPos, stack);
                }
            });
        }

        input.decrement(1);
        if (input.isEmpty()) {
            setStack(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private Map<Item, Integer> applyDurabilityOutputRules(Map<Item, Integer> fullOutput, ItemStack input) {
        if (fullOutput.isEmpty() || !input.isDamageable()) {
            return fullOutput;
        }

        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) {
            return fullOutput;
        }

        double durabilityFraction = (maxDamage - input.getDamage()) / (double) maxDamage;

        if (durabilityFraction < 0.25D) {
            return minimumSingleOutput(fullOutput);
        }

        if (durabilityFraction < 0.50D) {
            int fullTotal = 0;
            for (int count : fullOutput.values()) {
                fullTotal += count;
            }
            int minTotal = fullTotal >= 2 ? 2 : 1;
            return scaleOutputWithMinimum(fullOutput, 0.60D, minTotal);
        }

        if (durabilityFraction < 0.75D) {
            return scaleOutputWithMinimum(fullOutput, 0.70D, 1);
        }

        return fullOutput;
    }

    private Map<Item, Integer> minimumSingleOutput(Map<Item, Integer> fullOutput) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : fullOutput.entrySet()) {
            if (entry.getValue() > 0) {
                result.put(entry.getKey(), 1);
                break;
            }
        }
        return result;
    }

    private Map<Item, Integer> scaleOutputWithMinimum(Map<Item, Integer> fullOutput, double multiplier, int minTotal) {
        Map<Item, Integer> scaled = new LinkedHashMap<>();
        int total = 0;

        for (Map.Entry<Item, Integer> entry : fullOutput.entrySet()) {
            int scaledCount = (int) Math.floor(entry.getValue() * multiplier);
            if (scaledCount > 0) {
                scaled.put(entry.getKey(), scaledCount);
                total += scaledCount;
            }
        }

        if (total >= minTotal) {
            return scaled;
        }

        for (Map.Entry<Item, Integer> entry : fullOutput.entrySet()) {
            if (total >= minTotal) {
                break;
            }

            int maxForItem = entry.getValue();
            int current = scaled.getOrDefault(entry.getKey(), 0);
            if (current >= maxForItem) {
                continue;
            }

            scaled.put(entry.getKey(), current + 1);
            total++;
        }

        return scaled;
    }

    private boolean hasRoomFor(Item item, int count) {
        int remaining = count;

        for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) {
                remaining -= Math.min(64, remaining);
            } else if (stack.isOf(item)) {
                remaining -= Math.min(stack.getMaxCount() - stack.getCount(), remaining);
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
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) {
                int insert = Math.min(64, remaining);
                setStack(slot, new ItemStack(item, insert));
                remaining -= insert;
                continue;
            }

            if (stack.isOf(item) && stack.getCount() < stack.getMaxCount()) {
                int insert = Math.min(stack.getMaxCount() - stack.getCount(), remaining);
                stack.increment(insert);
                remaining -= insert;
            }
        }

        return remaining;
    }

    private boolean canExtractEnchantments(ItemStack input) {
        ItemEnchantmentsComponent enchantments = input.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        if (enchantments.isEmpty()) {
            return true;
        }

        ItemStack book = getStack(BOOK_SLOT);
        return !book.isEmpty() && book.isOf(Items.BOOK) && hasRoomFor(Items.ENCHANTED_BOOK, 1);
    }

    private boolean extractEnchantmentsToBook(ItemStack input) {
        ItemEnchantmentsComponent enchantments = input.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        if (enchantments.isEmpty()) {
            return true;
        }

        ItemStack book = getStack(BOOK_SLOT);
        if (book.isEmpty() || !book.isOf(Items.BOOK)) {
            return false;
        }

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK, 1);
        enchantedBook.set(DataComponentTypes.STORED_ENCHANTMENTS, enchantments);

        ItemStack leftover = insertOutputStack(enchantedBook);
        if (!leftover.isEmpty()) {
            return false;
        }

        book.decrement(1);
        if (book.isEmpty()) {
            setStack(BOOK_SLOT, ItemStack.EMPTY);
        }

        return true;
    }

    private ItemStack insertOutputStack(ItemStack stackToInsert) {
        if (stackToInsert.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stackToInsert.copy();

        for (int slot = OUTPUT_START; slot <= OUTPUT_END && !remaining.isEmpty(); slot++) {
            ItemStack stack = getStack(slot);

            if (stack.isEmpty()) {
                setStack(slot, remaining.copy());
                return ItemStack.EMPTY;
            }

            if (ItemStack.areItemsAndComponentsEqual(stack, remaining) && stack.getCount() < stack.getMaxCount()) {
                int insert = Math.min(stack.getMaxCount() - stack.getCount(), remaining.getCount());
                stack.increment(insert);
                remaining.decrement(insert);
            }
        }

        return remaining;
    }

    private void setActiveState(ServerWorld world, BlockState currentState, boolean active) {
        if (!currentState.contains(ReverseDeconstructorBlock.ACTIVE) || currentState.get(ReverseDeconstructorBlock.ACTIVE) == active) {
            return;
        }

        world.setBlockState(getPos(), currentState.with(ReverseDeconstructorBlock.ACTIVE, active), Block.NOTIFY_LISTENERS);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        Inventories.readData(view, items);
        this.progress = view.getInt("Progress", 0);
        this.maxProgress = view.getInt("MaxProgress", ModConfig.processTicks());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, items);
        view.putInt("Progress", progress);
        view.putInt("MaxProgress", maxProgress);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.seamlessdeconstructor.reverse_deconstructor");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ReverseDeconstructorScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    public ItemStack getRenderInputStack() {
        if (this.getCachedState().contains(ReverseDeconstructorBlock.ACTIVE)
                && this.getCachedState().get(ReverseDeconstructorBlock.ACTIVE)) {
            return getStack(INPUT_SLOT);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getRenderOutputStack() {
        return getStack(OUTPUT_START);
    }

    public ItemStack getRenderOutputStack(int outputIndex) {
        if (outputIndex < 0 || outputIndex > (OUTPUT_END - OUTPUT_START)) {
            return ItemStack.EMPTY;
        }
        return getStack(OUTPUT_START + outputIndex);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world instanceof ServerWorld serverWorld) {
            BlockEntityUpdateS2CPacket packet = this.toUpdatePacket();
            if (packet != null) {
                for (ServerPlayerEntity player : PlayerLookup.tracking(serverWorld, this.pos)) {
                    player.networkHandler.sendPacket(packet);
                }
            }
        }
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = ImplementedInventory.super.removeStack(slot, amount);
        if (!result.isEmpty()) {
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = ImplementedInventory.super.removeStack(slot);
        if (!result.isEmpty()) {
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ImplementedInventory.super.setStack(slot, stack);
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return side == Direction.DOWN ? EXTRACT_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        if (slot == INPUT_SLOT) {
            return !stack.isOf(Items.BOOK);
        }
        if (slot == BOOK_SLOT) {
            return stack.isOf(Items.BOOK) && getStack(BOOK_SLOT).isEmpty();
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_START && slot <= OUTPUT_END;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return !stack.isOf(Items.BOOK);
        }
        if (slot == BOOK_SLOT) {
            return stack.isOf(Items.BOOK);
        }
        return false;
    }
}
