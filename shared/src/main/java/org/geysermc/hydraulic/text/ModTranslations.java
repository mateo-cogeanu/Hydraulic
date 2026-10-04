package org.geysermc.hydraulic.text;

import com.google.gson.JsonParser;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.slf4j.Logger;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Separate from Geyser's downloaded vanilla locales, which may load after packs. */
public final class ModTranslations {
    private static final Map<String, Map<String, String>> LOCALES = new ConcurrentHashMap<>();

    public static void load(ModInfo mod, Logger logger) {
        for (var root : mod.roots()) {
            var directory = root.resolve("assets/" + mod.namespace() + "/lang");
            if (!Files.isDirectory(directory)) continue;
            try (var files = Files.list(directory)) {
                for (var file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                    String name = file.getFileName().toString().replace(".json", "").toLowerCase(Locale.ROOT);
                    var translations = LOCALES.computeIfAbsent(name, ignored -> new ConcurrentHashMap<>());
                    try (var reader = Files.newBufferedReader(file)) {
                        JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
                            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                                translations.put(entry.getKey(), entry.getValue().getAsString());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not read translations for {}: {}", mod.id(), e.getMessage());
            }
        }
    }

    public static String translate(String key, String locale) {
        String value = LOCALES.getOrDefault(locale.toLowerCase(Locale.ROOT), Map.of()).get(key);
        return value != null ? value : LOCALES.getOrDefault("en_us", Map.of()).get(key);
    }
}
