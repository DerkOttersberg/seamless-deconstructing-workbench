package com.seamlessdeconstructor.logic;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;

/** Loader-neutral fractional ingredient and configured-loss calculation. */
public final class FractionalOutputRoller {
    private FractionalOutputRoller() {
    }

    public static <T> Map<T, Integer> roll(
            Map<T, Double> units,
            RandomSource random,
            double minLoss,
            double maxLoss) {
        double lower = Math.min(minLoss, maxLoss);
        double upper = Math.max(minLoss, maxLoss);
        double loss = lower + random.nextDouble() * (upper - lower);
        Map<T, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<T, Double> entry : units.entrySet()) {
            double rawUnits = entry.getValue();
            int whole = (int) Math.floor(rawUnits);
            double fraction = rawUnits - whole;
            int trials = whole + (random.nextDouble() < fraction ? 1 : 0);
            int kept = 0;

            for (int i = 0; i < trials; i++) {
                if (random.nextDouble() >= loss) {
                    kept++;
                }
            }

            if (kept > 0) {
                result.put(entry.getKey(), kept);
            }
        }
        return result;
    }
}
