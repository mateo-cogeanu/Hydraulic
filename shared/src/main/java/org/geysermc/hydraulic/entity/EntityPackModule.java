package org.geysermc.hydraulic.entity;

import com.google.auto.service.AutoService;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.pack.context.PackPostProcessContext;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@AutoService(PackModule.class)
public class EntityPackModule extends PackModule<EntityPackModule> {
    public record Profile(String identifier, EntityType<?> proxy, int metadataLimit, float width, float height,
                          String modelName, String texture, String builtinGeometry, float scale) {}
    public static final Map<EntityType<?>, Profile> PROFILES = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<Integer, Profile>> TRACKED = new ConcurrentHashMap<>();
    private static final Map<String, CustomEntityDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    public EntityPackModule() {
        preProcess(context -> {
            for (var type : BuiltInRegistries.ENTITY_TYPE) {
                var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (!key.getNamespace().equals(context.mod().namespace())) continue;
                String name = key.getPath();
                String model = name;
                String texture = "entity/" + name;
                String builtin = null;
                EntityType<?> proxy = EntityTypes.PIG;
                int limit = context.mod().id().equals("the_sift") ? 15 : 7;
                float scale = 1;
                if (context.mod().id().equals("the_sift")) {
                    switch (name) {
                        case "rift" -> { texture = "entity/rift/sift"; proxy = EntityTypes.ARMOR_STAND; limit = 7; }
                        case "mini_rift" -> { model = "rift"; texture = "entity/rift/overworld"; proxy = EntityTypes.ARMOR_STAND; limit = 7; scale = 0.12f; }
                        case "dark_sniffer" -> { builtin = "geometry.sniffer"; proxy = EntityTypes.SNIFFER; }
                        case "overgrown_willow_boat" -> { builtin = "geometry.boat"; texture = "entity/boat/overgrown_willow"; proxy = EntityTypes.OAK_BOAT; limit = Integer.MAX_VALUE; }
                        case "overgrown_willow_chest_boat" -> { builtin = "geometry.chest_boat"; texture = "entity/chest_boat/overgrown_willow"; proxy = EntityTypes.OAK_CHEST_BOAT; limit = Integer.MAX_VALUE; }
                        case "siftite_return" -> { proxy = EntityTypes.SNOWBALL; limit = 7; }
                    }
                }
                if (builtin == null && context.mod().resolveFile("assets/" + key.getNamespace()
                        + "/geckolib/models/entity/" + model + ".geo.json") == null && proxy != EntityTypes.SNOWBALL) continue;
                PROFILES.put(type, new Profile(key.toString(), proxy, limit, type.getWidth(), type.getHeight(), model, texture, builtin, scale));
            }
        });
        listenOn(GeyserDefineEntitiesEvent.class, context -> {
            int count = 0;
            for (Profile profile : PROFILES.values()) {
                if (!profile.identifier().startsWith(context.mod().namespace() + ":") || profile.proxy() == EntityTypes.SNOWBALL) continue;
                CustomEntityDefinition definition = CustomEntityDefinition.of(profile.identifier());
                context.event().register(definition);
                DEFINITIONS.put(profile.identifier(), definition);
                count++;
            }
            if (count > 0) context.logger().info("Registered {} custom entity appearances for {}", count, context.mod().id());
        });
        // These events are handled directly: PackManager dispatches lifecycle listeners once per mod.
        postProcess(this::postProcess);
    }

    @org.geysermc.event.subscribe.Subscribe
    public void onSpawn(ServerSpawnEntityEvent event) {
        Profile profile = TRACKED.getOrDefault(event.connection().javaUuid(), Map.of()).get(event.entityId());
        if (profile == null) return;
        var definition = DEFINITIONS.get(profile.identifier());
        if (definition == null) return;
        event.definition(definition);
        event.preSpawnConsumer(entity -> {
            entity.override(GeyserEntityDataTypes.WIDTH, profile.width());
            entity.override(GeyserEntityDataTypes.HEIGHT, profile.height());
            entity.override(GeyserEntityDataTypes.SCALE, profile.scale());
        });
    }

