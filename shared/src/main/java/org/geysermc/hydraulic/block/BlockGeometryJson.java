package org.geysermc.hydraulic.block;

import com.google.gson.JsonObject;

/** Omit empty optional texture meshes that Bedrock rejects while loading cube geometry. */
public final class BlockGeometryJson {
    private BlockGeometryJson() {}

    public static JsonObject forExport(JsonObject source) {
        JsonObject result = source.deepCopy();
        var geometries = result.getAsJsonArray("minecraft:geometry");
        if (geometries == null) return result;
        for (var value : geometries) {
            var bones = value.getAsJsonObject().getAsJsonArray("bones");
            if (bones == null) continue;
            for (var boneValue : bones) {
                var bone = boneValue.getAsJsonObject();
                var meshes = bone.get("texture_meshes");
                if (meshes != null && meshes.isJsonArray() && meshes.getAsJsonArray().isEmpty()) {
                    bone.remove("texture_meshes");
                }
            }
        }
        return result;
    }
}
