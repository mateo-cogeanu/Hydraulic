package org.geysermc.hydraulic.compat;

import java.util.*;

/** Suppress exact duplicates within a server tick; retain corrections on every new tick. */
public final class EntityUpdateCache {
    private long tick = Long.MIN_VALUE;
    private final Map<Integer, Object> motions = new HashMap<>();
    public synchronized boolean duplicate(long tick, int id, Object motion) {
        if (this.tick != tick) { this.tick = tick; motions.clear(); }
        return Objects.equals(motions.put(id, motion), motion);
    }
    public synchronized void invalidate(int id) { motions.remove(id); }
}
