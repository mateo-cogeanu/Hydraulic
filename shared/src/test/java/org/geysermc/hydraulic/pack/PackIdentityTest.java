package org.geysermc.hydraulic.pack;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class PackIdentityTest {
    @Test void entityIsolationRegeneratesPackAndRestoresNormalIdentity() {
        UUID mod = UUID.randomUUID(), converter = UUID.randomUUID();
        UUID normal = PackIdentity.of(mod, converter, true);
        UUID isolated = PackIdentity.of(mod, converter, false);
        assertNotEquals(normal, isolated);
        assertEquals(PackIdentity.of(mod, converter), normal);
        assertEquals(isolated, PackIdentity.of(mod, converter, false));
    }
    @Test void remainsStableAcrossRestartsWithTheSameInputs() {
        UUID mod = UUID.randomUUID(), converter = UUID.randomUUID();
        assertEquals(PackIdentity.of(mod, converter), PackIdentity.of(mod, converter));
    }
    @Test void changingHydraulicInvalidatesAnUnchangedModsBedrockCache() {
        UUID mod = UUID.randomUUID();
        assertNotEquals(PackIdentity.of(mod, UUID.randomUUID()), PackIdentity.of(mod, UUID.randomUUID()));
    }
    @Test void changingTheModAlsoInvalidatesItsPack() {
        UUID converter = UUID.randomUUID();
        assertNotEquals(PackIdentity.of(UUID.randomUUID(), converter), PackIdentity.of(UUID.randomUUID(), converter));
    }
}
