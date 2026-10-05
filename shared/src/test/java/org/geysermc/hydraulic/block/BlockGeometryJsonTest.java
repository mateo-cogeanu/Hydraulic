package org.geysermc.hydraulic.block;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockGeometryJsonTest {
    @Test void rejectsTheReportedEmptyMeshRepresentationWithoutDroppingCubesOrMutatingSource() {
        var source = JsonParser.parseString("{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"bone_0\",\"texture_meshes\":[],\"cubes\":[{\"origin\":[-8,0,-8],\"size\":[16,16,16]}]}]}]}").getAsJsonObject();
        var result = BlockGeometryJson.forExport(source);
        var bone = result.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").get(0).getAsJsonObject();
        assertFalse(bone.has("texture_meshes"));
        assertEquals(1, bone.getAsJsonArray("cubes").size());
        assertTrue(source.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").get(0).getAsJsonObject().has("texture_meshes"));
    }
    @Test void preservesRealTextureMeshesAndUnrelatedEmptyFields() {
        var source = JsonParser.parseString("{\"minecraft:geometry\":[{\"bones\":[{\"texture_meshes\":[{\"texture\":\"texture.default\"}],\"locators\":{}}]}]}").getAsJsonObject();
        assertEquals(source, BlockGeometryJson.forExport(source));
    }
}
