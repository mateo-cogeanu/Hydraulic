package org.geysermc.hydraulic.util;

import java.util.Objects;
import java.util.function.IntFunction;

/** Completes sparse runtime tables without overwriting existing mappings. */
public final class MissingMappings {
    private MissingMappings() {
    }

    public static <T> int fill(T[] mappings, IntFunction<T> fallback) {
        int filled = 0;
        for (int id = 0; id < mappings.length; id++) {
            if (mappings[id] == null) {
                mappings[id] = Objects.requireNonNull(fallback.apply(id), "Missing fallback for runtime ID " + id);
                filled++;
            }
        }
        return filled;
    }
}
