package org.lts.demotivator.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutRegressionTest {
    @Test
    void wrapTerminatesWhenSingleGlyphIsWiderThanLine() {
        FontMetrics metrics = metrics(new Font(Font.SERIF, Font.PLAIN, 40));
        List<String> lines = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> DemotivatorComposer.wrap("WWWW", metrics, 1, true));
        assertEquals(List.of("W", "W", "W", "W"), lines);
    }

    @Test
    void wrapNeverSplitsSurrogatePairs() {
        FontMetrics metrics = metrics(new Font(Font.SERIF, Font.PLAIN, 40));
        String word = "😀😀😀";
        List<String> lines = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> DemotivatorComposer.wrap(word, metrics, 1, true));
        for (String line : lines) {
            assertFalse(!line.isEmpty() && Character.isLowSurrogate(line.charAt(0)), line);
            assertFalse(!line.isEmpty() && Character.isHighSurrogate(line.charAt(line.length() - 1)), line);
        }
    }

    @Test
    void longTitleShrinksToLineLimit() {
        DemotivatorComposer.TextBlock block = DemotivatorComposer.layout(
                "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen "
                        .repeat(3).strip(),
                new Font(Font.SERIF, Font.PLAIN, 12), 80, 1000, 3);
        int minimum = (int) Math.round(80 * 0.45);
        assertTrue(block.font().getSize() < 80);
        assertTrue(block.lines().size() <= 3 || block.font().getSize() == minimum,
                block.font().getSize() + " " + block.lines());
    }

    @Test
    void explicitLineBreaksAreKept() {
        DemotivatorComposer.TextBlock block = DemotivatorComposer.layout("a\nb\nc\nd\ne",
                new Font(Font.SERIF, Font.PLAIN, 12), 40, 1000, 5);
        assertEquals(5, block.lines().size());
        assertEquals(40, block.font().getSize());
    }

    @Test
    void fitKeepsAspectRatioAndLimit() {
        BufferedImage image = new BufferedImage(4000, 1000, BufferedImage.TYPE_INT_RGB);
        BufferedImage fitted = DemotivatorComposer.fit(image, 2048);
        assertEquals(2048, fitted.getWidth());
        assertEquals(512, fitted.getHeight());
        BufferedImage small = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        assertTrue(DemotivatorComposer.fit(small, 2048) == small);
    }

    @Test
    void reservedWindowsNamesFallBackToDefault() {
        assertEquals(OutputFiles.DEFAULT_FOLDER, OutputFiles.sanitizeFolder("con"));
        assertEquals(OutputFiles.DEFAULT_FOLDER, OutputFiles.sanitizeFolder("NUL.txt"));
        assertEquals(OutputFiles.DEFAULT_FOLDER, OutputFiles.sanitizeFolder("com1"));
        assertEquals("console", OutputFiles.sanitizeFolder("console"));
    }

    @Test
    void truncationDoesNotLeaveHalfSurrogate() {
        String input = "a".repeat(DemotivatorText.MAX_LENGTH - 1) + "😀tail";
        String title = DemotivatorText.parse(input, "").title();
        assertFalse(Character.isHighSurrogate(title.charAt(title.length() - 1)));
    }

    private static FontMetrics metrics(Font font) {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            return g.getFontMetrics(font);
        } finally {
            g.dispose();
        }
    }
}
