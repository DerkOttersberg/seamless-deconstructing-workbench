package com.seamlessdeconstructor.logic;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/** Vanilla cannot see stacks stored inside a mod's custom pending-operation NBT. */
public final class LegacyItemStackMigration {
    private LegacyItemStackMigration() {}

    public static CompoundTag migrate(CompoundTag stack) {
        if (!stack.contains("Count", Tag.TAG_ANY_NUMERIC)) return stack;
        // The preserved 1.20.1 format predates the 1.20.5 item-component rewrite.
        var migrated = DataFixers.getDataFixer().update(References.ITEM_STACK,
            new Dynamic<>(NbtOps.INSTANCE, stack.copy()), 3465,
            SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (!(migrated instanceof CompoundTag result)) throw new IllegalStateException("Legacy stack migration returned non-compound data");
        return result;
    }
}
