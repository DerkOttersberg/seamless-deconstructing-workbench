package io.github.derkottersberg.seamlessdeconstructor.neoforge;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

final class WorkbenchTransferHandler extends SidedInvWrapper {
    WorkbenchTransferHandler(ReverseDeconstructorBlockEntity inventory, @Nullable Direction side) {
        super(inventory, side);
    }

    @Override
    public int getSlotLimit(int wrapperSlot) {
        int inventorySlot = getSlot(inv, wrapperSlot, side);
        return inventorySlot == ReverseDeconstructorBlockEntity.BOOK_SLOT ? 1 : super.getSlotLimit(wrapperSlot);
    }

    @Override
    public ItemStack insertItem(int wrapperSlot, ItemStack stack, boolean simulate) {
        int inventorySlot = getSlot(inv, wrapperSlot, side);
        if (inventorySlot == ReverseDeconstructorBlockEntity.BOOK_SLOT
                && !ReverseDeconstructorBlockEntity.isPlainBook(stack)) {
            return stack;
        }
        return super.insertItem(wrapperSlot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int wrapperSlot, int amount, boolean simulate) {
        int inventorySlot = getSlot(inv, wrapperSlot, side);
        if (inventorySlot < ReverseDeconstructorBlockEntity.OUTPUT_START
                || inventorySlot > ReverseDeconstructorBlockEntity.OUTPUT_END) return ItemStack.EMPTY;
        return super.extractItem(wrapperSlot, amount, simulate);
    }
}
