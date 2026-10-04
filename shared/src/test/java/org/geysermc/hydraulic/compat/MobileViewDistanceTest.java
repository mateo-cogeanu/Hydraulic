package org.geysermc.hydraulic.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MobileViewDistanceTest {
    @Test void worldLimitAppliesOnlyInsideSift() {
        assertEquals(4, MobileViewDistance.forWorld(8, "the_sift:the_sift"));
        assertEquals(2, MobileViewDistance.forWorld(2, "the_sift:the_sift"));
        for (String world : new String[]{"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "other:world"}) {
            assertEquals(12, MobileViewDistance.forWorld(12, world));
        }
    }
    @Test void overworldEffectsAreRestoredWhileSiftBurstsAreBounded() {
        var budget = new ParticleBudget();
        assertEquals(100, MobileViewDistance.particleCount(100, "minecraft:overworld", true, budget, 1_000_000_000));
        assertEquals(0, MobileViewDistance.particleCount(100, "the_sift:the_sift", true, budget, 1_000_000_000));
        assertEquals(8, MobileViewDistance.particleCount(100, "the_sift:the_sift", false, budget, 1_000_000_000));
        assertEquals(0, MobileViewDistance.particleCount(100, "the_sift:the_sift", false, budget, 1_000_000_000));
        assertEquals(100, MobileViewDistance.particleCount(100, "minecraft:overworld", false, budget, 1_000_000_000));
    }
    @Test void limitsPhoneAndTabletRequests() {
        for (String os : new String[]{"IOS", "GOOGLE", "AMAZON"}) {
            assertEquals(4, MobileViewDistance.limit(8, os, 4));
            assertEquals(4, MobileViewDistance.limit(32, os, 4));
        }
    }
    @Test void preservesSmallerRequests() {
        assertEquals(2, MobileViewDistance.limit(2, "IOS", 4));
        assertEquals(3, MobileViewDistance.limit(3, "GOOGLE", 4));
    }
    @Test void leavesOtherPlatformsUnchanged() {
        for (String os : new String[]{"UNKNOWN", "UWP", "WIN32", "LINUX", "OSX", "NX", "XBOX", "PS4"}) {
            assertEquals(16, MobileViewDistance.limit(16, os, 4));
        }
        assertEquals(16, MobileViewDistance.limit(16, null, 4));
    }
    @Test void boundsConfiguredMaximum() {
        assertEquals(2, MobileViewDistance.limit(8, "IOS", -10));
        assertEquals(32, MobileViewDistance.limit(64, "IOS", 100));
        assertEquals(8, MobileViewDistance.limit(8, "IOS", 12));
    }
}
