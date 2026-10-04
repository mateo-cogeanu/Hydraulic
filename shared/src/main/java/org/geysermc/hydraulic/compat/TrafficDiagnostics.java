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
        String report = record(direction, packet.getClass().getSimpleName(), bytes, System.nanoTime());
        if (report != null) LOGGER.info("Hydraulic traffic: client={} protocol={} dimension={} {}", session.getClientData() == null ? "unknown" : session.getClientData().getGameVersion(),
                session.getUpstream().getProtocolVersion(), session.getDimensionType(), report);
    }
    public void error(org.geysermc.geyser.session.GeyserSession session, org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent event) {
        if (error()) LOGGER.error("Hydraulic decode diagnostic: packet=" + (event.getPacketClass() == null ? "unknown" : event.getPacketClass().getName()), event.getCause());
    }
}
