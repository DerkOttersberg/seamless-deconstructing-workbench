package com.seamlessdeconstructor.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

    public void save(ValueOutput root) {
        ValueOutput output = root.child(STORAGE_KEY);
        output.store(INPUT_KEY, ItemStack.CODEC, inputIdentity);
        output.putBoolean(CONSUMES_BOOK_KEY, consumesBook);
        ValueOutput.TypedOutputList<ItemStack> storedOutputs = output.list(OUTPUTS_KEY, ItemStack.CODEC);
        outputs.forEach(storedOutputs::add);
    }

    public static Optional<PendingDeconstructionOperation> load(ValueInput root) {
        Optional<ValueInput> stored = root.child(STORAGE_KEY);
        if (stored.isEmpty()) {
            return Optional.empty();
        }

        Optional<ItemStack> input = stored.get().read(INPUT_KEY, ItemStack.CODEC).filter(stack -> !stack.isEmpty());
        if (input.isEmpty()) {
            return Optional.empty();
        }

        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : stored.get().listOrEmpty(OUTPUTS_KEY, ItemStack.CODEC)) {
            if (!stack.isEmpty()) {
                outputs.addAll(OutputSlotPlanner.splitToMaxStackSize(stack));
            }
        }
        return Optional.of(new PendingDeconstructionOperation(
                input.get(),
                stored.get().getBooleanOr(CONSUMES_BOOK_KEY, false),
                outputs));
    }
}
