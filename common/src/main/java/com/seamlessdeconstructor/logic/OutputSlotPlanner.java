package com.seamlessdeconstructor.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Simulates an output insertion without mutating the live block-entity inventory.
 */
public final class OutputSlotPlanner {
    private OutputSlotPlanner() {
    }

    public static Optional<List<ItemStack>> plan(
            List<ItemStack> currentInventory,
            int outputStart,
            int outputEnd,
            List<ItemStack> outputs) {
        if (outputStart < 0 || outputEnd < outputStart || outputEnd >= currentInventory.size()) {
            throw new IllegalArgumentException("Invalid output slot range");
        }

        List<ItemStack> planned = currentInventory.stream().map(ItemStack::copy).toList();
        planned = new ArrayList<>(planned);

        for (ItemStack requested : outputs) {
            if (requested.isEmpty()) {
                continue;
            }

            ItemStack remaining = requested.copy();
            for (int slot = outputStart; slot <= outputEnd && !remaining.isEmpty(); slot++) {
                ItemStack existing = planned.get(slot);
                if (!existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, remaining)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    int inserted = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
                    existing.grow(inserted);
                    remaining.shrink(inserted);
                }
            }

            for (int slot = outputStart; slot <= outputEnd && !remaining.isEmpty(); slot++) {
                if (!planned.get(slot).isEmpty()) {
                    continue;
                }

                int inserted = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                planned.set(slot, remaining.copyWithCount(inserted));
                remaining.shrink(inserted);
            }

            if (!remaining.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(planned);
    }

    public static List<ItemStack> splitToMaxStackSize(ItemStack stack) {
        List<ItemStack> result = new ArrayList<>();
        if (stack.isEmpty()) {
            return result;
        }

        int remaining = stack.getCount();
        int maxStackSize = Math.max(1, stack.getMaxStackSize());
        while (remaining > 0) {
            int count = Math.min(maxStackSize, remaining);
            result.add(stack.copyWithCount(count));
            remaining -= count;
        }
        return result;
    }
}
