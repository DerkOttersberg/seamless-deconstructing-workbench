package io.github.derkottersberg.seamlessdeconstructor.neoforge;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import org.jspecify.annotations.Nullable;

final class WorkbenchTransferHandler extends WorldlyContainerWrapper {
    private final ReverseDeconstructorBlockEntity inventory;
    private final @Nullable Direction side;

    WorkbenchTransferHandler(ReverseDeconstructorBlockEntity inventory, @Nullable Direction side) {
        super(inventory, side);
        this.inventory = inventory;
        this.side = side;
    }

    @Override
    public long getCapacityAsLong(int wrapperSlot, ItemResource resource) {
        int inventorySlot = side == null ? wrapperSlot : inventory.getSlotsForFace(side)[wrapperSlot];
        long capacity = super.getCapacityAsLong(wrapperSlot, resource);
        return inventorySlot == ReverseDeconstructorBlockEntity.BOOK_SLOT ? Math.min(1L, capacity) : capacity;
    }

    @Override
    public int extract(
            int wrapperSlot,
            ItemResource resource,
            int amount,
            TransactionContext transaction) {
        int inventorySlot = side == null ? wrapperSlot : inventory.getSlotsForFace(side)[wrapperSlot];
        if (inventorySlot < ReverseDeconstructorBlockEntity.OUTPUT_START
                || inventorySlot > ReverseDeconstructorBlockEntity.OUTPUT_END) {
            return 0;
        }
        return super.extract(wrapperSlot, resource, amount, transaction);
    }
}
