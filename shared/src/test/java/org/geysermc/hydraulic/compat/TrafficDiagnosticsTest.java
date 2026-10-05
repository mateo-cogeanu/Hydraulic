package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TrafficDiagnosticsTest {
    @Test void shortDisconnectRetainsBoundedRecentPacketSummaries() {
        var counters=new TrafficDiagnostics();
        for(int i=0;i<30;i++)counters.detail("packet"+i);
        counters.record("native","Chunk",100,1_000_000_000L);
        counters.error();
        String summary=counters.disconnectSummary();
        assertFalse(summary.contains("packet5,"));
        assertTrue(summary.contains("packet6,"));
        assertTrue(summary.contains("packet29"));
        assertTrue(summary.contains("decodeErrors=1"));
        assertTrue(summary.contains("nativeChunkBytes=100"));
    }

    @Test void countsClearBetweenFiveSecondReports() {
        var counters = new TrafficDiagnostics();
        assertNull(counters.record("native", "Chunk", 100, 1_000_000_000L));
        assertNull(counters.record("native", "Chunk", 200, 2_000_000_000L));
        assertTrue(counters.error());
        String report = counters.record("bedrock", "Move", 0, 6_000_000_000L);
        assertTrue(report.contains("nativeChunkBytes=300"));
        assertTrue(report.contains("native:Chunk=2"));
        assertTrue(report.contains("decodeErrors=1"));
        String next = counters.record("bedrock", "Move", 0, 11_000_000_000L);
        assertTrue(next.contains("nativeChunkBytes=0"));
        assertTrue(next.contains("decodeErrors=0"));
        assertFalse(next.contains("native:Chunk"));
    }
    @Test void onlyFirstThreeDecodeErrorsRequestStackTraces() {
        var counters = new TrafficDiagnostics();
        assertTrue(counters.error());
        assertTrue(counters.error());
        assertTrue(counters.error());
        assertFalse(counters.error());
        assertFalse(counters.error());
    }
}
