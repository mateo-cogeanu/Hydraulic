package org.geysermc.hydraulic.sound;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.sound.SoundEntry;
import team.unnamed.creative.sound.SoundEvent;
import java.util.HashSet;
import java.util.Map;

public final class SoundAliasResolver {
    private SoundAliasResolver() {
    }

    /** Resolves an unchanged event alias to its vanilla event without treating it as an OGG file. */
    @Nullable
    public static Key vanillaEvent(Key event, Map<Key, SoundEvent> events) {
        var visited = new HashSet<Key>();
        while (visited.add(event)) {
            if (event.namespace().equals(Key.MINECRAFT_NAMESPACE)) return event;
            SoundEvent definition = events.get(event);
            if (definition == null || definition.sounds().size() != 1) return null;
            SoundEntry entry = definition.sounds().getFirst();
            if (entry.type() != SoundEntry.Type.EVENT || entry.volume() != 1 || entry.pitch() != 1) return null;
            event = entry.key();
        }
        return null;
    }
}
