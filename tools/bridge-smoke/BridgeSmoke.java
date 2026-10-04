package hydraulic.smoke;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import org.geysermc.hydraulic.compat.NativePacketBridge;
import org.geysermc.hydraulic.entity.EntityPackModule;
import org.geysermc.hydraulic.text.ModTranslations;
import java.util.*;

/** Unshipped Fabric smoke mod: tests native-to-MCProtocolLib wire compatibility. */
public class BridgeSmoke implements ModInitializer {
    public void onInitialize() {
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!done.compareAndSet(false, true)) return;
            try {
                var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "HydraulicProbe"), ClientInformation.createDefault());
                if (!org.geysermc.hydraulic.compat.SessionTrafficAccess.class.isAssignableFrom(org.geysermc.geyser.session.GeyserSession.class)) throw new AssertionError("Session traffic mixin missing");
                Class.forName("org.geysermc.geyser.session.GeyserSessionAdapter");
                System.out.println("HYDRAULIC TRAFFIC MIXIN SMOKE PASS: upstream and decode diagnostic hooks applied.");
                int entities = 0, appearances = 0, sounds = 0;
                if (NativePacketBridge.MOD_PARTICLES_ENABLED) throw new AssertionError("Diagnostic particles unexpectedly enabled");
                int filteredParticles = 0;
                for (var particleType : BuiltInRegistries.PARTICLE_TYPE) {
                    if (!BuiltInRegistries.PARTICLE_TYPE.getKey(particleType).getNamespace().equals("the_sift")) continue;
                    if (particleType instanceof net.minecraft.core.particles.ParticleOptions options) {
                        var burst = new ClientboundLevelParticlesPacket(options, true, true, 1, 2, 3, 1, 1, 1, 1, 10000);
                        if (NativePacketBridge.remap(burst, player, null) != null) throw new AssertionError("Mod particle burst not filtered");
                        filteredParticles++;
                    }
                }
                if (filteredParticles != 10) throw new AssertionError("Expected ten filtered mod particles: " + filteredParticles);
                System.out.println("HYDRAULIC PARTICLE FILTER SMOKE PASS: ten 10,000-effect bursts suppressed without upstream work.");
                Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl");
                for (var entry : EntityPackModule.PROFILES.entrySet()) {
                    int id = 1234 + entities;
                    var packet = new ClientboundAddEntityPacket(id, UUID.randomUUID(), 1, 2, 3, 0, 0, entry.getKey(), 0, Vec3.ZERO, 0);
                    var bridged = (ClientboundAddEntityPacket) NativePacketBridge.remap(packet, player, null);
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
                    try {
                        ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer, bridged);
                        var decoded = new org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket(buffer);
                        if (decoded.getEntityId() != id || !decoded.getUuid().equals(packet.getUUID())) throw new AssertionError("Entity identity changed");
                        if (!decoded.getType().name().equals(BuiltInRegistries.ENTITY_TYPE.getKey(entry.getValue().proxy()).getPath().toUpperCase(Locale.ROOT))) throw new AssertionError("Wrong proxy type");
                    } finally { buffer.release(); }
                    if (entry.getValue().proxy() != net.minecraft.world.entity.EntityTypes.SNOWBALL) {
                        var connection = (org.geysermc.geyser.api.connection.GeyserConnection) java.lang.reflect.Proxy.newProxyInstance(
                                getClass().getClassLoader(), new Class<?>[]{org.geysermc.geyser.api.connection.GeyserConnection.class},
                                (proxy, method, arguments) -> method.getName().equals("javaUuid") ? player.getUUID() : null);
                        var selected = new java.util.concurrent.atomic.AtomicReference<org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition>();
                        var spawn = new org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent(connection) {
                            public int entityId() { return id; }
                            public UUID uuid() { return packet.getUUID(); }
                            public org.geysermc.geyser.api.entity.definition.JavaEntityType entityType() { return null; }
                            public org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition definition() { return selected.get(); }
                            public void definition(org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition value) { selected.set(value); }
                            public void preSpawnConsumer(java.util.function.Consumer<org.geysermc.geyser.api.entity.type.GeyserEntity> callback) {}
                            public boolean isCancelled() { return false; }
                            public void setCancelled(boolean value) {}
                        };
                        org.geysermc.geyser.GeyserImpl.getInstance().eventBus().fire(spawn);
                        if (selected.get() == null || !selected.get().identifier().toString().equals(entry.getValue().identifier())) throw new AssertionError("Custom appearance not selected: " + entry.getValue().identifier());
                        appearances++;
                    }
                    var values = List.<SynchedEntityData.DataValue<?>>of(new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte)0), new SynchedEntityData.DataValue<>(30, EntityDataSerializers.INT, 99));
                    var metadata = (ClientboundSetEntityDataPacket) NativePacketBridge.remap(new ClientboundSetEntityDataPacket(id, values), player, null);
                    if (entry.getValue().metadataLimit() != Integer.MAX_VALUE && metadata.packedItems().size() != 1) throw new AssertionError("Custom metadata leaked");
                    entities++;
                }
                for (var sound : BuiltInRegistries.SOUND_EVENT) {
                    if (!sound.location().getNamespace().equals("the_sift")) continue;
                    var packet = new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.RECORDS, 1, 2, 3, 3, 1, 42);
                    var bridged = (ClientboundSoundPacket) NativePacketBridge.remap(packet, player, null);
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
                    try {
                        ClientboundSoundPacket.STREAM_CODEC.encode(buffer, bridged);
                        var decoded = new org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket(buffer);
                        if (!decoded.getSound().getName().equals(sound.location().toString()) || decoded.getVolume() != 3) throw new AssertionError("Sound identity/volume changed");
                        sounds++;
                    } finally { buffer.release(); }
                }
                // Always-ticking test actor is visible in an empty test world without
                // adding a real player or depending on asynchronously promoted chunks.
                var blub = new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW, server.overworld()) {
                    @Override public boolean isAlwaysTicking() { return true; }
                };
                blub.setPos(2, 80, 2);
                if (!server.overworld().addFreshEntity(blub)) throw new AssertionError("Could not add test sound actor");
                if (server.overworld().getEntity(blub.getId()) == null) throw new AssertionError("Test sound actor not tracked");
                var blubSound = BuiltInRegistries.SOUND_EVENT.get(net.minecraft.resources.Identifier.parse("the_sift:entity.blub.idle_water")).orElseThrow().value();
                var entitySound = new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(blubSound), SoundSource.NEUTRAL, blub, 1, 1, 42);
                var positionalSound = (ClientboundSoundPacket) NativePacketBridge.remap(entitySound, player, null);
                if (!positionalSound.getSound().value().location().equals(blubSound.location()) || positionalSound.getY() != 80) throw new AssertionError("Entity sound bridge lost identifier or position");
                blub.discard();
                for (var itemPalette : org.geysermc.geyser.registry.Registries.ITEMS.get().values()) {
                    for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) {
                        String identifier = "the_sift:siftite_" + piece;
                        boolean found = false;
                        for (Object definition : itemPalette.getItemDefinitions().values()) {
                            if (definition.getClass().getMethod("getIdentifier").invoke(definition).equals(identifier)) found = true;
                        }
                        if (!found) throw new AssertionError("Missing Bedrock armor item definition " + identifier);
                    }
                }
                NativePacketBridge.remap(new ClientboundRemoveEntitiesPacket(EntityPackModule.TRACKED.get(player.getUUID()).keySet().stream().mapToInt(Integer::intValue).toArray()), player, null);
                if (!EntityPackModule.TRACKED.get(player.getUUID()).isEmpty()) throw new AssertionError("Entity tracking leak");
                EntityPackModule.TRACKED.remove(player.getUUID());
                for (var block : org.geysermc.geyser.registry.BlockRegistries.CUSTOM_BLOCKS.get()) {
                    if (!block.identifier().startsWith("the_sift:")) continue;
                    java.util.Set<String> presentations = new java.util.TreeSet<>();
                    java.util.List<org.geysermc.geyser.api.block.custom.component.CustomBlockComponents> choices = new java.util.ArrayList<>();
                    choices.add(block.components());
                    block.permutations().forEach(permutation -> choices.add(permutation.components()));
                    for (var components : choices) {
                        if (components == null || components.geometry() == null) continue;
                        presentations.add(components.geometry().identifier() + " " + components.materialInstances().values().stream().map(material -> material.renderMethod()).distinct().sorted().toList());
                    }
                    var methods = choices.stream().filter(java.util.Objects::nonNull).flatMap(c -> c.materialInstances().values().stream())
                            .map(m -> m.renderMethod()).distinct().sorted().toList();
                    if (!java.util.Set.of("the_sift:ichor_glass", "the_sift:ichor_glass_pane", "the_sift:ichor_cauldron").contains(block.identifier()) && methods.contains("blend")) throw new AssertionError("Unexpected blended block " + block.identifier());
                    System.out.println("HYDRAULIC RENDER AUDIT " + block.identifier() + " " + methods + " presentations=" + presentations.size());
                }
                int frameBlocks = 0;
                for (var block : org.geysermc.geyser.registry.BlockRegistries.CUSTOM_BLOCKS.get()) {
                    if (org.geysermc.hydraulic.block.StructureGeometry.isSiftSolidCube(block.identifier())) {
                        var components = block.components();
                        var geometry = components.geometry();
                        if (geometry == null || !geometry.identifier().equals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER)) {
                            boolean permutationMatch = block.permutations().stream().anyMatch(permutation ->
                                    permutation.components().geometry() != null && permutation.components().geometry().identifier().equals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER));
                            if (!permutationMatch) throw new AssertionError("Frame geometry not assigned to " + block.identifier());
                        }
                        frameBlocks++;
                    }
                }
                var convert = org.geysermc.geyser.registry.populator.CustomBlockRegistryPopulator.class.getDeclaredMethod("convertComponents", org.geysermc.geyser.api.block.custom.component.CustomBlockComponents.class);
                convert.setAccessible(true);
                int culled = 0;
                for (var block : org.geysermc.geyser.registry.BlockRegistries.CUSTOM_BLOCKS.get()) {
                    java.util.List<org.geysermc.geyser.api.block.custom.component.CustomBlockComponents> all = new java.util.ArrayList<>();
                    all.add(block.components());
                    for (var permutation : block.permutations()) all.add(permutation.components());
                    for (var components : all) {
                        if (components == null || components.geometry() == null) continue;
                        String id = components.geometry().identifier();
                        if (block.identifier().equals("the_sift:sift_portal")) {
                            if (!id.equals(org.geysermc.hydraulic.compat.PortalPresentation.GEOMETRY)) throw new AssertionError("Portal is not a built-in cube");
                            if (!components.materialInstances().get("*").renderMethod().equals("opaque")) throw new AssertionError("Portal is translucent");
                            System.out.println("HYDRAULIC PORTAL SMOKE PASS: built-in opaque full block.");
                        }
                        if (!id.equals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER)) continue;
                        var nbt = (org.cloudburstmc.nbt.NbtMap) convert.invoke(null, components);
                        if (!nbt.getCompound("minecraft:geometry").getString("culling").equals(org.geysermc.hydraulic.block.StructureGeometry.CULLING)) throw new AssertionError("Culling missing for " + block.identifier());
                        culled++;
                    }
                }
                if (culled < 5) throw new AssertionError("Culling not applied: " + culled);
                System.out.println("HYDRAULIC CULLING SMOKE PASS: " + culled + " cube components have culling rules.");
                if (frameBlocks != 5) throw new AssertionError("Missing portal frame mappings: " + frameBlocks);
                String key = "advancement.the_sift.story.brave_the_unknown.title";
                String translation = org.geysermc.geyser.text.MinecraftLocale.getLocaleStringIfPresent(key, "en_us");
                if (!"Brave the Unknown".equals(translation)) throw new AssertionError("Advancement title not translated: " + translation);
                System.out.println("HYDRAULIC BRIDGE SMOKE PASS: " + entities + " entity spawn/metadata wire round trips; " + sounds + " custom sound wire round trips; " + appearances + " event-bus appearance selections; entity audio, armor identifiers, frame components, tracking cleanup and Geyser advancement translation passed.");
            } catch (Throwable e) {
                System.err.println("HYDRAULIC BRIDGE SMOKE FAILED");
                e.printStackTrace();
            }
        });
    }
}
