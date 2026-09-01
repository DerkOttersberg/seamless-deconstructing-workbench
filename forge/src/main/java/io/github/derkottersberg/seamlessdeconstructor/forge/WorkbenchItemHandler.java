package io.github.derkottersberg.seamlessdeconstructor.forge;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

final class WorkbenchItemHandler extends SidedInvWrapper {
    WorkbenchItemHandler(ReverseDeconstructorBlockEntity inventory, @Nullable Direction side) {
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
}
