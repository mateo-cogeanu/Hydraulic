package org.geysermc.hydraulic.compat;

/** Per-player budget: at most eight effects in 250 ms, even across multiple emitters. */
public final class ParticleBudget {
    private long windowStart;
    private int used;
    public synchronized int reserve(int requested, long now) {
        if (now - windowStart >= 250_000_000L || now < windowStart) {
            windowStart = now;
            used = 0;
        }
        int allowed = Math.min(Math.max(0, requested), 8 - used);
        used += allowed;
        return allowed;
    }
}
