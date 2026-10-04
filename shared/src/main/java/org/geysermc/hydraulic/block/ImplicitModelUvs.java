package org.geysermc.hydraulic.block;

import com.google.gson.*;

/** Materialize Java's default face UVs before PackConverter reads the model. */
public final class ImplicitModelUvs {
    public static JsonElement normalize(JsonElement source) {
        if (!source.isJsonObject() || !source.getAsJsonObject().has("elements")) return source;
        JsonObject result = source.deepCopy().getAsJsonObject();
        for (JsonElement value : result.getAsJsonArray("elements")) {
            JsonObject element = value.getAsJsonObject();
            if (!element.has("faces") || !element.has("from") || !element.has("to")) continue;
            var from = element.getAsJsonArray("from"); var to = element.getAsJsonArray("to");
            float x0 = from.get(0).getAsFloat(), y0 = from.get(1).getAsFloat(), z0 = from.get(2).getAsFloat();
            float x1 = to.get(0).getAsFloat(), y1 = to.get(1).getAsFloat(), z1 = to.get(2).getAsFloat();
            for (var face : element.getAsJsonObject("faces").entrySet()) {
                JsonObject definition = face.getValue().getAsJsonObject();
                if (definition.has("uv")) continue;
                float[] uv = switch (face.getKey()) {
                    case "down" -> new float[]{x0, 16-z1, x1, 16-z0};
                    case "up" -> new float[]{x0, z0, x1, z1};
                    case "north" -> new float[]{16-x1, 16-y1, 16-x0, 16-y0};
                    case "south" -> new float[]{x0, 16-y1, x1, 16-y0};
                    case "west" -> new float[]{z0, 16-y1, z1, 16-y0};
                    case "east" -> new float[]{16-z1, 16-y1, 16-z0, 16-y0};
                    default -> null;
                };
                if (uv != null) { JsonArray array = new JsonArray(); for (float f : uv) array.add(f); definition.add("uv", array); }
            }
        }
        return result;
    }
}
