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
    private static int readBedrockInt(io.netty.buffer.ByteBuf buffer) {
        int raw = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int value = buffer.readUnsignedByte();
            raw |= (value & 127) << shift;
            if ((value & 128) == 0) return (raw >>> 1) ^ -(raw & 1);
        }
        throw new AssertionError("Oversized Bedrock varint");
    }
    public void onInitialize() {
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!done.compareAndSet(false, true)) return;
            try {
                var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "HydraulicProbe"), ClientInformation.createDefault());
                if (!org.geysermc.hydraulic.compat.SessionTrafficAccess.class.isAssignableFrom(org.geysermc.geyser.session.GeyserSession.class)) throw new AssertionError("Session traffic mixin missing");
                Class.forName("org.geysermc.geyser.session.GeyserSessionAdapter");
                System.out.println("HYDRAULIC TRAFFIC MIXIN SMOKE PASS: upstream and decode diagnostic hooks applied.");
                // Allocate a field-only fixture; no network session is opened by this unshipped probe.
                var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                var sessionClass = org.geysermc.geyser.session.GeyserSession.class;
                var sessionFixture = unsafe.allocateInstance(sessionClass);
                var recipeField = sessionClass.getDeclaredField("lastRecipeNetId");
                recipeField.setAccessible(true);
                var recipeCounter = new java.util.concurrent.atomic.AtomicInteger(org.geysermc.geyser.util.InventoryUtils.LAST_RECIPE_NET_ID + 1);
                recipeField.set(sessionFixture, recipeCounter);
                var reservationHook = java.util.Arrays.stream(sessionClass.getDeclaredMethods())
                        .filter(method -> method.getName().contains("hydraulic$reserveMapRecipeIds")).findFirst().orElseThrow();
                reservationHook.setAccessible(true);
                reservationHook.invoke(sessionFixture, new Object[]{null});
                java.util.Set<Integer> mapRecipeIds = new java.util.HashSet<>();
                for(Object recipe : org.geysermc.geyser.inventory.recipe.RecipeUtil.CARTOGRAPHY_RECIPES) {
                    mapRecipeIds.add((int)recipe.getClass().getMethod("getNetId").invoke(recipe));
                }
                int firstDynamic = recipeCounter.getAndIncrement();
                if(mapRecipeIds.size()!=4 || firstDynamic<=java.util.Collections.max(mapRecipeIds))throw new AssertionError("Map recipe IDs collide with first dynamic recipe");
                reservationHook.invoke(sessionFixture, new Object[]{null});
                if(recipeCounter.get()!=firstDynamic+1)throw new AssertionError("Recipe IDs rewound after allocation");
                System.out.println("HYDRAULIC RECIPE IDS SMOKE PASS: live constructor hook reserves four map IDs before dynamic recipes and never rewinds allocated IDs.");
                var clientField = sessionClass.getDeclaredField("clientData");
                var viewField = sessionClass.getDeclaredField("clientRenderDistance");
                var serverViewField = sessionClass.getDeclaredField("serverRenderDistance");
                var worldField = sessionClass.getDeclaredField("worldName");
                var renderMethod = sessionClass.getDeclaredMethod("getRenderDistance");
                for (var field : java.util.List.of(clientField, viewField, serverViewField, worldField)) field.setAccessible(true);
                renderMethod.setAccessible(true);
                var clientFixture = new org.geysermc.geyser.session.auth.BedrockClientData();
                var osField = clientFixture.getClass().getDeclaredField("deviceOs");
                osField.setAccessible(true);
                clientField.set(sessionFixture, clientFixture);
                viewField.setInt(sessionFixture, 8);
                worldField.set(sessionFixture, net.kyori.adventure.key.Key.key("the_sift:the_sift"));
                for (var os : org.geysermc.floodgate.util.DeviceOs.values()) {
                    osField.set(clientFixture, os);
                    int expected = 4;
                    if ((int) renderMethod.invoke(sessionFixture) != expected) throw new AssertionError("Incorrect view for " + os);
                    worldField.set(sessionFixture, net.kyori.adventure.key.Key.key("minecraft:overworld"));
                    if ((int) renderMethod.invoke(sessionFixture) != 8) throw new AssertionError("Overworld still limited");
                    worldField.set(sessionFixture, net.kyori.adventure.key.Key.key("the_sift:the_sift"));
                }
                osField.set(clientFixture, org.geysermc.floodgate.util.DeviceOs.IOS);
                viewField.setInt(sessionFixture, -1);
                serverViewField.setInt(sessionFixture, 16);
                if ((int) renderMethod.invoke(sessionFixture) != 4) throw new AssertionError("Login server view not capped");
                serverViewField.setInt(sessionFixture, -1);
                if ((int) renderMethod.invoke(sessionFixture) != 2) throw new AssertionError("Initial view changed");
                System.out.println("HYDRAULIC MOBILE VIEW SMOKE PASS: live Geyser mixin caps the Sift on every Bedrock OS, restores Overworld distance and handles login defaults.");
                String protocolPrefix = "org.geysermc.geyser.shaded.org.cloudburstmc.protocol.bedrock.";
                var diagnosticsField = sessionClass.getDeclaredField("hydraulic$diagnostics");
                diagnosticsField.setAccessible(true);
                diagnosticsField.set(sessionFixture, new org.geysermc.hydraulic.compat.TrafficDiagnostics());
                var startClass = Class.forName(protocolPrefix+"packet.StartGamePacket");
                Object startPacket = startClass.getConstructor().newInstance();
                var experimentMethod = sessionClass.getDeclaredMethod("configureExperiments", startClass);
                experimentMethod.setAccessible(true);
                experimentMethod.invoke(sessionFixture,startPacket);
                var experiments = (java.util.List<?>) startClass.getMethod("getExperiments").invoke(startPacket);
                int customBiomesEnabled=0;
                for(Object experiment:experiments) {
                    if(experiment.getClass().getMethod("getName").invoke(experiment).equals("data_driven_biomes")
                            && (boolean)experiment.getClass().getMethod("isEnabled").invoke(experiment))customBiomesEnabled++;
                }
                if(customBiomesEnabled!=1)throw new AssertionError("Custom Sift biomes sent without their login experiment");
                if(!((org.geysermc.hydraulic.compat.SessionTrafficAccess)sessionFixture).hydraulic$traffic().disconnectSummary().contains("customBiomeCount=10"))throw new AssertionError("Login diagnostic snapshot missing");
                if(java.util.Arrays.stream(sessionClass.getDeclaredMethods()).noneMatch(method->method.getName().contains("hydraulic$reportDisconnect")))throw new AssertionError("Short disconnect diagnostic hook missing");
                System.out.println("HYDRAULIC LOGIN EXPERIMENT SMOKE PASS: ten custom biomes have the required StartGame experiment; short-disconnect hook and settings snapshot applied.");
                var voxelClass = Class.forName(protocolPrefix + "packet.VoxelShapesPacket");
                var voxelPacket = voxelClass.getDeclaredConstructor().newInstance();
                voxelClass.getMethod("setShapes", List.class).invoke(voxelPacket, new java.util.ArrayList<>());
                voxelClass.getMethod("setNameMap", Map.class).invoke(voxelPacket, new java.util.HashMap<>());
                var voxelHook = java.util.Arrays.stream(sessionClass.getDeclaredMethods())
                        .filter(method -> method.getName().contains("supplyBuiltinVoxelShapes")).findFirst().orElseThrow();
                voxelHook.setAccessible(true); voxelHook.invoke(sessionFixture, voxelPacket);
                var shapeList = (List<?>) voxelClass.getMethod("getShapes").invoke(voxelPacket);
                var shapeNames = (Map<?,?>) voxelClass.getMethod("getNameMap").invoke(voxelPacket);
                if (shapeList.size() != 2 || !shapeNames.keySet().containsAll(List.of("minecraft:empty", "minecraft:unit_cube"))) throw new AssertionError("Built-in voxel registry not supplied");
                var voxelCodec = Class.forName(protocolPrefix + "codec.v2193.Bedrock_v2193").getField("CODEC").get(null);
                var helperClass = Class.forName(protocolPrefix + "codec.BedrockCodecHelper");
                var packetClass = Class.forName(protocolPrefix + "packet.BedrockPacket");
                var voxelBuffer = Unpooled.buffer();
                try {
                    var codecHelper = voxelCodec.getClass().getMethod("createHelper").invoke(voxelCodec);
                    voxelCodec.getClass().getMethod("tryEncode", helperClass, io.netty.buffer.ByteBuf.class, packetClass).invoke(voxelCodec, codecHelper, voxelBuffer, voxelPacket);
                    var definition = voxelCodec.getClass().getMethod("getPacketDefinition", Class.class).invoke(voxelCodec, voxelClass);
                    int packetId = (int) definition.getClass().getMethod("getId").invoke(definition);
                    var decodedVoxel = voxelCodec.getClass().getMethod("tryDecode", helperClass, io.netty.buffer.ByteBuf.class, int.class).invoke(voxelCodec, codecHelper, voxelBuffer, packetId);
                    if (!decodedVoxel.equals(voxelPacket) || voxelBuffer.isReadable()) throw new AssertionError("Voxel registry wire mismatch");
                } finally { voxelBuffer.release(); }
                System.out.println("HYDRAULIC VOXEL REGISTRY SMOKE PASS: login mixin supplies both built-ins; protocol 2193 wire round trip passed.");
                var nativeSpear = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("the_sift:siftite_spear"));
                var nativeRange=nativeSpear.components().get(net.minecraft.core.component.DataComponents.ATTACK_RANGE);
                var nativeKinetic=nativeSpear.components().get(net.minecraft.core.component.DataComponents.KINETIC_WEAPON);
                int spearId=BuiltInRegistries.ITEM.getId(nativeSpear);
                var spearMapping=org.geysermc.geyser.registry.Registries.ITEMS.get().values().iterator().next().getMapping(spearId);
                var baseField=org.geysermc.geyser.item.type.Item.class.getDeclaredField("baseComponents");baseField.setAccessible(true);
                var spearComponents=(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents)baseField.get(spearMapping.getJavaItem());
                var bedrockRange=spearComponents.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes.ATTACK_RANGE);
                var bedrockKinetic=spearComponents.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes.KINETIC_WEAPON);
                if(bedrockRange==null || bedrockRange.maxReach()!=nativeRange.maxReach() || bedrockRange.minReach()!=nativeRange.minReach()
                        || bedrockKinetic==null || bedrockKinetic.delayTicks()!=nativeKinetic.delayTicks()
                        || spearComponents.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes.PIERCING_WEAPON)==null) throw new AssertionError("Native spear combat components missing");
                var bucket=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("the_sift:ichor_bucket"));
                for(var items:org.geysermc.geyser.registry.Registries.ITEMS.get().values()) {
                    if(!items.getBuckets().contains(items.getMapping(BuiltInRegistries.ITEM.getId(bucket)).getClass().getMethod("getBedrockDefinition").invoke(items.getMapping(BuiltInRegistries.ITEM.getId(bucket)))))throw new AssertionError("Ichor bucket omitted from native-use path");
                }
                if(org.geysermc.hydraulic.block.IchorFluidLayer.LEVELS.size()!=16)throw new AssertionError("Incomplete Ichor fluid states");
                int fluidChunkChecks = 0;
                for(var blocks:org.geysermc.geyser.registry.BlockRegistries.BLOCKS.get().values()) {
                    Object waterDefinition=blocks.getClass().getMethod("getBedrockWater").invoke(blocks);
                    int air=blocks.getBedrockAir().getRuntimeId(), water=(int)waterDefinition.getClass().getMethod("getRuntimeId").invoke(waterDefinition);
                    var nativePalette=org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette.createForBlockState(0,65536);
                    for(var entry:org.geysermc.hydraulic.block.IchorFluidLayer.LEVELS.entrySet())nativePalette.set(entry.getValue(),0,0,entry.getKey());
                    for(boolean singleton:new boolean[]{false,true}) {
                        var bits=singleton?org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray.INSTANCE:
                                org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion.V1.createArray(4096);
                        var original=new org.geysermc.geyser.level.chunk.BlockStorage(bits,singleton?
                                it.unimi.dsi.fastutil.ints.IntLists.singleton(water):it.unimi.dsi.fastutil.ints.IntList.of(air,water));
                        if(!singleton)for(int x=0;x<16;x++)bits.set(x<<8,1);
                        var first=new org.geysermc.geyser.level.chunk.BlockStorage(air);
                        var section=new org.geysermc.geyser.level.chunk.GeyserChunkSection(new org.geysermc.geyser.level.chunk.BlockStorage[]{first,original},0);
                        org.geysermc.hydraulic.block.IchorFluidLayer.apply(blocks,new org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette[]{nativePalette},
                                new org.geysermc.geyser.level.chunk.GeyserChunkSection[]{section},0);
                        for(var entry:org.geysermc.hydraulic.block.IchorFluidLayer.LEVELS.entrySet()) {
                            Object fluid=org.geysermc.hydraulic.block.IchorFluidLayer.class.getMethod("fluid",org.geysermc.geyser.registry.type.BlockMappings.class,int.class).invoke(null,blocks,entry.getKey());
                            int expected=(int)fluid.getClass().getMethod("getRuntimeId").invoke(fluid);
                            if(section.getBlockStorageArray()[1].getFullBlock(entry.getValue()<<8)!=expected)throw new AssertionError("Ichor chunk depth lost");
                            if(original.getFullBlock(entry.getValue()<<8)!=water)throw new AssertionError("Original immutable water layer changed");
                            fluidChunkChecks++;
                        }
                        if(section.getBlockStorageArray()[0]!=first || section.getBlockStorageArray()[1].getFullBlock(1)!=(singleton?water:air))throw new AssertionError("Non-Ichor cell changed");
                    }
                }
                System.out.println("HYDRAULIC ICHOR CHUNK SMOKE PASS: "+fluidChunkChecks+" depth checks with immutable singleton/two-entry water palettes; original layers and non-Ichor cells preserved.");
                for(var entry:org.geysermc.hydraulic.block.IchorFluidLayer.LEVELS.entrySet()) {
                    if(org.geysermc.hydraulic.block.JavaBlockStateRemapper.translate(entry.getKey())!=entry.getKey())throw new AssertionError("Ichor visual still maps to vanilla water");
                    for(var blocks:org.geysermc.geyser.registry.BlockRegistries.BLOCKS.get().values()) {
                        Object water=org.geysermc.hydraulic.block.IchorFluidLayer.class.getMethod("fluid", org.geysermc.geyser.registry.type.BlockMappings.class,int.class).invoke(null,blocks,entry.getKey());
                        var state=(org.cloudburstmc.nbt.NbtMap)water.getClass().getMethod("getState").invoke(water);
                        if(!java.util.Set.of("minecraft:water","minecraft:flowing_water").contains(state.getString("name")) || state.getCompound("states").getInt("liquid_depth")!=entry.getValue())throw new AssertionError("Incorrect Ichor fluid layer: "+state);
                    }
                }
                Object definitions=org.geysermc.geyser.registry.Registries.BIOMES.get();
                Map<?,?> definitionMap=(Map<?,?>)definitions.getClass().getMethod("getDefinitions").invoke(definitions);
                if(org.geysermc.hydraulic.compat.SiftBiomes.IDS.size()!=10)throw new AssertionError("Missing Sift biome definitions");
                for(var entry:org.geysermc.hydraulic.compat.SiftBiomes.IDS.entrySet()) {
                    if(entry.getValue()<30000 || entry.getValue()>32767)throw new AssertionError("Custom biome outside Bedrock reserved range");
                    if(definitionMap.get(entry.getKey()).getClass().getMethod("getChunkGenData").invoke(definitionMap.get(entry.getKey()))!=null)throw new AssertionError("Client worldgen payload present for proxied biome");
                    if(!definitionMap.containsKey(entry.getKey()) || !definitionMap.get(entry.getKey()).getClass().getMethod("getId").invoke(definitionMap.get(entry.getKey())).equals(entry.getValue()))throw new AssertionError("Sift biome ID mismatch");
                }
                Object biomePacket=Class.forName(protocolPrefix+"packet.BiomeDefinitionListPacket").getConstructor().newInstance();
                biomePacket.getClass().getMethod("setBiomes",definitions.getClass()).invoke(biomePacket,definitions);
                var biomeBuffer=Unpooled.buffer();
                try {
                    var helper=voxelCodec.getClass().getMethod("createHelper").invoke(voxelCodec);
                    voxelCodec.getClass().getMethod("tryEncode",helperClass,io.netty.buffer.ByteBuf.class,packetClass).invoke(voxelCodec,helper,biomeBuffer,biomePacket);
                    var definition=voxelCodec.getClass().getMethod("getPacketDefinition",Class.class).invoke(voxelCodec,biomePacket.getClass());
                    int packetId=(int)definition.getClass().getMethod("getId").invoke(definition);
                    var decoded=voxelCodec.getClass().getMethod("tryDecode",helperClass,io.netty.buffer.ByteBuf.class,int.class).invoke(voxelCodec,helper,biomeBuffer,packetId);
                    Object decodedDefinitions=decoded.getClass().getMethod("getBiomes").invoke(decoded);
                    var decodedMap=(Map<?,?>)decodedDefinitions.getClass().getMethod("getDefinitions").invoke(decodedDefinitions);
                    for(var entry:org.geysermc.hydraulic.compat.SiftBiomes.IDS.entrySet()) {
                    if(entry.getValue()<30000 || entry.getValue()>32767)throw new AssertionError("Custom biome outside Bedrock reserved range");
                        Object value=decodedMap.get(entry.getKey());if(value==null || !value.getClass().getMethod("getId").invoke(value).equals(entry.getValue()))throw new AssertionError("Sift biome wire mismatch");
                    }
                    if(biomeBuffer.isReadable())throw new AssertionError("Unread biome bytes");
                } finally {biomeBuffer.release();}
                var beamBlock=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("the_sift:sonorous_deepslate"));
                var nativeBeam=((net.minecraft.world.level.block.EntityBlock)beamBlock).newBlockEntity(net.minecraft.core.BlockPos.ZERO,beamBlock.defaultBlockState());
                nativeBeam.getClass().getMethod("startBeam",int.class).invoke(nativeBeam,0x33ccff);
                if(!(boolean)nativeBeam.getClass().getMethod("hasBeam").invoke(nativeBeam) || (int)nativeBeam.getClass().getMethod("getBeamColor").invoke(nativeBeam)!=0x33ccff)throw new AssertionError("Sonorous state reflection failed");
                System.out.println("HYDRAULIC RESTORATION SMOKE PASS: spear components, bucket use list, 16 exact fluid layers, 10 biome IDs on protocol 2193, original Sonorous beam state.");
                int entities = 0, appearances = 0, sounds = 0;
                Class.forName("org.geysermc.geyser.translator.protocol.java.JavaRespawnTranslator");
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
                System.out.println("HYDRAULIC PARTICLE FILTER SMOKE PASS: ten mod particle packets safely intercepted without vanilla registry decoding.");
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
                        if (org.geysermc.hydraulic.HydraulicImpl.instance().getConfig().customEntityAppearances()) {
                            if (selected.get() == null || !selected.get().identifier().toString().equals(entry.getValue().identifier())) throw new AssertionError("Custom appearance not selected: " + entry.getValue().identifier());
                            appearances++;
                        } else if (selected.get() != null) {
                            throw new AssertionError("Custom appearance selected during vanilla isolation");
                        }
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
                var manager = org.geysermc.hydraulic.HydraulicImpl.instance().getPackManager();
                var modulesField = manager.getClass().getDeclaredField("modules"); modulesField.setAccessible(true);
                var modules = (List<?>) modulesField.get(manager);
                var blockModule = modules.stream().filter(org.geysermc.hydraulic.block.BlockPackModule.class::isInstance).findFirst().orElseThrow();
                var statesField = blockModule.getClass().getDeclaredField("resolvedStates"); statesField.setAccessible(true);
                var resolved = (Map<?, ?>) statesField.get(blockModule);
                for (var block : org.geysermc.geyser.registry.BlockRegistries.CUSTOM_BLOCKS.get()) {
                    if (!block.identifier().startsWith("the_sift:")) continue;
                    var defaultNative = BuiltInRegistries.BLOCK.get(net.minecraft.resources.Identifier.parse(block.identifier())).orElseThrow().value().defaultBlockState();
                    if (resolved.containsKey(net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(defaultNative))
                            && (block.components().geometry() == null || block.components().materialInstances().isEmpty())) {
                        throw new AssertionError("Sift base presentation lacks geometry/material: " + block.identifier());
                    }
                    if (block.identifier().equals("the_sift:siftslate")) {
                        if (!block.components().geometry().identifier().equals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER)) throw new AssertionError("Siftslate base geometry mismatch");
                        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
                            var material = block.components().materialInstances().get(face);
                            if (material == null || !"the_sift:siftslate".equals(material.texture()) || !"opaque".equals(material.renderMethod())) throw new AssertionError("Siftslate base face material mismatch: " + face);
                        }
                    }
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
                    if (!java.util.Set.of("the_sift:ichor", "the_sift:ichor_glass", "the_sift:ichor_glass_pane", "the_sift:ichor_cauldron").contains(block.identifier()) && methods.contains("blend")) throw new AssertionError("Unexpected blended block " + block.identifier());
                    System.out.println("HYDRAULIC RENDER AUDIT " + block.identifier() + " " + methods + " presentations=" + presentations.size());
                }
                System.out.println("HYDRAULIC BASE PRESENTATION AUDIT PASS: all modeled defaults have geometry/material; Siftslate binds all six opaque faces to its texture.");
                int conditionStates = 0;
                for (var block : org.geysermc.geyser.registry.BlockRegistries.CUSTOM_BLOCKS.get()) {
                    if (!block.identifier().startsWith("the_sift:") || block.permutations().isEmpty()) continue;
                    var nativeBlock = BuiltInRegistries.BLOCK.get(net.minecraft.resources.Identifier.parse(block.identifier())).orElseThrow().value();
                    for (var state : nativeBlock.getStateDefinition().getPossibleStates()) {
                        Map<String, String> values = new HashMap<>();
                        for (var property : state.getProperties()) values.put(property.getName(), state.getValue(property).toString().toLowerCase(java.util.Locale.ROOT));
                        long matches = block.permutations().stream().filter(p -> matchesCondition(p.condition(), values)).count();
                        if (!block.identifier().equals("the_sift:ichor") && !resolved.containsKey(net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(state))) {
                            if (matches != 0) throw new AssertionError("Previously absent model gained a permutation: " + state);
                            continue;
                        }
                        if (matches != 1) throw new AssertionError("Permutation coverage " + matches + " for " + state);
                        conditionStates++;
                    }
                }
                System.out.println("HYDRAULIC PERMUTATION AUDIT PASS: " + conditionStates + " native states select exactly one shared definition.");
                int wirePositions = 0;
                int bedrockWirePositions = 0;
                var siftLevel = java.util.stream.StreamSupport.stream(server.getAllLevels().spliterator(), false)
                        .filter(level -> level.dimension().identifier().toString().equals("the_sift:the_sift")).findFirst().orElseThrow();
                int biomeSize = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).size();
                for (int[] coordinates : new int[][]{{-2,2},{-1,2},{-2,1},{-1,1}}) {
                    var chunk = siftLevel.getChunk(coordinates[0], coordinates[1]);
                    var data = new ClientboundLevelChunkPacketData(chunk).getReadBuffer();
                    try {
                        for (var nativeSection : chunk.getSections()) {
                            var decoded = org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes.readChunkSection(data,
                                    org.geysermc.geyser.registry.BlockRegistries.BLOCK_STATES.get().size(), biomeSize);
                            if (decoded.isBlockCountEmpty() != nativeSection.hasOnlyAir()) throw new AssertionError("Chunk section empty count mismatch");
                            for (int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                                int id = net.minecraft.world.level.block.Block.getId(nativeSection.getBlockState(x,y,z));
                                if (decoded.getBlockData().get(x,y,z) != id) throw new AssertionError("Native chunk state decode mismatch at " + x + "," + y + "," + z);
                            }
                            var translated = org.geysermc.hydraulic.block.BlockPaletteRemapper.remap(decoded.getBlockData(), org.geysermc.hydraulic.block.JavaBlockStateRemapper::translate);
                            for (int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                                int expected = org.geysermc.hydraulic.block.JavaBlockStateRemapper.translate(net.minecraft.world.level.block.Block.getId(nativeSection.getBlockState(x,y,z)));
                                if (translated.get(x,y,z) != expected) throw new AssertionError("Chunk palette remap mismatch");
                                wirePositions++;
                            }
                            for (var bedrockMappings : org.geysermc.geyser.registry.BlockRegistries.BLOCKS.get().values()) {
                                var storage = new org.geysermc.geyser.level.chunk.BlockStorage(bedrockMappings.getBedrockAir().getRuntimeId());
                                for (int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                                    storage.setFullBlock((x << 8) | (z << 4) | y, bedrockMappings.getBedrockBlockId(translated.get(x,y,z)));
                                }
                                var encoded = Unpooled.buffer();
                                try {
                                    storage.writeToNetwork(encoded);
                                    int header = encoded.readUnsignedByte();
                                    if ((header & 1) != 1) throw new AssertionError("Expected runtime palette");
                                    var version = org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion.forBitsCeil(header >> 1);
                                    var decodedBits = version.createArray(4096);
                                    int[] words = decodedBits.getWords();
                                    for (int w=0;w<words.length;w++) words[w] = encoded.readIntLE();
                                    int paletteSize = (header >> 1) == 0 ? 1 : readBedrockInt(encoded);
                                    int[] paletteIds = new int[paletteSize];
                                    for(int i=0;i<paletteSize;i++) paletteIds[i]=readBedrockInt(encoded);
                                    for (int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                                        int expected = bedrockMappings.getBedrockBlockId(translated.get(x,y,z));
                                        int actual = paletteIds[decodedBits.get((x << 8) | (z << 4) | y)];
                                        if (actual != expected) throw new AssertionError("Bedrock palette wire mismatch");
                                        bedrockWirePositions++;
                                    }
                                    if (encoded.isReadable()) throw new AssertionError("Unused Bedrock palette bytes");
                                } finally { encoded.release(); }
                            }
                        }
                        if (data.readableBytes() != 0) throw new AssertionError("Unconsumed chunk section bytes " + data.readableBytes());
                    } finally { data.release(); }
                }
                System.out.println("HYDRAULIC CHUNK WIRE AUDIT PASS: " + wirePositions + " real Sift block positions decoded and remapped exactly.");
                System.out.println("HYDRAULIC BEDROCK PALETTE WIRE AUDIT PASS: " + bedrockWirePositions + " block positions encoded and decoded exactly.");
                int solidMappings = 0;
                for (var state : net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY) {
                    if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("the_sift")) continue;
                    if (state.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE) continue;
                    for (var palette : org.geysermc.geyser.registry.BlockRegistries.BLOCKS.get().values()) {
                        int id = org.geysermc.hydraulic.block.JavaBlockStateRemapper.translate(net.minecraft.world.level.block.Block.getId(state));
                        if (palette.getBedrockBlockId(id) == palette.getBedrockAir().getRuntimeId()) throw new AssertionError("Visible Sift state mapped to air: " + state);
                        solidMappings++;
                    }
                }
                System.out.println("HYDRAULIC VISIBLE STATE AUDIT PASS: " + solidMappings + " native Sift state/palette lookups are visible.");
                for (var entry : EntityPackModule.PROFILES.entrySet()) {
                    if (!java.util.Set.of("the_sift:singer", "the_sift:rift", "the_sift:mini_rift").contains(entry.getValue().identifier())) continue;
                    var actor = entry.getKey().create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if (org.geysermc.hydraulic.entity.SiftAnimationBridge.read(actor, entry.getValue().identifier()) == null) throw new AssertionError("Native animation reflection failed");
                    if (entry.getValue().identifier().equals("the_sift:singer")) {
                        actor.getClass().getMethod("beginSequence").invoke(actor);
                        var state = org.geysermc.hydraulic.entity.SiftAnimationBridge.read(actor, entry.getValue().identifier());
                        if (state.phase() != 2 || state.tick() != 0) throw new AssertionError("Singer appear sequence not captured: " + state);
                        var field = actor.getClass().getDeclaredField("DATA_SEQUENCE_TICK"); field.setAccessible(true);
                        var accessor = (net.minecraft.network.syncher.EntityDataAccessor<Integer>) field.get(null);
                        actor.getEntityData().set(accessor, 148);
                        if (org.geysermc.hydraulic.entity.SiftAnimationBridge.read(actor, entry.getValue().identifier()).phase() != 3) throw new AssertionError("Singer sing sequence missing");
                        actor.getEntityData().set(accessor, 309);
                        if (org.geysermc.hydraulic.entity.SiftAnimationBridge.read(actor, entry.getValue().identifier()).phase() != 4) throw new AssertionError("Singer disappear sequence missing");
                    }
                    actor.discard();
                }
                System.out.println("HYDRAULIC SEQUENCE SMOKE PASS: actual Sift Singer, Rift and Mini Rift synchronized state read successfully.");
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
                        if (block.identifier().startsWith("the_sift:") && id.equals("minecraft:geometry.full_block")) throw new AssertionError("Sift cube still relies on built-in geometry: " + block.identifier());
                        if (block.identifier().equals("the_sift:sift_portal")) {
                            if (!id.equals(org.geysermc.hydraulic.compat.PortalPresentation.GEOMETRY)) throw new AssertionError("Portal is not an explicit cube");
                            if (components.lightEmission() != 15) throw new AssertionError("Portal not luminous");
                            if (!components.materialInstances().get("*").renderMethod().equals("opaque")) throw new AssertionError("Portal is translucent");
                            System.out.println("HYDRAULIC PORTAL SMOKE PASS: explicit opaque full block.");
                        }
                        if (!id.startsWith("geometry.the_sift.") && !id.equals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER)) continue;
                        var nbt = (org.cloudburstmc.nbt.NbtMap) convert.invoke(null, components);
                        boolean cullingEnabled = org.geysermc.hydraulic.HydraulicImpl.instance().getConfig().siftFaceCulling();
                        if (!cullingEnabled) {
                            if (nbt.getCompound("minecraft:geometry").containsKey("culling")) throw new AssertionError("Diagnostic still includes culling");
                            continue;
                        }
                        if (!nbt.getCompound("minecraft:geometry").getString("culling").equals((id.equals(org.geysermc.hydraulic.block.StructureGeometry.IDENTIFIER) ? org.geysermc.hydraulic.block.StructureGeometry.CULLING : org.geysermc.hydraulic.block.GeometryCulling.identifier(id)))) throw new AssertionError("Culling missing for " + block.identifier());
                        culled++;
                    }
                }
                if (org.geysermc.hydraulic.HydraulicImpl.instance().getConfig().siftFaceCulling() && culled < 5) throw new AssertionError("Culling not applied: " + culled);
                System.out.println("HYDRAULIC CULLING SMOKE PASS: " + culled + " custom shape components have culling rules; Sift cubes remain explicit.");
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
    private static boolean matchesCondition(String expression, Map<String, String> values) {
        if (expression.equals("1.0")) return true;
        var atom = java.util.regex.Pattern.compile("query\\.block_property\\('([^']+)'\\) == '?([^']+?)'?$");
        for (String alternative : expression.split(" \\|\\| ")) {
            String term = alternative.substring(1, alternative.length() - 1);
            boolean matches = true;
            for (String comparison : term.split(" && ")) {
                var matcher = atom.matcher(comparison);
                if (!matcher.matches()) throw new AssertionError("Unexpected condition " + comparison);
                if (!Objects.equals(values.get(matcher.group(1)), matcher.group(2))) matches = false;
            }
            if (matches) return true;
        }
        return false;
    }

}
