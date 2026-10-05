package org.geysermc.hydraulic.compat;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.entity.EntityPackModule;
import org.geysermc.hydraulic.entity.SiftAnimationBridge;

/** Nearby client-only effects, sampled on the server without loading chunks or changing Sift. */
public final class SiftClientEffects {
    public static final int MAX_BEAMS = 16;
    public static final int BEAM_RADIUS = 48;
    private record Beam(int color, int growth) {}
    private static final Map<UUID, Map<BlockPos,Long>> ACTORS = new ConcurrentHashMap<>();
    private static final Map<UUID,String> WORLDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<BlockPos,Beam>> PREVIOUS = new ConcurrentHashMap<>();
    private static final ClassValue<Method[]> BEAM_METHODS = new ClassValue<>() {
        protected Method[] computeValue(Class<?> type) {
            try { return new Method[]{type.getMethod("hasBeam"), type.getMethod("getBeamColor"), type.getMethod("getBeamGrowth",float.class)}; }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read Sonorous beam",e); }
        }
    };
    public static void disconnect(UUID owner) { ACTORS.remove(owner); PREVIOUS.remove(owner); WORLDS.remove(owner); }
    public static void clear() { ACTORS.clear(); PREVIOUS.clear(); WORLDS.clear(); }
    public static void tick(MinecraftServer server) {
        if ((server.getTickCount() & 3) != 0 || GeyserApi.api() == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(GeyserApi.api().connectionByUuid(player.getUUID()) instanceof GeyserSession session) || session.isClosed()) continue;
            boolean inSift = MobileViewDistance.inSift(player.level().dimension().identifier().toString());
            if (inSift) ambient(player, session);
            // Sonorous consoles and the portal summoning also work in the Overworld.
            beams(player, session);
            int singerEmitters = 0;
            for (var entry : EntityPackModule.TRACKED.getOrDefault(player.getUUID(), Map.of()).entrySet()) {
                var entity = player.level().getEntity(entry.getKey());
                if (entity == null) continue;
                SiftAnimationBridge.capture(player.getUUID(),entity,entry.getValue().identifier(),session);
                if (entry.getValue().identifier().equals("the_sift:singer") && entity.distanceToSqr(player) <= 24*24) {
                    var state = SiftAnimationBridge.read(entity, entry.getValue().identifier());
                    if (state != null && (state.phase() == 2 || state.phase() == 4) && singerEmitters++ < 2) {
                        NativePacketBridge.particle(session,"the_sift:sift_parallax",Vector3f.from(entity.getX(),entity.getY()+0.1,entity.getZ()));
                    }
                }
            }
        }
    }
    private static void ambient(ServerPlayer player, GeyserSession session) {
        var level = player.level(); var random = level.getRandom();
        // At most four extra emitter packets every four ticks (20/s); lifetimes are finite.
        int remaining = 4;
        for (int i=0; i<512 && remaining>0; i++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(33)-16,random.nextInt(25)-8,random.nextInt(33)-16);
            boolean loaded=true;
            for(int cx=-1;cx<=1 && loaded;cx++) for(int cz=-1;cz<=1;cz++)
                if(level.getChunkSource().getChunkNow((pos.getX()>>4)+cx,(pos.getZ()>>4)+cz)==null) { loaded=false; break; }
            if(!loaded) continue; // Attribute interpolation may read the adjacent biome cells.
            var particles = level.environmentAttributes().getValue(EnvironmentAttributes.AMBIENT_PARTICLES,pos);
            for (var ambient : particles) {
                String id = BuiltInRegistries.PARTICLE_TYPE.getKey(ambient.particle().getType()).toString();
                // Four ticks of client animation sampling per pass. Never substitute a biome's effects.
                if (NativePacketBridge.PARTICLES.contains(id) && random.nextFloat() < Math.min(1,ambient.probability()*4)) {
                    if (id.equals("the_sift:sift_note")) pos = new BlockPos(pos.getX(),level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())+10+random.nextInt(19),pos.getZ());
                    if (!level.getBlockState(pos).isAir()) continue;
                    double[][] palette={{0.45,0.93,0.98},{0.52,0.76,1},{0.78,0.58,1},{1,0.58,0.86},{1,0.74,0.46},{0.97,0.94,0.47},{0.52,0.96,0.66}};
                    var color=palette[random.nextInt(palette.length)];
                    NativePacketBridge.particle(session,id,Vector3f.from(pos.getX()+random.nextFloat(),pos.getY()+random.nextFloat(),pos.getZ()+random.nextFloat()),
                            id.equals("the_sift:sift_note")?Map.of("r",color[0],"g",color[1],"b",color[2]):Map.of());
                    if (--remaining==0) break;
                }
            }
            if (remaining==0) break;
            var block = level.getBlockState(pos);
            String id = BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            if (id.equals("the_sift:sift_portal") && random.nextInt(4)==0) {
                NativePacketBridge.particle(session,"the_sift:sift_parallax",Vector3f.from(pos.getX()+random.nextFloat(),pos.getY()+random.nextFloat(),pos.getZ()+random.nextFloat())); remaining--;
            } else if (id.equals("the_sift:ichor") && block.getFluidState().isSource() && level.getBlockState(pos.above()).isAir() && random.nextInt(8)==0) {
                NativePacketBridge.particle(session,"the_sift:ichor_surface_mist",Vector3f.from(pos.getX()+0.1+random.nextFloat()*0.8,pos.getY()+block.getFluidState().getHeight(level,pos)+0.00625,pos.getZ()+0.1+random.nextFloat()*0.8)); remaining--;
            }
        }
    }
    private static void beams(ServerPlayer player, GeyserSession session) {
        Map<BlockPos,Beam> visible = new HashMap<>();
        int x = player.blockPosition().getX()>>4, z = player.blockPosition().getZ()>>4;
        for (int dx=-3;dx<=3;dx++) for (int dz=-3;dz<=3;dz++) {
            var chunk = player.level().getChunkSource().getChunkNow(x+dx,z+dz); if(chunk==null) continue;
            for (var entity : chunk.getBlockEntities().values()) {
                BlockPos pos = entity.getBlockPos();
                if (!entity.getClass().getName().equals("mielon.thesift.block.entity.SonorousDeepslateBlockEntity") || pos.distToCenterSqr(player.position())>BEAM_RADIUS*BEAM_RADIUS) continue;
                try {
                    Method[] methods = BEAM_METHODS.get(entity.getClass());
                    if (!(boolean)methods[0].invoke(entity)) continue;
                    int color = (int)methods[1].invoke(entity); float growth=(float)methods[2].invoke(entity,0f);
                    visible.put(pos,new Beam(color,Math.round(growth*1000)));
                } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot synchronize Sonorous beam",e); }
            }
        }
        Map<BlockPos,Beam> selected = new LinkedHashMap<>();
        visible.entrySet().stream().sorted(Comparator.comparingDouble(e->e.getKey().distToCenterSqr(player.position())))
                .limit(MAX_BEAMS).forEach(e->selected.put(e.getKey(),e.getValue()));
        UUID owner=player.getUUID(); String dimension=player.level().dimension().identifier().toString();
        session.executeInEventLoop(()-> {
            if(session.isClosed() || (session.getWorldName() == null || !dimension.equals(session.getWorldName().asString()))) return;
            String previousWorld=WORLDS.put(owner,dimension);
            if(previousWorld!=null && !previousWorld.equals(dimension)) { ACTORS.remove(owner); PREVIOUS.remove(owner); }
            var actors=ACTORS.computeIfAbsent(owner,k->new HashMap<>()); var previous=PREVIOUS.computeIfAbsent(owner,k->new HashMap<>());
            for (var iterator=actors.entrySet().iterator();iterator.hasNext();) {
                var entry=iterator.next(); if(selected.containsKey(entry.getKey())) continue;
                RemoveEntityPacket remove=new RemoveEntityPacket(); remove.setUniqueEntityId(entry.getValue()); session.sendUpstreamPacket(remove);
                previous.remove(entry.getKey()); iterator.remove();
            }
            for(var entry:selected.entrySet()) {
                BlockPos pos=entry.getKey(); Beam state=entry.getValue(); Long runtime=actors.get(pos);
                if(runtime==null) {
                    runtime=session.getEntityCache().nextEntityId(); actors.put(pos,runtime);
                    AddEntityPacket add=new AddEntityPacket(); add.setIdentifier(SonorousBeamPresentation.IDENTIFIER); add.setUniqueEntityId(runtime); add.setRuntimeEntityId(runtime);
                    add.setPosition(Vector3f.from(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5)); add.setMotion(Vector3f.ZERO); add.setRotation(Vector2f.ZERO);
                    add.getMetadata().put(EntityDataTypes.WIDTH,0f); add.getMetadata().put(EntityDataTypes.HEIGHT,0f); add.getMetadata().put(EntityDataTypes.SCALE,1f);
                    add.getMetadata().put(EntityDataTypes.VARIANT,state.color()); add.getMetadata().put(EntityDataTypes.MARK_VARIANT,state.growth());
                    session.sendUpstreamPacket(add);
                } else if(!state.equals(previous.get(pos))) {
                    SetEntityDataPacket update=new SetEntityDataPacket();update.setRuntimeEntityId(runtime);
                    update.getMetadata().put(EntityDataTypes.VARIANT,state.color()); update.getMetadata().put(EntityDataTypes.MARK_VARIANT,state.growth());session.sendUpstreamPacket(update);
                }
                previous.put(pos,state);
            }
        });
    }
}
