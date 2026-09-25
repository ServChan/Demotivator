package org.lts.demotivator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.lts.demotivator.render.ColorCodec;
import org.lts.demotivator.render.DemotivatorStyle;
import org.lts.demotivator.render.OutputFiles;

public final class DemotivatorConfig {
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile DemotivatorConfig current = defaults();

    public static final int MIN_DELAY = 1;
    public static final int MAX_DELAY = 60;
    public static final int MIN_WIDTH = 320;
    public static final int MAX_WIDTH = 3840;
    public static final double MIN_MARGIN = 2.0;
    public static final double MAX_MARGIN = 30.0;
    public static final int MAX_FRAME = 40;
    public static final double MIN_TITLE = 2.0;
    public static final double MAX_TITLE = 20.0;
    public static final double MIN_SUBTITLE = 1.0;
    public static final double MAX_SUBTITLE = 12.0;
    public static final int MAX_FONT_NAME = 64;
    public static final int MAX_DEFAULT_SUBTITLE = 128;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean hideHud = true;
    public int captureDelayTicks = 3;
    public boolean showPreview = true;
    public boolean chatMessage = true;
    public String outputFolder = OutputFiles.DEFAULT_FOLDER;
    public String defaultSubtitle = "";
    public int imageWidth = 1000;
    public double marginPercent = 10.0;
    public int frameThickness = 3;
    public int frameGap = 4;
    public String titleFont = "Times New Roman";
    public String subtitleFont = "Arial";
    public double titleSizePercent = 8.0;
    public double subtitleSizePercent = 3.6;
    public boolean uppercaseTitle = false;
    public String backgroundColor = "#000000";
    public String frameColor = "#FFFFFF";
    public String titleColor = "#FFFFFF";
    public String subtitleColor = "#FFFFFF";

    public static void load() {
        Path configPath = configPath();
        DemotivatorConfig primary = read(configPath);
        if (primary != null) {
            boolean migrated = primary.configVersion < CURRENT_CONFIG_VERSION;
            current = sanitize(primary);
            if (migrated) {
                try {
                    writeAtomically(configPath, current, true);
                } catch (IOException exception) {
                    DemotivatorClient.LOGGER.error("Could not persist migrated config {}", configPath, exception);
                }
            }
            return;
        }

        DemotivatorConfig backup = read(backupPath(configPath));
        current = sanitize(backup == null ? defaults() : backup);
        try {
            writeAtomically(configPath, current, false);
        } catch (IOException exception) {
            DemotivatorClient.LOGGER.error("Could not initialize config {}", configPath, exception);
        }
    }

    public static DemotivatorConfig current() {
        return current;
    }

    public static DemotivatorConfig currentCopy() {
        return copyOf(current);
    }

    public static DemotivatorConfig defaultsCopy() {
        return defaults();
    }

    public static boolean save(DemotivatorConfig config) {
        DemotivatorConfig clean = sanitize(copyOf(config));
        Path configPath = configPath();
        try {
            writeAtomically(configPath, clean, true);
            current = clean;
            return true;
        } catch (IOException exception) {
            DemotivatorClient.LOGGER.error("Could not save config {}", configPath, exception);
            return false;
        }
    }

    public DemotivatorStyle style() {
        return new DemotivatorStyle(
                imageWidth,
                marginPercent,
                frameThickness,
                frameGap,
                titleFont,
                subtitleFont,
                titleSizePercent,
                subtitleSizePercent,
                uppercaseTitle,
                ColorCodec.parse(backgroundColor).orElse(0x000000),
                ColorCodec.parse(frameColor).orElse(0xFFFFFF),
                ColorCodec.parse(titleColor).orElse(0xFFFFFF),
                ColorCodec.parse(subtitleColor).orElse(0xFFFFFF));
    }

    static DemotivatorConfig sanitize(DemotivatorConfig config) {
        DemotivatorConfig defaults = defaults();
        config.configVersion = CURRENT_CONFIG_VERSION;
        config.captureDelayTicks = clamp(config.captureDelayTicks, MIN_DELAY, MAX_DELAY);
        config.outputFolder = OutputFiles.sanitizeFolder(config.outputFolder);
        config.defaultSubtitle = trimTo(config.defaultSubtitle, MAX_DEFAULT_SUBTITLE, "");
        config.imageWidth = clamp(config.imageWidth, MIN_WIDTH, MAX_WIDTH);
        config.marginPercent = clamp(config.marginPercent, MIN_MARGIN, MAX_MARGIN);
        config.frameThickness = clamp(config.frameThickness, 0, MAX_FRAME);
        config.frameGap = clamp(config.frameGap, 0, MAX_FRAME);
        config.titleFont = trimTo(config.titleFont, MAX_FONT_NAME, defaults.titleFont);
        config.subtitleFont = trimTo(config.subtitleFont, MAX_FONT_NAME, defaults.subtitleFont);
        config.titleSizePercent = clamp(config.titleSizePercent, MIN_TITLE, MAX_TITLE);
        config.subtitleSizePercent = clamp(config.subtitleSizePercent, MIN_SUBTITLE, MAX_SUBTITLE);
        config.backgroundColor = ColorCodec.normalize(config.backgroundColor, defaults.backgroundColor);
        config.frameColor = ColorCodec.normalize(config.frameColor, defaults.frameColor);
        config.titleColor = ColorCodec.normalize(config.titleColor, defaults.titleColor);
        config.subtitleColor = ColorCodec.normalize(config.subtitleColor, defaults.subtitleColor);
        return config;
    }

    private static DemotivatorConfig read(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, DemotivatorConfig.class);
        } catch (IOException | RuntimeException exception) {
            DemotivatorClient.LOGGER.warn("Could not read config {}", path, exception);
            return null;
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("demotivator.json");
    }

    private static void writeAtomically(Path path, DemotivatorConfig config, boolean backupExisting) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(config, writer);
        }
        if (backupExisting && Files.isRegularFile(path)) {
            Files.copy(path, backupPath(path), StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private static DemotivatorConfig defaults() {
        return new DemotivatorConfig();
    }

    private static DemotivatorConfig copyOf(DemotivatorConfig source) {
        return GSON.fromJson(GSON.toJson(source), DemotivatorConfig.class);
    }

    private static String trimTo(String value, int maxLength, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength).strip();
        }
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
