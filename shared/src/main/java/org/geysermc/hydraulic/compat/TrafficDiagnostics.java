package org.geysermc.hydraulic.compat;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bounded per-session counters, never retain packets or payload buffers. */
public final class TrafficDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("Hydraulic/Traffic");
    private final Map<String, Long> counts = new HashMap<>();
    private long windowStart;
    private final java.util.ArrayDeque<String> recent = new java.util.ArrayDeque<>();
    private boolean disconnectReported;
    private String loginSettings="not captured";
    public synchronized void loginSettings(String settings) { loginSettings=settings; }
    public synchronized void detail(String summary) {
        if(recent.size()==24) recent.removeFirst();
        recent.addLast(summary);
    }
    public synchronized String disconnectSummary() {
        return "loginSettings="+loginSettings+" decodeErrors="+errors+" nativeChunkBytes="+chunkBytes+" recentTranslatedPackets="+recent;
    }
    public synchronized void disconnect(org.geysermc.geyser.session.GeyserSession session) {
        if(disconnectReported) return;
        disconnectReported=true;
        LOGGER.info("Hydraulic disconnect diagnostic: protocol={} world={} {}", session.getUpstream().getProtocolVersion(), session.getWorldName(), disconnectSummary());
    }

    private long chunkBytes;
    private int errors;
    private int reportedErrors;
    public synchronized String record(String direction, String type, long bytes, long now) {
        if (windowStart == 0) windowStart = now;
        counts.merge(direction + ":" + type, 1L, Long::sum);
        chunkBytes += bytes;
        if (now - windowStart < 5_000_000_000L) return null;
        String top = counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8).map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining(", "));
        String report = "intervalMs=" + (now - windowStart) / 1_000_000 + " nativeChunkBytes=" + chunkBytes + " decodeErrors=" + errors + " packets={" + top + "}";
        counts.clear();
        chunkBytes = 0;
        errors = 0;
        windowStart = now;
        return report;
    }
    public synchronized boolean error() {
        errors++;
        return reportedErrors++ < 3;
    }
    public void record(org.geysermc.geyser.session.GeyserSession session, String direction, Object packet) {
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket bundle) {
            for (var child : bundle.subPackets()) record(session, direction, child);
            return;
        }
        long bytes = 0;
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket chunk) {
            var buffer = chunk.chunkData().getReadBuffer();
            try { bytes = buffer.readableBytes(); } finally { buffer.release(); }
        }
        if(direction.equals("bedrock")) {
            String type=packet.getClass().getSimpleName();
            String extra="";
            if(packet instanceof org.cloudburstmc.protocol.bedrock.packet.StartGamePacket start) extra=" experiments="+start.getExperiments()+" blockDefinitions="+start.getBlockProperties().size();
            else if(packet instanceof org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket biomes) extra=" biomeDefinitions="+biomes.getBiomes().getDefinitions().size();
            else if(packet instanceof org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket chunk) extra=" chunk="+chunk.getChunkX()+","+chunk.getChunkZ()+" sections="+chunk.getSubChunksLength()+" bytes="+chunk.getData().readableBytes();
            else if(packet instanceof org.cloudburstmc.protocol.bedrock.packet.BossEventPacket boss) extra=" titleLength="+(boss.getTitle()==null?0:boss.getTitle().length());
            detail(type+extra);
        }
        String report = record(direction, packet.getClass().getSimpleName(), bytes, System.nanoTime());
        if (report != null) {
            var client = session.getClientData();
            int requested = session.getClientRenderDistance() != -1 ? session.getClientRenderDistance() : session.getServerRenderDistance();
            int effective = requested == -1 ? 2 : requested;
            effective = MobileViewDistance.forWorld(effective, String.valueOf(session.getWorldName()));
            LOGGER.info("Hydraulic traffic: client={} protocol={} dimension={} requestedView={} javaViewRequest={} entityAppearance={} {}",
                    client == null ? "unknown" : client.getGameVersion(), session.getUpstream().getProtocolVersion(),
                    session.getDimensionType(), requested, effective,
                    org.geysermc.hydraulic.HydraulicImpl.instance().getConfig().customEntityAppearances() ? "custom" : "vanilla-proxy", report);
        }
    }
    public void error(org.geysermc.geyser.session.GeyserSession session, org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent event) {
        if (error()) LOGGER.error("Hydraulic decode diagnostic: packet=" + (event.getPacketClass() == null ? "unknown" : event.getPacketClass().getName()), event.getCause());
    }
}
