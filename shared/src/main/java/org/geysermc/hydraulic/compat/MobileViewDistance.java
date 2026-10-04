package org.geysermc.hydraulic.compat;

/** Limits the Java server's chunk and entity tracking requests for mobile clients. */
public final class MobileViewDistance {
    public static final int MAXIMUM = Math.clamp(Integer.getInteger("hydraulic.bedrock.mobile-view-distance", 4), 2, 32);

    private MobileViewDistance() {}

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
