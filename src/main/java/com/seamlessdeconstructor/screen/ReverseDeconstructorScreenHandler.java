package com.seamlessdeconstructor.screen;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class ReverseDeconstructorScreenHandler extends AbstractContainerMenu {
    private static final int INVENTORY_SIZE = 8;

    private final Container inventory;
    private final ContainerData propertyDelegate;

    public ReverseDeconstructorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(INVENTORY_SIZE), new SimpleContainerData(2));
    }

    public ReverseDeconstructorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.REVERSE_DECONSTRUCTOR_SCREEN_HANDLER.get(), syncId);
        checkContainerSize(inventory, INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);

        addSlot(new Slot(inventory, 0, 30, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(Items.BOOK);
            }
        });
        addSlot(new Slot(inventory, 1, 30, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        int outputIndex = 2;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                final int slotIndex = outputIndex++;
                addSlot(new Slot(inventory, slotIndex, 98 + column * 18, 25 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
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

        addDataSlots(propertyDelegate);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (index < INVENTORY_SIZE) {
                if (!moveItemStackTo(originalStack, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else {
                if (originalStack.is(Items.BOOK)) {
                    if (!moveItemStackTo(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
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
