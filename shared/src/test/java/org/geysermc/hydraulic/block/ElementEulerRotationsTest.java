package org.geysermc.hydraulic.block;

import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import team.unnamed.creative.model.Model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElementEulerRotationsTest {
    @Test void inheritedThreeAxisChainsKeepAllAxesInsteadOfBeingFlippedBelowTheBlock() {
        var parentKey=Key.key("hydraulic_test:sign_template");
        ElementEulerRotations.capture(JsonParser.parseString("{\"elements\":[{\"rotation\":{\"x\":180,\"y\":-67.5,\"z\":-180}}]}"),parentKey);
        var parent=Model.builder().key(parentKey).build();
        var child=Model.builder().key(Key.key("hydraulic_test:sign")).parent(parentKey).build();
        var geometry=JsonParser.parseString("{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"bone_0\",\"cubes\":[{\"rotation\":[-180,0,0],\"origin\":[2,11,3],\"size\":[3,4,0]}]}]}]}").getAsJsonObject();
        ElementEulerRotations.apply(geometry,child,key->key.equals(parentKey)?parent:null);
        var cube=geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject();
        assertEquals("[-180.0,67.5,180.0]",cube.getAsJsonArray("rotation").toString());
        assertEquals("[2,11,3]",cube.getAsJsonArray("origin").toString());
    }
}
