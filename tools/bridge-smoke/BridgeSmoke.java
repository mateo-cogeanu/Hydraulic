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
                int entities = 0, sounds = 0;
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
                NativePacketBridge.remap(new ClientboundRemoveEntitiesPacket(EntityPackModule.TRACKED.get(player.getUUID()).keySet().stream().mapToInt(Integer::intValue).toArray()), player, null);
                if (!EntityPackModule.TRACKED.get(player.getUUID()).isEmpty()) throw new AssertionError("Entity tracking leak");
                EntityPackModule.TRACKED.remove(player.getUUID());
                String key = "advancement.the_sift.story.brave_the_unknown.title";
                String translation = org.geysermc.geyser.text.MinecraftLocale.getLocaleStringIfPresent(key, "en_us");
                if (!"Brave the Unknown".equals(translation)) throw new AssertionError("Advancement title not translated: " + translation);
                System.out.println("HYDRAULIC BRIDGE SMOKE PASS: " + entities + " entity spawn/metadata wire round trips; " + sounds + " custom sound wire round trips; tracking cleanup and Geyser advancement translation passed.");
            } catch (Throwable e) {
                System.err.println("HYDRAULIC BRIDGE SMOKE FAILED");
                e.printStackTrace();
            }
        });
    }
}
