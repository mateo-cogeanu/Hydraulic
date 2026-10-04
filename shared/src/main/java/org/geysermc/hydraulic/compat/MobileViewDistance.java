package org.geysermc.hydraulic.compat;

/** Scopes Bedrock chunk requests and effect budgets to the Sift dimension. */
public final class MobileViewDistance {
    public static final int MAXIMUM = Math.clamp(Integer.getInteger("hydraulic.bedrock.mobile-view-distance", 4), 2, 32);

    private MobileViewDistance() {}

    public static boolean inSift(String world) {
        return "the_sift:the_sift".equals(world);
    }

    /** All Bedrock devices get the Sift cap; other worlds retain the requested distance. */
    public static int forWorld(int requested, String world) {
        return inSift(world) ? Math.min(requested, MAXIMUM) : requested;
    }

    public static int particleCount(int requested, String world, ParticleBudget budget, long now) {
        if (!inSift(world)) return Math.clamp(requested, 0, 256);
        return budget.reserve(requested, now);
    }

    public static int limit(int requested, String deviceOs) {
        return limit(requested, deviceOs, MAXIMUM);
    }

    static int limit(int requested, String deviceOs, int maximum) {
        if (!"IOS".equals(deviceOs) && !"GOOGLE".equals(deviceOs) && !"AMAZON".equals(deviceOs)) {
            return requested;
        }
        return Math.min(requested, Math.clamp(maximum, 2, 32));
    }
}
