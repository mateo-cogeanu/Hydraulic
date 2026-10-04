package org.geysermc.hydraulic.text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ModTranslationsTest {
    @TempDir Path root;
    @Test void loadsModAdvancementsAndFallsBackToEnglish() throws Exception {
        Path lang = Files.createDirectories(root.resolve("assets/example/lang"));
        Files.writeString(lang.resolve("en_us.json"), "{\"example.achievement.arrival\":\"A New World\",\"example.other\":\"Fallback\"}");
        Files.writeString(lang.resolve("pl_pl.json"), "{\"example.achievement.arrival\":\"Nowy świat\"}");
        ModTranslations.load(new ModInfo("example", "example", "Example", "1", null, List.of(root)), LoggerFactory.getLogger(getClass()));
        assertEquals("A New World", ModTranslations.translate("example.achievement.arrival", "EN_US"));
        assertEquals("Nowy świat", ModTranslations.translate("example.achievement.arrival", "pl_pl"));
        assertEquals("Fallback", ModTranslations.translate("example.other", "pl_pl"));
        assertEquals("A New World", ModTranslations.translate("example.achievement.arrival", "fr_fr"));
        assertNull(ModTranslations.translate("minecraft.unhandled", "en_us"));
    }
}
