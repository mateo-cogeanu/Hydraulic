package org.geysermc.hydraulic.sound;

import com.google.auto.service.AutoService;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.pack.context.PackEventContext;
import org.geysermc.hydraulic.pack.context.PackPostProcessContext;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.sound.SoundEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Keeps vanilla event aliases in the protocol sound mapping instead of exporting invalid file paths. */
@AutoService(PackModule.class)
public class SoundPackModule extends PackModule<SoundPackModule> {
    // The 26.3 preview still stores these vanilla mappings under their earlier keys.
    private static final Map<String, String> VANILLA_MAPPING_ALIASES = Map.of(
            "entity.copper_golem.item_get", "entity.copper_golem.no_item_get",
            "entity.copper_golem.item_no_get", "entity.copper_golem.no_item_no_get"
    );
    private final Map<Key, SoundEvent> events = new HashMap<>();
    private final Set<String> aliases = new HashSet<>();

    public SoundPackModule() {
        preProcess(context -> {
            for (SoundEvent event : context.assets(ResourcePack::soundEvents)) events.put(event.key(), event);
        });
        // This runs on every startup, including when the resource pack is cached.
        listenOn(GeyserDefineCustomItemsEvent.class, this::registerAliases);
        postProcess(context -> {
            var definitions = context.bedrockResourcePack().soundDefinitions();
            if (definitions != null) aliases.forEach(definitions.soundDefinitions()::remove);
        });
    }

    private void registerAliases(PackEventContext<GeyserDefineCustomItemsEvent, SoundPackModule> context) {
        int registered = 0;
        for (SoundEvent event : events.values()) {
            if (!event.key().namespace().equals(context.mod().namespace())) continue;
            Key vanilla = SoundAliasResolver.vanillaEvent(event.key(), events);
            if (vanilla == null) continue;
            var mapping = Registries.SOUNDS.get(vanilla.value());
            if (mapping == null && VANILLA_MAPPING_ALIASES.containsKey(vanilla.value())) {
                mapping = Registries.SOUNDS.get(VANILLA_MAPPING_ALIASES.get(vanilla.value()));
            }
            if (mapping == null) {
                context.logger().warn("No Bedrock sound mapping for event {} referenced by {}", vanilla, event.key());
                continue;
            }
            Registries.SOUNDS.get().put(event.key().asString(), mapping);
            aliases.add(event.key().asString());
            registered++;
        }
        if (registered > 0) context.logger().info("Registered {} vanilla sound aliases for {}", registered, context.mod().id());
    }

    @Override
    public boolean test(PackPostProcessContext<SoundPackModule> context) {
        return !context.javaResourcePack().soundEvents().isEmpty();
    }
}
