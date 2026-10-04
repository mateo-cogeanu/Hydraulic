package org.geysermc.hydraulic.item;

import com.google.gson.JsonElement;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/** Reads a known equipment layer without rejecting unrelated layers from newer Java versions. */
public final class EquipmentTextureResolver {
    private EquipmentTextureResolver() {
    }

    @Nullable
    public static Key texture(JsonElement definition, String layer) {
        var layers = definition.getAsJsonObject().getAsJsonObject("layers");
        if (layers == null || !layers.has(layer)) return null;
        var entries = layers.getAsJsonArray(layer);
        if (entries.isEmpty()) return null;
        var texture = entries.get(0).getAsJsonObject().get("texture");
        return texture == null ? null : Key.key(texture.getAsString());
    }
}
