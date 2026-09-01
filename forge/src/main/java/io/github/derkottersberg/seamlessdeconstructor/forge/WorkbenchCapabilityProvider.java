package io.github.derkottersberg.seamlessdeconstructor.forge;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class WorkbenchCapabilityProvider implements ICapabilityProvider {
    private final ReverseDeconstructorBlockEntity inventory;
    private final Map<Direction, LazyOptional<IItemHandler>> sidedHandlers = new EnumMap<>(Direction.class);
    private final LazyOptional<IItemHandler> unsidedHandler;

    WorkbenchCapabilityProvider(ReverseDeconstructorBlockEntity inventory) {
        this.inventory = inventory;
        this.unsidedHandler = LazyOptional.of(() -> new WorkbenchItemHandler(inventory, null));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability,
            @Nullable Direction side) {
        if (capability != ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.empty();
        }

        LazyOptional<IItemHandler> handler = side == null
                ? unsidedHandler
                : sidedHandlers.computeIfAbsent(
                        side,
                        direction -> LazyOptional.of(() -> new WorkbenchItemHandler(inventory, direction)));
        return handler.cast();
    }

    void invalidate() {
        unsidedHandler.invalidate();
        sidedHandlers.values().forEach(LazyOptional::invalidate);
        sidedHandlers.clear();
    }
}
