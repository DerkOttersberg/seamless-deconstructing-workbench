package com.seamlessdeconstructor.block.entity;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.logic.DeconstructionPlan;
import com.seamlessdeconstructor.logic.DeconstructionResolver;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ReverseDeconstructorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    private static final int INPUT_SLOT = 0;
    private static final int BOOK_SLOT = 1;
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 7;
    private static final int[] ALL_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] EXTRACT_SLOTS = new int[]{2, 3, 4, 5, 6, 7};

    private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
    private final ContainerData data = new SimpleContainerData(2);
    private int progress;
    private int maxProgress = 100;

    public ReverseDeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReverseDeconstructorBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        blockEntity.maxProgress = ModConfig.processTicks();
        blockEntity.data.set(1, blockEntity.maxProgress);

        Optional<DeconstructionPlan> plan = blockEntity.findPlan(serverLevel);
        if (plan.isEmpty() || !blockEntity.canOutput(plan.get())) {
            blockEntity.setActiveState(serverLevel, state, false);
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                blockEntity.data.set(0, 0);
                setChanged(level, pos, state);
            }
            return;
        }

        blockEntity.setActiveState(serverLevel, state, true);
        blockEntity.progress++;
        blockEntity.data.set(0, blockEntity.progress);

        if (blockEntity.progress >= blockEntity.maxProgress) {
            blockEntity.progress = 0;
            blockEntity.data.set(0, 0);
            blockEntity.process(serverLevel, plan.get());
        }

        setChanged(level, pos, state);
    }

    private Optional<DeconstructionPlan> findPlan(ServerLevel level) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        try {
            return DeconstructionResolver.resolve(level, input.getItem());
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

        Map<Item, Integer> required = new LinkedHashMap<>(plan.maxRollPerOperation());
        if (!EnchantmentHelper.getEnchantments(input).isEmpty()) {
            required.merge(Items.ENCHANTED_BOOK, 1, Integer::sum);
        }

        return canFitAllOutputs(required);
    }

    private void process(ServerLevel level, DeconstructionPlan plan) {
        ItemStack input = getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        if (!extractEnchantmentsToBook(input)) {
            return;
        }

        Map<Item, Integer> rolledOutput = plan.rollOutput(level.random, ModConfig.minLossFraction(), ModConfig.maxLossFraction());
        if (rolledOutput.isEmpty()) {
            rolledOutput = plan.maxRollPerOperation();
        }

        Map<Item, Integer> leftovers = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : rolledOutput.entrySet()) {
            int remaining = insertOutput(entry.getKey(), entry.getValue());
            if (remaining > 0) {
                leftovers.put(entry.getKey(), remaining);
            }
        }

        leftovers.forEach((item, count) -> Block.popResource(level, worldPosition, new ItemStack(item, count)));

        input.shrink(1);
        if (input.isEmpty()) {
            setItem(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canFitAllOutputs(Map<Item, Integer> required) {
        if (required.isEmpty()) {
            return true;
        }

        Map<Item, Integer> remaining = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            if (entry.getValue() > 0) {
                remaining.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        int emptySlots = 0;
        for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                emptySlots++;
                continue;
            }

            Integer need = remaining.get(stack.getItem());
            if (need == null || need <= 0) {
                continue;
            }

            int freeInStack = Math.max(0, stack.getMaxStackSize() - stack.getCount());
            if (freeInStack <= 0) {
                continue;
            }

            int reduced = Math.min(need, freeInStack);
            int updated = need - reduced;
            if (updated <= 0) {
                remaining.remove(stack.getItem());
            } else {
                remaining.put(stack.getItem(), updated);
            }
        }

        int neededEmptySlots = 0;
        for (Map.Entry<Item, Integer> entry : remaining.entrySet()) {
            int need = entry.getValue();
            if (need <= 0) {
                continue;
            }

            int maxStack = new ItemStack(entry.getKey()).getMaxStackSize();
            if (maxStack <= 0) {
                return false;
            }
            neededEmptySlots += (need + maxStack - 1) / maxStack;

            if (neededEmptySlots > emptySlots) {
                return false;
            }
        }

        return true;
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
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(input);
        if (enchantments.isEmpty()) {
            return true;
        }

        ItemStack book = getItem(BOOK_SLOT);
        return !book.isEmpty() && book.is(Items.BOOK);
    }

    private boolean extractEnchantmentsToBook(ItemStack input) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(input);
        if (enchantments.isEmpty()) {
            return true;
        }

        ItemStack book = getItem(BOOK_SLOT);
        if (book.isEmpty() || !book.is(Items.BOOK)) {
            return false;
        }

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK, 1);
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            EnchantedBookItem.addEnchantment(enchantedBook, new EnchantmentInstance(entry.getKey(), entry.getValue()));
        }

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

            if (ItemStack.isSameItemSameTags(stack, remaining) && stack.getCount() < stack.getMaxStackSize()) {
                int insert = Math.min(stack.getMaxStackSize() - stack.getCount(), remaining.getCount());
                stack.grow(insert);
                remaining.shrink(insert);
            }
        }

        return remaining;
    }

    private void setActiveState(ServerLevel level, BlockState currentState, boolean active) {
        if (!currentState.hasProperty(ReverseDeconstructorBlock.ACTIVE) || currentState.getValue(ReverseDeconstructorBlock.ACTIVE) == active) {
            return;
        }

        level.setBlock(worldPosition, currentState.setValue(ReverseDeconstructorBlock.ACTIVE, active), 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.seamlessdeconstructor.reverse_deconstructor");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        data.set(0, progress);
        data.set(1, maxProgress);
        return new ReverseDeconstructorScreenHandler(syncId, playerInventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) {
            setChanged();
            syncToClient();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        syncToClient();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
        syncToClient();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clearItemsForLoad();
        ContainerHelper.loadAllItems(tag, items);
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : ModConfig.processTicks();
        this.data.set(0, this.progress);
        this.data.set(1, this.maxProgress);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canTakeItem(this, slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT || (slot == BOOK_SLOT && stack.is(Items.BOOK));
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot >= OUTPUT_START;
    }

    public ItemStack getRenderInputStack() {
        if (getBlockState().hasProperty(ReverseDeconstructorBlock.ACTIVE) && getBlockState().getValue(ReverseDeconstructorBlock.ACTIVE)) {
            return getItem(INPUT_SLOT);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getRenderOutputStack(int index) {
        int slot = OUTPUT_START + index;
        if (slot < OUTPUT_START || slot > OUTPUT_END) {
            return ItemStack.EMPTY;
        }
        return getItem(slot);
    }

    private void syncToClient() {
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void clearItemsForLoad() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
