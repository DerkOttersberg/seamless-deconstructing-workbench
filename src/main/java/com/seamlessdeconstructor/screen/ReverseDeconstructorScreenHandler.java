package com.seamlessdeconstructor.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ReverseDeconstructorScreenHandler extends ScreenHandler {
    private static final int INVENTORY_SIZE = 8;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public ReverseDeconstructorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE), new ArrayPropertyDelegate(2));
    }

    public ReverseDeconstructorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.REVERSE_DECONSTRUCTOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);

        addSlot(new Slot(inventory, 0, 30, 24) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return !stack.isOf(Items.BOOK);
            }
        });
        addSlot(new Slot(inventory, 1, 30, 42) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BOOK);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        int outputIndex = 2;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                final int slotIndex = outputIndex++;
                addSlot(new Slot(inventory, slotIndex, 98 + column * 18, 25 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        addProperties(propertyDelegate);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (index < INVENTORY_SIZE) {
                if (!insertItem(originalStack, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else {
                if (originalStack.isOf(Items.BOOK)) {
                    if (!insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }

    public boolean isProcessing() {
        return propertyDelegate.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        int width = 24;

        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }

        return progress * width / maxProgress;
    }
}
