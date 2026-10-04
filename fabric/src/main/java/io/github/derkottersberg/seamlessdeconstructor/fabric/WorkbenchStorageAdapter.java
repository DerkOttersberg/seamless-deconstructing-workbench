package io.github.derkottersberg.seamlessdeconstructor.fabric;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps Fabric's unsided storage view consistent with the workbench's sided automation contract.
 * InventoryStorage deliberately treats a null side as unrestricted, so input and book slots must
 * not be exposed as extractable views here.
 */
final class WorkbenchStorageAdapter implements Storage<ItemVariant> {
    private final ReverseDeconstructorBlockEntity inventory;
    private final InventoryStorage delegate;
    private final List<SingleSlotStorage<ItemVariant>> extractionSlots;
    private final List<StorageView<ItemVariant>> exposedViews;

    WorkbenchStorageAdapter(ReverseDeconstructorBlockEntity inventory, @Nullable Direction side) {
        this.inventory = inventory;
        delegate = InventoryStorage.of(inventory, side);
        if (side == null) {
            List<SingleSlotStorage<ItemVariant>> allSlots = delegate.getSlots();
            extractionSlots = List.copyOf(allSlots.subList(
                    ReverseDeconstructorBlockEntity.OUTPUT_START,
                    ReverseDeconstructorBlockEntity.OUTPUT_END + 1));
            exposedViews = new ArrayList<>(extractionSlots);
        } else {
            extractionSlots = delegate.getSlots();
            exposedViews = new ArrayList<>(delegate.getSlots());
        }
    }

    @Override
    public boolean supportsInsertion() {
        return delegate.supportsInsertion();
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        // The 1.20.1 Transfer API reads only the container-wide stack limit.
        // Restrict the request before its transactional slot math, not afterwards.
        if (resource.isOf(net.minecraft.world.item.Items.BOOK)) {
            maxAmount = Math.min(maxAmount, Math.max(0,
                    1 - inventory.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount()));
        }
        return delegate.insert(resource, maxAmount, transaction);
    }

    @Override
    public boolean supportsExtraction() {
        return !extractionSlots.isEmpty();
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long extracted = 0;
        for (SingleSlotStorage<ItemVariant> slot : extractionSlots) {
            extracted += slot.extract(resource, maxAmount - extracted, transaction);
            if (extracted == maxAmount) {
                break;
            }
        }
        return extracted;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return exposedViews.iterator();
    }

    @Override
    public long getVersion() {
        return delegate.getVersion();
    }
}
