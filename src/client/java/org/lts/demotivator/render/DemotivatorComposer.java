package org.lts.demotivator.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DemotivatorComposer {
    private static final double MIN_FONT_SHRINK = 0.45;
    private static volatile Set<String> availableFamilies;

    private DemotivatorComposer() {
    }

    public static BufferedImage compose(BufferedImage source, DemotivatorText text, DemotivatorStyle style) {
        int imageWidth = Math.max(16, style.imageWidth());
        int imageHeight = Math.max(1, (int) Math.round(source.getHeight() * (imageWidth / (double) source.getWidth())));
        int margin = (int) Math.round(imageWidth * style.marginPercent() / 100.0);
        int frame = Math.max(0, style.frameThickness());
        int border = frame + Math.max(0, style.frameGap());
        int frameOuterWidth = imageWidth + border * 2;
        int frameOuterHeight = imageHeight + border * 2;
        int canvasWidth = frameOuterWidth + margin * 2;
        int textWidth = Math.max(32, canvasWidth - Math.max(margin, 8));

        String title = style.uppercaseTitle() ? text.title().toUpperCase(Locale.ROOT) : text.title();
        TextBlock titleBlock = layout(title, resolveFont(style.titleFont(), Font.SERIF),
                imageWidth * style.titleSizePercent() / 100.0, textWidth);
        TextBlock subtitleBlock = layout(text.subtitle(), resolveFont(style.subtitleFont(), Font.SANS_SERIF),
                imageWidth * style.subtitleSizePercent() / 100.0, textWidth);

        int leadSize = titleBlock.isEmpty() ? subtitleBlock.font().getSize() : titleBlock.font().getSize();
        int afterFrame = Math.max(margin / 3, (int) Math.round(leadSize * 0.4));
        int betweenBlocks = titleBlock.isEmpty() || subtitleBlock.isEmpty()
                ? 0 : Math.max(4, (int) Math.round(subtitleBlock.font().getSize() * 0.4));
        int bottom = Math.max(margin / 2, (int) Math.round(leadSize * 0.6));
        int canvasHeight = margin + frameOuterHeight + afterFrame
                + titleBlock.height() + betweenBlocks + subtitleBlock.height() + bottom;

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            applyQualityHints(g);
            g.setColor(new Color(style.backgroundRgb()));
            g.fillRect(0, 0, canvasWidth, canvasHeight);

            if (frame > 0) {
                g.setColor(new Color(style.frameRgb()));
                g.fillRect(margin, margin, frameOuterWidth, frameOuterHeight);
                g.setColor(new Color(style.backgroundRgb()));
                g.fillRect(margin + frame, margin + frame, frameOuterWidth - frame * 2, frameOuterHeight - frame * 2);
            }
            g.drawImage(scale(source, imageWidth, imageHeight), margin + border, margin + border, null);

            int y = margin + frameOuterHeight + afterFrame;
            y = draw(g, titleBlock, style.titleRgb(), canvasWidth, y);
            draw(g, subtitleBlock, style.subtitleRgb(), canvasWidth, y + betweenBlocks);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    public static Font resolveFont(String family, String logicalFallback) {
        String wanted = family == null ? "" : family.strip();
        if (!wanted.isEmpty() && (isLogical(wanted) || families().contains(wanted.toLowerCase(Locale.ROOT)))) {
            return new Font(wanted, Font.PLAIN, 12);
        }
        return new Font(logicalFallback, Font.PLAIN, 12);
    }

    public static boolean isFontAvailable(String family) {
        String wanted = family == null ? "" : family.strip();
        return !wanted.isEmpty() && (isLogical(wanted) || families().contains(wanted.toLowerCase(Locale.ROOT)));
    }

    static TextBlock layout(String text, Font base, double size, int maxWidth) {
        int requested = Math.max(6, (int) Math.round(size));
        Font font = base.deriveFont(Font.PLAIN, (float) requested);
        if (text == null || text.isEmpty()) {
            return new TextBlock(font, List.of(), 0, 0);
        }
        Graphics2D scratch = scratchGraphics();
        try {
            int minimum = Math.max(6, (int) Math.round(requested * MIN_FONT_SHRINK));
            int current = requested;
            while (true) {
                font = base.deriveFont(Font.PLAIN, (float) current);
                FontMetrics metrics = scratch.getFontMetrics(font);
                List<String> lines = wrap(text, metrics, maxWidth, current <= minimum);
                if (lines != null) {
                    return new TextBlock(font, lines, metrics.getAscent() + metrics.getDescent(), metrics.getAscent());
                }
                current = Math.max(minimum, (int) Math.floor(current * 0.9));
            }
        } finally {
            scratch.dispose();
        }
    }

    static List<String> wrap(String text, FontMetrics metrics, int maxWidth, boolean allowBreak) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.strip().split(" +")) {
                if (word.isEmpty()) {
                    continue;
                }
                if (metrics.stringWidth(word) > maxWidth) {
                    if (!allowBreak) {
                        return null;
                    }
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    String rest = word;
                    while (metrics.stringWidth(rest) > maxWidth) {
                        int cut = rest.length() - 1;
                        while (cut > 1 && metrics.stringWidth(rest.substring(0, cut)) > maxWidth) {
                            cut--;
                        }
                        lines.add(rest.substring(0, cut));
                        rest = rest.substring(cut);
                    }
                    line.append(rest);
                    continue;
                }
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        int currentWidth = source.getWidth();
        int currentHeight = source.getHeight();
        while (currentWidth / 2 >= width && currentHeight / 2 >= height) {
            currentWidth /= 2;
            currentHeight /= 2;
            current = resize(current, currentWidth, currentHeight);
        }
        return current.getWidth() == width && current.getHeight() == height ? current : resize(current, width, height);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            applyQualityHints(g);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static int draw(Graphics2D g, TextBlock block, int rgb, int canvasWidth, int top) {
        if (block.isEmpty()) {
            return top;
        }
        g.setFont(block.font());
        g.setColor(new Color(rgb));
        FontMetrics metrics = g.getFontMetrics();
        int y = top;
        for (String line : block.lines()) {
            int x = (canvasWidth - metrics.stringWidth(line)) / 2;
            g.drawString(line, x, y + block.ascent());
            y += block.lineHeight();
        }
        return y;
    }

    private static Graphics2D scratchGraphics() {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        applyQualityHints(g);
        return g;
    }

    private static void applyQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static boolean isLogical(String family) {
        return switch (family.toLowerCase(Locale.ROOT)) {
            case "serif", "sansserif", "monospaced", "dialog", "dialoginput" -> true;
            default -> false;
        };
    }

    private static Set<String> families() {
        Set<String> cached = availableFamilies;
        if (cached == null) {
            Set<String> names = new HashSet<>();
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)) {
                names.add(name.toLowerCase(Locale.ROOT));
            }
            cached = Set.copyOf(names);
            availableFamilies = cached;
        }
        return cached;
    }

    record TextBlock(Font font, List<String> lines, int lineHeight, int ascent) {
        boolean isEmpty() {
            return lines.isEmpty();
        }

        int height() {
            return lines.size() * lineHeight;
        }
    }
}