    @org.geysermc.event.subscribe.Subscribe
    public void onDisconnect(SessionDisconnectEvent event) {
        UUID uuid = event.connection().javaUuid();
        if (uuid != null) TRACKED.remove(uuid);
    }

    private void postProcess(PackPostProcessContext<EntityPackModule> context) {
        for (Profile profile : PROFILES.values()) {
            if (!profile.identifier().startsWith(context.mod().namespace() + ":") || profile.proxy() == EntityTypes.SNOWBALL) continue;
            String name = profile.identifier().split(":")[1];
            String geometry = profile.builtinGeometry();
            JsonObject animations = null;
            try {
                if (geometry == null) {
                    geometry = "geometry.hydraulic." + context.mod().namespace() + "." + name;
                    var model = read(context.mod(), "geckolib/models/entity/" + profile.modelName() + ".geo.json");
                    context.bedrockResourcePack().addExtraFile(GeckoAssets.geometry(model, geometry), "models/entity/" + name + ".geo.json");
                    var file = context.mod().resolveFile("assets/" + context.mod().namespace() + "/geckolib/animations/entity/" + profile.modelName() + ".animation.json");
                    if (file != null) {
                        animations = GeckoAssets.animations(JsonParser.parseString(Files.readString(file)).getAsJsonObject(), "animation.hydraulic." + context.mod().namespace() + "." + name);
                        context.bedrockResourcePack().addExtraFile(animations, "animations/" + name + ".animation.json");
                    }
                }
                String texture = "textures/hydraulic/" + context.mod().namespace() + "/" + profile.texture();
                Path image = context.mod().resolveFile("assets/" + context.mod().namespace() + "/textures/" + profile.texture() + ".png");
                if (image == null) throw new IllegalStateException("Missing entity texture " + profile.texture());
                context.bedrockResourcePack().addExtraFile(Files.readAllBytes(image), texture + ".png");
                JsonObject description = GSON.toJsonTree(Map.of("identifier", profile.identifier(),
                        "materials", Map.of("default", "entity_alphatest"), "textures", Map.of("default", texture),
                        "geometry", Map.of("default", geometry), "render_controllers", List.of("controller.render.hydraulic.entity"))).getAsJsonObject();
                if (animations != null) {
                    JsonObject available = animations.getAsJsonObject("animations");
                    Map<String, String> names = new LinkedHashMap<>();
                    for (String key : available.keySet()) names.put(key.substring(key.lastIndexOf('.') + 1), key);
                    description.add("animations", GSON.toJsonTree(names));
                    String idle = first(names, "idle", "flapping", "appear");
                    String walk = first(names, "walk", "running", "hopping");
                    JsonArray animate = new JsonArray();
                    if (idle != null) animate.add(GSON.toJsonTree(Map.of(idle, walk == null ? "1.0" : "query.modified_move_speed < 0.01")));
                    if (walk != null) animate.add(GSON.toJsonTree(Map.of(walk, "query.modified_move_speed >= 0.01")));
                    description.add("scripts", GSON.toJsonTree(Map.of("animate", animate)));
                }
                context.bedrockResourcePack().addExtraFile(GSON.toJsonTree(Map.of("format_version", "1.10.0", "minecraft:client_entity", Map.of("description", description))), "entity/" + name + ".entity.json");
            } catch (Exception e) {
                throw new IllegalStateException("Could not convert entity " + profile.identifier(), e);
            }
        }
        context.bedrockResourcePack().addExtraFile(GSON.toJsonTree(Map.of("format_version", "1.8.0", "render_controllers", Map.of("controller.render.hydraulic.entity", Map.of("geometry", "Geometry.default", "materials", List.of(Map.of("*", "Material.default")), "textures", List.of("Texture.default"))))), "render_controllers/hydraulic_entity.json");
    }

    private static String first(Map<String, String> names, String... choices) {
        for (String choice : choices) if (names.containsKey(choice)) return choice;
        return null;
    }
    private static JsonObject read(ModInfo mod, String path) throws Exception {
        return JsonParser.parseString(Files.readString(Objects.requireNonNull(mod.resolveFile("assets/" + mod.namespace() + "/" + path)))).getAsJsonObject();
    }
}
