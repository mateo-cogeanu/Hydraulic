package org.geysermc.hydraulic.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.SoundUtils;
import org.geysermc.hydraulic.entity.EntityPackModule;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Convert mod registry entries before MCProtocolLib decodes its vanilla-only enums. */
public final class NativePacketBridge {
    public static final Set<String> PARTICLES = ConcurrentHashMap.newKeySet();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Packet<?> remap(Packet<?> packet, ServerPlayer player, GeyserSession session) {
        UUID owner = player.getUUID();
        if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            boolean changed = false;
            for (var child : bundle.subPackets()) {
                var result = remap(child, player, session);
                if (result != child) changed = true;
                if (result != null) packets.add((Packet) result);
            }
            return changed ? new ClientboundBundlePacket(packets) : packet;
        }
        if (packet instanceof ClientboundAddEntityPacket entity) {
            var profile = EntityPackModule.PROFILES.get(entity.getType());
            if (profile == null) return packet;
            EntityPackModule.TRACKED.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>()).put(entity.getId(), profile);
            return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), entity.getX(), entity.getY(), entity.getZ(),
                    entity.getXRot(), entity.getYRot(), profile.proxy(), 0, entity.getMovement(), entity.getYHeadRot());
        }
        if (packet instanceof ClientboundSetEntityDataPacket metadata) {
            var profile = EntityPackModule.TRACKED.getOrDefault(owner, Map.of()).get(metadata.id());
            if (profile != null) return new ClientboundSetEntityDataPacket(metadata.id(), metadata.packedItems().stream()
                    .filter(value -> value.id() <= profile.metadataLimit()).toList());
        }
        if (packet instanceof ClientboundRemoveEntitiesPacket remove) {
            var tracked = EntityPackModule.TRACKED.get(owner);
            if (tracked != null) remove.entityIds().forEach((int id) -> tracked.remove(id));
        }
        if (packet instanceof ClientboundSoundPacket sound && !sound.getSound().value().location().getNamespace().equals("minecraft")) {
            // Direct holders encode the sound identifier, not an unrecognised mod registry index.
            return new ClientboundSoundPacket(Holder.direct(sound.getSound().value()), sound.getSource(), sound.getX(), sound.getY(), sound.getZ(), sound.getVolume(), sound.getPitch(), sound.getSeed());
        }
        if (packet instanceof ClientboundSoundEntityPacket sound && !sound.getSound().value().location().getNamespace().equals("minecraft")) {
            var entity = player.level().getEntity(sound.getId());
            if (entity != null) {
                var position = Vector3f.from(entity.getX(), entity.getY(), entity.getZ());
                session.executeInEventLoop(() -> SoundUtils.playSound(session, new CustomSound(sound.getSound().value().location().toString(), false, 0), position, sound.getVolume(), sound.getPitch()));
            }
            return null;
        }
        if (packet instanceof ClientboundLevelParticlesPacket particles) {
            String identifier = BuiltInRegistries.PARTICLE_TYPE.getKey(particles.particle().getType()).toString();
            if (!identifier.startsWith("minecraft:")) {
                if (PARTICLES.contains(identifier)) {
                    int count = particles.count() == 0 ? 1 : Math.min(256, particles.count());
                    var random = java.util.concurrent.ThreadLocalRandom.current();
                    for (int i = 0; i < count; i++) {
                        var position = Vector3f.from(particles.x() + random.nextGaussian() * particles.xDist(),
                                particles.y() + random.nextGaussian() * particles.yDist(), particles.z() + random.nextGaussian() * particles.zDist());
                        particle(session, identifier, position);
                    }
                }
                return null;
            }
        }
        if (packet instanceof ClientboundBlockEventPacket event && event.getBlock() == Blocks.NOTE_BLOCK) {
            var below = player.level().getBlockState(event.getPos().below());
            if (BuiltInRegistries.BLOCK.getKey(below.getBlock()).toString().equals("the_sift:sonorous_deepslate")) {
                String mode = below.getProperties().stream().filter(property -> property.getName().equals("mode"))
                        .map(property -> below.getValue(property).toString().toLowerCase(Locale.ROOT)).findFirst().orElse("");
                if (mode.equals("note") || mode.equals("horn")) {
                    if (mode.equals("note")) particle(session, "the_sift:sift_note", Vector3f.from(event.getPos().getX() + 0.5, event.getPos().getY() + 1.2, event.getPos().getZ() + 0.5));
                    // Sift's server sends the actual custom sound separately. Avoid Bedrock's
                    // automatic vanilla instrument sound for this same block event.
                    return null;
                }
            }
        }
        return packet;
    }

    private static void particle(GeyserSession session, String identifier, Vector3f position) {
        session.executeInEventLoop(() -> {
            SpawnParticleEffectPacket effect = new SpawnParticleEffectPacket();
            effect.setIdentifier(identifier);
            effect.setDimensionId(DimensionUtils.javaToBedrock(session));
            effect.setPosition(position);
            session.sendUpstreamPacket(effect);
        });
    }
}
