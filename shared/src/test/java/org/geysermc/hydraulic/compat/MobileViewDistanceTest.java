package org.geysermc.hydraulic.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MobileViewDistanceTest {
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
