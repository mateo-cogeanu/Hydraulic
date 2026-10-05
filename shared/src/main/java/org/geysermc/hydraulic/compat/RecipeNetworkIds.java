package org.geysermc.hydraulic.compat;

import java.util.concurrent.atomic.AtomicInteger;

/** Keep dynamic recipe IDs above the IDs already reserved for built-in recipes. */
public final class RecipeNetworkIds {
    public static void reserve(AtomicInteger next, int lastReserved) {
        next.accumulateAndGet(lastReserved + 1, Math::max);
    }
}
