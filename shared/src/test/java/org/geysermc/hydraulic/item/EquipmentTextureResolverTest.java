package org.geysermc.hydraulic.item;

import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentTextureResolverTest {
    @Test
    void modernSiftiteLayersDoNotDiscardAdultArmor() {
        var definition = JsonParser.parseString("""
            {"layers": {
                "humanoid": [{"texture": "the_sift:siftite"}],
                "humanoid_baby": [{"texture": "the_sift:siftite"}],
                "humanoid_leggings": [{"texture": "the_sift:siftite"}]
            }}
            """);
        assertEquals(Key.key("the_sift:siftite"), EquipmentTextureResolver.texture(definition, "humanoid"));
        assertEquals(Key.key("the_sift:siftite"), EquipmentTextureResolver.texture(definition, "humanoid_leggings"));
        assertNull(EquipmentTextureResolver.texture(definition, "wolf_body"));
    }
}
