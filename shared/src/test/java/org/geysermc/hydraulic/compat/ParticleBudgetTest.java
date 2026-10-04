package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ParticleBudgetTest {
    @Test void burstAndMultipleEmittersShareLimit() {
        var budget = new ParticleBudget();
        assertEquals(8, budget.reserve(256, 1_000_000_000L));
        assertEquals(0, budget.reserve(256, 1_100_000_000L));
        assertEquals(8, budget.reserve(256, 1_250_000_000L));
    }
    @Test void smallerBatchesPreserveRemainingBudget() {
        var budget = new ParticleBudget();
        assertEquals(3, budget.reserve(3, 1_000_000_000L));
        assertEquals(5, budget.reserve(10, 1_000_000_001L));
        assertEquals(0, budget.reserve(-1, 1_000_000_002L));
    }
}
