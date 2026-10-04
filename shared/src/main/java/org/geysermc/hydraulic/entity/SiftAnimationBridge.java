package org.geysermc.hydraulic.entity;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.hydraulic.compat.MobileViewDistance;

/** Reads Sift's synchronized animation state without altering or depending on its JAR. */
public final class SiftAnimationBridge {
    public record State(int phase, int tick) {}
    public static final Map<UUID, Map<Integer, State>> STATES = new ConcurrentHashMap<>();
    private static final java.util.Set<Class<?>> WARNED = ConcurrentHashMap.newKeySet();
    private static final ClassValue<Map<String, Field>> FIELDS = new ClassValue<>() {
        protected Map<String, Field> computeValue(Class<?> type) {
            Map<String, Field> fields = new ConcurrentHashMap<>();
            for (Class<?> c = type; c != null && c.getName().startsWith("mielon."); c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    field.setAccessible(true);
                    fields.putIfAbsent(field.getName(), field);
                }
            }
            return fields;
        }
    };

    public static State sequence(int tick, int appear, int sing) {
        return tick < appear ? new State(2, Math.max(0, tick)) : tick < appear + sing
                ? new State(3, tick - appear) : new State(4, tick - appear - sing);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T data(Entity entity, String field) throws ReflectiveOperationException {
        var accessor = (EntityDataAccessor) FIELDS.get(entity.getClass()).get(field).get(null);
        return (T) entity.getEntityData().get(accessor);
    }

    public static State read(Entity entity, String identifier) {
        try {
            if (identifier.equals("the_sift:singer")) {
                if (SiftAnimationBridge.<Boolean>data(entity, "DATA_SOUL_EVENT")) {
                    int phase = data(entity, "DATA_SOUL_EVENT_PHASE"), tick = data(entity, "DATA_SOUL_EVENT_TICK");
                    int mapped = switch (phase) {
                        case 0 -> 2;
                        case 2 -> 5;
                        case 4 -> 4;
                        case 5 -> SiftAnimationBridge.<Boolean>data(entity, "DATA_HOLDING_SOUL") ? 6 : 0;
                        default -> entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001 ? 1 : 0;
                    };
                    return new State(mapped, tick);
                }
                if (SiftAnimationBridge.<Boolean>data(entity, "DATA_SEQUENCE_STARTED")) {
                    Object timings = FIELDS.get(entity.getClass()).get("timings").get(entity);
                    int appear = (int) timings.getClass().getMethod("appearTicks").invoke(timings);
                    int sing = (int) timings.getClass().getMethod("singTicks").invoke(timings);
                    return sequence(data(entity, "DATA_SEQUENCE_TICK"), appear, sing);
                }
                return new State(entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001 ? 1 : 0, entity.tickCount);
            }
            if (identifier.equals("the_sift:rift") || identifier.equals("the_sift:mini_rift")) {
                boolean mini = identifier.equals("the_sift:mini_rift");
                if (SiftAnimationBridge.<Boolean>data(entity, "DATA_CLOSING")) {
                    int tick = FIELDS.get(entity.getClass()).get("closingTicks").getInt(entity);
                    return new State(2, mini ? tick * 16 / 12 : tick);
                }
                if (mini) return new State(entity.tickCount < 12 ? 1 : 0, Math.min(16, entity.tickCount * 16 / 12));
                boolean appearing = data(entity, "DATA_APPEARING");
                long born = data(entity, "DATA_BORN");
                int tick = (int) Math.clamp(entity.level().getGameTime() - born, 0, 16);
                return new State(appearing && tick < 16 ? 1 : 0, tick);
            }
        } catch (ReflectiveOperationException | NullPointerException e) {
            if (WARNED.add(entity.getClass())) org.slf4j.LoggerFactory.getLogger(SiftAnimationBridge.class).error("Cannot read Sift animation state for {}", identifier, e);
            return null;
        }
        return null;
    }

    public static void capture(UUID owner, Entity nativeEntity, String identifier, GeyserSession session) {
        State state = read(nativeEntity, identifier);
        if (state == null) return;
        var tracked = STATES.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>());
        State previous = tracked.get(nativeEntity.getId());
        // Preserve transitions immediately; synchronize elapsed time at 5 Hz only inside the Sift.
        if (previous != null && previous.phase() == state.phase() && MobileViewDistance.inSift(nativeEntity.level().dimension().identifier().toString())
                && Math.abs(state.tick() - previous.tick()) < 4) return;
        int nativeId = nativeEntity.getId();
        tracked.put(nativeId, state);
        if (session != null) session.executeInEventLoop(() -> {
            if (session.isClosed()) return;
            var entity = session.getEntityCache().getEntityByJavaId(nativeId);
            if (entity != null && org.geysermc.hydraulic.HydraulicImpl.instance().getConfig().customEntityAppearances()) {
                entity.override(GeyserEntityDataTypes.VARIANT, state.phase());
                send(session, entity.geyserId(), state);
            }
        });
    }

    public static void send(GeyserSession session, long runtimeId, State state) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(runtimeId);
        packet.getMetadata().put(EntityDataTypes.VARIANT, state.phase());
        packet.getMetadata().put(EntityDataTypes.MARK_VARIANT, state.tick());
        session.sendUpstreamPacket(packet);
    }
}
