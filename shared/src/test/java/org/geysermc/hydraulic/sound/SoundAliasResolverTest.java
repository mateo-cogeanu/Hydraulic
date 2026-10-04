package org.geysermc.hydraulic.sound;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import team.unnamed.creative.sound.SoundEntry;
import team.unnamed.creative.sound.SoundEvent;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SoundAliasResolverTest {
    private static SoundEvent alias(String id, String target) {
        return SoundEvent.soundEvent(Key.key(id), false, null,
                List.of(SoundEntry.event(target, 1, 1, 1, false, 16, false)));
    }

    @Test
    void siftPortalUsesVanillaEventInsteadOfFilename() {
        var portal = alias("the_sift:block.sift_portal.ambient", "minecraft:block.portal.ambient");
        assertEquals(Key.key("minecraft:block.portal.ambient"),
                SoundAliasResolver.vanillaEvent(portal.key(), Map.of(portal.key(), portal)));
    }

    @Test
    void nestedAliasesResolveAndCyclesTerminate() {
        var first = alias("example:first", "example:second");
        var second = alias("example:second", "minecraft:entity.parrot.ambient");
        assertEquals(Key.key("minecraft:entity.parrot.ambient"),
                SoundAliasResolver.vanillaEvent(first.key(), Map.of(first.key(), first, second.key(), second)));
        second = alias("example:second", "example:first");
        assertNull(SoundAliasResolver.vanillaEvent(first.key(), Map.of(first.key(), first, second.key(), second)));
    }

    @Test
    void audioFilesAreNotEventAliases() {
        var sound = SoundEvent.soundEvent(Key.key("the_sift:entity.singer.song"), false, null,
                List.of(SoundEntry.sound(Key.key("the_sift:singer_song"), 1, 1, 1, false, 16, false)));
        assertNull(SoundAliasResolver.vanillaEvent(sound.key(), Map.of(sound.key(), sound)));
    }
}
