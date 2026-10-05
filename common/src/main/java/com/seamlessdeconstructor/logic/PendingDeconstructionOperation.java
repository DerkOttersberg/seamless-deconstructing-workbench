package com.seamlessdeconstructor.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * The exact, already-randomized result of one operation. Keeping this object until it can be
 * committed prevents blocked machines and world reloads from rerolling their output.
 */
public final class PendingDeconstructionOperation {
    public static final String STORAGE_KEY = "PendingOperation";
    private static final String INPUT_KEY = "Input";
    private static final String CONSUMES_BOOK_KEY = "ConsumesBook";
    private static final String OUTPUTS_KEY = "Outputs";

    private final ItemStack inputIdentity;
    private final boolean consumesBook;
    private final List<ItemStack> outputs;

    public PendingDeconstructionOperation(ItemStack inputIdentity, boolean consumesBook, List<ItemStack> outputs) {
        if (inputIdentity.isEmpty()) {
            throw new IllegalArgumentException("A pending operation needs an input identity");
        }
        this.inputIdentity = inputIdentity.copyWithCount(1);
        this.consumesBook = consumesBook;
        this.outputs = outputs.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    public boolean matchesInput(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(inputIdentity, stack);
    }

    public ItemStack inputIdentity() {
        return inputIdentity.copy();
    }

    public boolean consumesBook() {
        return consumesBook;
    }

    public List<ItemStack> outputs() {
        return outputs.stream().map(ItemStack::copy).toList();
    }

    public void save(CompoundTag root, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag output = new CompoundTag();
        output.put(INPUT_KEY, inputIdentity.save(registries));
        output.putBoolean(CONSUMES_BOOK_KEY, consumesBook);
        ListTag storedOutputs = new ListTag();
        outputs.forEach(stack -> storedOutputs.add(stack.save(registries)));
        output.put(OUTPUTS_KEY, storedOutputs);
        root.put(STORAGE_KEY, output);
    }

    public static Optional<PendingDeconstructionOperation> load(CompoundTag root, net.minecraft.core.HolderLookup.Provider registries) {
        if (!root.contains(STORAGE_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag stored = root.getCompound(STORAGE_KEY);
        ItemStack input = ItemStack.parseOptional(registries, LegacyItemStackMigration.migrate(stored.getCompound(INPUT_KEY)));
        if (input.isEmpty()) return Optional.empty();
        List<ItemStack> outputs = new ArrayList<>();
        ListTag storedOutputs = stored.getList(OUTPUTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < storedOutputs.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, LegacyItemStackMigration.migrate(storedOutputs.getCompound(i)));
            if (!stack.isEmpty()) outputs.addAll(OutputSlotPlanner.splitToMaxStackSize(stack));
        }
        return Optional.of(new PendingDeconstructionOperation(input,
                stored.getBoolean(CONSUMES_BOOK_KEY), outputs));
    }
}
