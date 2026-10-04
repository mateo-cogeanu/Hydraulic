package org.geysermc.hydraulic.compat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EntityUpdateCacheTest {
 @Test void exactDuplicateSuppressionNeverDropsANewTickOrChangedVelocity() {
  var cache = new EntityUpdateCache();
  assertFalse(cache.duplicate(1, 7, "velocityA"));
  assertTrue(cache.duplicate(1, 7, "velocityA"));
  assertFalse(cache.duplicate(1, 7, "velocityB"));
  assertFalse(cache.duplicate(2, 7, "velocityB"));
  assertFalse(cache.duplicate(2, 8, "velocityB"));
  cache.invalidate(7);assertFalse(cache.duplicate(2, 7, "velocityB"));
 }
}
