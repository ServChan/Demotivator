package org.lts.demotivator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lts.demotivator.render.ColorCodec;
import org.lts.demotivator.render.OutputFiles;

class ConfigAndCodecTest {
    @Test
    void colorsParseShortLongAndPrefixedForms() {
        assertEquals(0xFFFFFF, ColorCodec.parse("#fff").getAsInt());
        assertEquals(0x12AB34, ColorCodec.parse("12ab34").getAsInt());
        assertEquals(0x00FF00, ColorCodec.parse("0x00FF00").getAsInt());
        assertTrue(ColorCodec.parse("#12345").isEmpty());
        assertTrue(ColorCodec.parse("zzzzzz").isEmpty());
        assertEquals("#0A0B0C", ColorCodec.format(0x0A0B0C));
    }

    @Test
    void folderNamesAreSanitized() {
        assertEquals("demotivators", OutputFiles.sanitizeFolder(null));
        assertEquals("demotivators", OutputFiles.sanitizeFolder(".."));
        assertEquals("my_pics", OutputFiles.sanitizeFolder("my/pics"));
        assertEquals("posters", OutputFiles.sanitizeFolder("  posters. "));
    }

    @Test
    void uniqueFileAvoidsCollisions(@TempDir Path dir) throws IOException {
        LocalDateTime time = LocalDateTime.of(2026, 9, 25, 12, 30, 5);
        Path first = OutputFiles.uniqueFile(dir, time);
        assertEquals("demotivator_2026-09-25_12.30.05.png", first.getFileName().toString());
        Files.createFile(first);
        assertEquals("demotivator_2026-09-25_12.30.05_1.png", OutputFiles.uniqueFile(dir, time).getFileName().toString());
    }

    @Test
    void sanitizeClampsAndRepairsFields() {
        DemotivatorConfig config = new DemotivatorConfig();
        config.captureDelayTicks = 0;
        config.imageWidth = 99999;
        config.marginPercent = Double.NaN;
        config.frameThickness = -5;
        config.titleSizePercent = 100;
        config.titleFont = "   ";
        config.backgroundColor = "not a color";
        config.frameColor = "#abc";
        config.outputFolder = "../../evil";
        DemotivatorConfig clean = DemotivatorConfig.sanitize(config);
        assertEquals(DemotivatorConfig.MIN_DELAY, clean.captureDelayTicks);
        assertEquals(DemotivatorConfig.MAX_WIDTH, clean.imageWidth);
        assertEquals(DemotivatorConfig.MIN_MARGIN, clean.marginPercent);
        assertEquals(0, clean.frameThickness);
        assertEquals(DemotivatorConfig.MAX_TITLE, clean.titleSizePercent);
        assertEquals("Times New Roman", clean.titleFont);
        assertEquals("#000000", clean.backgroundColor);
        assertEquals("#AABBCC", clean.frameColor);
        assertEquals("_.._evil", clean.outputFolder);
    }
}
