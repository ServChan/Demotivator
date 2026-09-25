package org.lts.demotivator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.lts.demotivator.render.DemotivatorComposer;
import org.lts.demotivator.render.DemotivatorStyle;
import org.lts.demotivator.render.DemotivatorText;

class DemotivatorComposerTest {
    private static final int SOURCE_COLOR = 0x3366CC;

    @Test
    void posterPlacesImageInsideFrameOnBackground() {
        DemotivatorStyle style = DemotivatorStyle.classic();
        BufferedImage poster = DemotivatorComposer.compose(source(400, 200),
                new DemotivatorText("Title", "subtitle"), style);

        int margin = (int) Math.round(style.imageWidth() * style.marginPercent() / 100.0);
        int border = style.frameThickness() + style.frameGap();
        int imageHeight = 500;
        assertEquals(style.imageWidth() + border * 2 + margin * 2, poster.getWidth());
        assertTrue(poster.getHeight() > margin + imageHeight + border * 2);

        assertEquals(0x000000, rgb(poster, 1, 1));
        assertEquals(0xFFFFFF, rgb(poster, margin, margin));
        assertEquals(0x000000, rgb(poster, margin + style.frameThickness(), margin + style.frameThickness()));
        assertEquals(SOURCE_COLOR, rgb(poster, margin + border + 500, margin + border + imageHeight / 2));
    }

    @Test
    void textIsDrawnBelowTheFrame() {
        DemotivatorStyle style = DemotivatorStyle.classic();
        BufferedImage poster = DemotivatorComposer.compose(source(400, 200),
                new DemotivatorText("WWWWWW", ""), style);
        int margin = (int) Math.round(style.imageWidth() * style.marginPercent() / 100.0);
        int frameBottom = margin + 500 + (style.frameThickness() + style.frameGap()) * 2;
        boolean foundWhite = false;
        for (int y = frameBottom + 1; y < poster.getHeight() && !foundWhite; y++) {
            for (int x = 0; x < poster.getWidth(); x++) {
                if (rgb(poster, x, y) == 0xFFFFFF) {
                    foundWhite = true;
                    break;
                }
            }
        }
        assertTrue(foundWhite);
    }

    @Test
    void emptyTextStillProducesPoster() {
        BufferedImage poster = DemotivatorComposer.compose(source(64, 64), new DemotivatorText("", ""),
                DemotivatorStyle.classic());
        assertTrue(poster.getHeight() > poster.getWidth() - 400);
    }

    @Test
    void unbreakableTitleFitsInsidePoster() {
        DemotivatorStyle style = DemotivatorStyle.classic();
        BufferedImage poster = DemotivatorComposer.compose(source(100, 100),
                new DemotivatorText("A".repeat(200), ""), style);
        assertTrue(poster.getWidth() < 2000);
    }

    @Test
    void unknownFontFallsBackToLogicalFont() {
        assertEquals("Serif", DemotivatorComposer.resolveFont("Definitely Missing Font 42", "Serif").getName());
        assertTrue(!DemotivatorComposer.isFontAvailable("Definitely Missing Font 42"));
        assertTrue(DemotivatorComposer.isFontAvailable("Serif"));
    }

    private static BufferedImage source(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, SOURCE_COLOR);
            }
        }
        return image;
    }

    private static int rgb(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) & 0xFFFFFF;
    }
}
