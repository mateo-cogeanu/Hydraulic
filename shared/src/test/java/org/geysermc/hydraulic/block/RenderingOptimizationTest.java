package org.geysermc.hydraulic.block;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RenderingOptimizationTest {
    @Test void defaultUvsMatchAsymmetricJavaElementFacesAndPreserveExplicitUvs() {
        JsonObject source = JsonParser.parseString("{\"elements\":[{\"from\":[1,2,3],\"to\":[11,12,13],\"faces\":{\"up\":{\"texture\":\"#all\"},\"down\":{\"texture\":\"#all\"},\"north\":{\"texture\":\"#all\"},\"south\":{\"texture\":\"#all\"},\"west\":{\"texture\":\"#all\"},\"east\":{\"texture\":\"#all\",\"uv\":[4,5,6,7]}}}]}").getAsJsonObject();
        var faces = ImplicitModelUvs.normalize(source).getAsJsonObject().getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals("[1.0,3.0,11.0,13.0]", faces.getAsJsonObject("up").get("uv").toString());
        assertEquals("[5.0,4.0,15.0,14.0]", faces.getAsJsonObject("north").get("uv").toString());
        assertEquals("[3.0,4.0,13.0,14.0]", faces.getAsJsonObject("west").get("uv").toString());
        assertEquals("[4,5,6,7]", faces.getAsJsonObject("east").get("uv").toString());
        assertFalse(source.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces").getAsJsonObject("up").has("uv"));
    }
    @Test void onlyBoundaryFacesAreCulledAndParentRotationIsRespected() {
        var cube = StructureGeometry.create().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertEquals(6, GeometryCulling.definition(cube).getAsJsonObject("minecraft:block_culling_rules").getAsJsonArray("rules").size());
        cube.getAsJsonArray("bones").get(0).getAsJsonObject().add("rotation", JsonParser.parseString("[0,90,0]"));
        assertEquals(0, GeometryCulling.definition(cube).getAsJsonObject("minecraft:block_culling_rules").getAsJsonArray("rules").size());
        cube.getAsJsonArray("bones").get(0).getAsJsonObject().remove("rotation");
        cube.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject().add("origin", JsonParser.parseString("[-7,1,-7]"));
        cube.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject().add("size", JsonParser.parseString("[14,14,14]"));
        assertEquals(0, GeometryCulling.definition(cube).getAsJsonObject("minecraft:block_culling_rules").getAsJsonArray("rules").size());
    }
    @Test void facesExtendingBeyondTheNeighborAreNotCulled() {
        var geometry = StructureGeometry.create().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var cube = geometry.getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("cubes").get(0).getAsJsonObject();
        cube.add("origin", JsonParser.parseString("[-8,0,-9]"));
        cube.add("size", JsonParser.parseString("[16,16,18]"));
        assertEquals(0, GeometryCulling.definition(geometry).getAsJsonObject("minecraft:block_culling_rules").getAsJsonArray("rules").size());
    }
    @Test void stateReductionExactlyPreservesTheTruthTable() {
        Map<String, Set<String>> domains = Map.of("a", Set.of("0","1"), "b", Set.of("0","1"), "c", Set.of("0","1"));
        // Exhaust every subset of the 3-bit domain; reduction must neither hide nor add a state.
        var domain = new ArrayList<Map<String,String>>();
        for(int i=0;i<8;i++) domain.add(Map.of("a", ""+(i&1), "b", ""+((i>>1)&1), "c", ""+((i>>2)&1)));
        for(int mask=1;mask<256;mask++) {
            var selected = new ArrayList<Map<String,String>>(); for(int i=0;i<8;i++) if((mask&(1<<i))!=0) selected.add(domain.get(i));
            var reduced = PermutationConditions.reduce(selected, domains);
            for(var state:domain) assertEquals(selected.contains(state), reduced.stream().anyMatch(term->state.entrySet().containsAll(term.entrySet())));
        }
        assertEquals("1.0", PermutationConditions.condition(domain, domains));
    }
}
