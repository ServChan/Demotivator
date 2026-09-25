package org.lts.demotivator;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.lts.demotivator.gui.DemotivatorPreviewScreen;
import org.lts.demotivator.render.DemotivatorComposer;
import org.lts.demotivator.render.DemotivatorStyle;
import org.lts.demotivator.render.DemotivatorText;
import org.lts.demotivator.render.OutputFiles;
import org.lwjgl.system.MemoryUtil;

public final class DemotivatorManager {
    private static final DemotivatorManager INSTANCE = new DemotivatorManager();
    private static final int SCREEN_WAIT_LIMIT_TICKS = 200;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Demotivator Worker");
        thread.setDaemon(true);
        return thread;
    });

    private Phase phase = Phase.IDLE;
    private DemotivatorText pendingText;
    private DemotivatorConfig pendingConfig;
    private int delayTicks;
    private int waitedTicks;
    private boolean hudChanged;
    private Supplier<Screen> pendingScreen;
    private DemotivatorResult lastResult;
    private int textureCounter;

    private DemotivatorManager() {
    }

    public static DemotivatorManager getInstance() {
        return INSTANCE;
    }

    public boolean isBusy() {
        return phase != Phase.IDLE;
    }

    public DemotivatorResult lastResult() {
        return lastResult;
    }

    public boolean request(DemotivatorText text) {
        if (phase != Phase.IDLE) {
            return false;
        }
        pendingText = text;
        pendingConfig = DemotivatorConfig.currentCopy();
        delayTicks = pendingConfig.captureDelayTicks;
        waitedTicks = 0;
        hudChanged = false;
        phase = Phase.WAITING;
        return true;
    }

    public void openNextTick(Supplier<Screen> screen) {
        pendingScreen = screen;
    }

    public void tick(Minecraft minecraft) {
        if (pendingScreen != null && phase != Phase.WAITING) {
            Supplier<Screen> screen = pendingScreen;
            pendingScreen = null;
            minecraft.setScreenAndShow(screen.get());
        }
        if (phase != Phase.WAITING) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            abort(minecraft, Component.translatable("demotivator.message.no_world"));
            return;
        }
        waitedTicks++;
        if (MinecraftAccess.getScreen(minecraft) != null) {
            if (waitedTicks > SCREEN_WAIT_LIMIT_TICKS) {
                abort(minecraft, Component.translatable("demotivator.message.screen_open"));
            }
            return;
        }
        if (pendingConfig.hideHud && !hudChanged && !MinecraftAccess.isHudHidden(minecraft)) {
            MinecraftAccess.setHudHidden(minecraft, true);
            hudChanged = true;
            delayTicks = Math.max(delayTicks, 2);
        }
        if (--delayTicks > 0) {
            return;
        }
        capture(minecraft);
    }

    public void openFolder() {
        Path folder = outputDirectory(Minecraft.getInstance(), DemotivatorConfig.current());
        try {
            Files.createDirectories(folder);
            Util.getPlatform().openPath(folder);
        } catch (IOException | RuntimeException exception) {
            DemotivatorClient.LOGGER.warn("Could not open folder {}", folder, exception);
        }
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    private void capture(Minecraft minecraft) {
        phase = Phase.CAPTURING;
        DemotivatorText text = pendingText;
        DemotivatorConfig config = pendingConfig;
        try {
            Screenshot.takeScreenshot(MinecraftAccess.mainRenderTarget(minecraft),
                    image -> onCaptured(minecraft, image, text, config));
        } catch (RuntimeException exception) {
            DemotivatorClient.LOGGER.error("Could not take screenshot", exception);
            fail(minecraft, exception);
        } finally {
            restoreHud(minecraft);
        }
    }

    private void onCaptured(Minecraft minecraft, NativeImage image, DemotivatorText text, DemotivatorConfig config) {
        int width;
        int height;
        int[] pixels;
        try {
            width = image.getWidth();
            height = image.getHeight();
            pixels = image.getPixels();
        } catch (RuntimeException exception) {
            DemotivatorClient.LOGGER.error("Could not read screenshot pixels", exception);
            fail(minecraft, exception);
            return;
        } finally {
            image.close();
        }
        phase = Phase.PROCESSING;
        DemotivatorStyle style = config.style();
        Path directory = outputDirectory(minecraft, config);
        try {
            worker.execute(() -> process(minecraft, width, height, pixels, text, style, directory, config));
        } catch (RuntimeException exception) {
            fail(minecraft, exception);
        }
    }

    private void process(Minecraft minecraft, int width, int height, int[] pixels, DemotivatorText text,
                         DemotivatorStyle style, Path directory, DemotivatorConfig config) {
        try {
            BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            source.setRGB(0, 0, width, height, pixels, 0, width);
            BufferedImage poster = DemotivatorComposer.compose(source, text, style);
            Files.createDirectories(directory);
            Path file = OutputFiles.uniqueFile(directory, LocalDateTime.now());
            if (!ImageIO.write(poster, "png", file.toFile())) {
                throw new IOException("No PNG writer available");
            }
            int posterWidth = poster.getWidth();
            int posterHeight = poster.getHeight();
            int[] abgr = toAbgr(poster.getRGB(0, 0, posterWidth, posterHeight, null, 0, posterWidth));
            minecraft.execute(() -> publish(minecraft, file, posterWidth, posterHeight, abgr, config));
        } catch (IOException | RuntimeException exception) {
            DemotivatorClient.LOGGER.error("Could not build demotivator", exception);
            minecraft.execute(() -> fail(minecraft, exception));
        }
    }

    private void publish(Minecraft minecraft, Path file, int width, int height, int[] abgr, DemotivatorConfig config) {
        phase = Phase.IDLE;
        Identifier texture = uploadTexture(minecraft, width, height, abgr);
        if (lastResult != null && lastResult.texture() != null) {
            minecraft.getTextureManager().release(lastResult.texture());
        }
        lastResult = new DemotivatorResult(file, width, height, texture);
        DemotivatorClient.LOGGER.info("Saved demotivator {}", file);
        if (config.chatMessage) {
            MinecraftAccess.addChatMessage(minecraft, savedMessage(file));
        }
        if (config.showPreview && texture != null && MinecraftAccess.getScreen(minecraft) == null) {
            minecraft.setScreenAndShow(new DemotivatorPreviewScreen(null, lastResult));
        }
    }

    private Identifier uploadTexture(Minecraft minecraft, int width, int height, int[] abgr) {
        NativeImage image = null;
        try {
            image = new NativeImage(width, height, false);
            MemoryUtil.memIntBuffer(image.getPointer(), abgr.length).put(abgr);
            Identifier id = Identifier.fromNamespaceAndPath(DemotivatorClient.MOD_ID, "preview/" + (++textureCounter));
            DynamicTexture texture = new DynamicTexture(() -> "Demotivator preview", image);
            minecraft.getTextureManager().register(id, texture);
            texture.upload();
            return id;
        } catch (RuntimeException exception) {
            if (image != null) {
                image.close();
            }
            DemotivatorClient.LOGGER.warn("Could not upload demotivator preview", exception);
            return null;
        }
    }

    private static Component savedMessage(Path file) {
        MutableComponent name = Component.literal(file.getFileName().toString()).withStyle(style -> style
                .withUnderlined(true)
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.OpenFile(file.toAbsolutePath().toString())));
        MutableComponent folder = Component.translatable("demotivator.message.open_folder").withStyle(style -> style
                .withUnderlined(true)
                .withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent.OpenFile(file.getParent().toAbsolutePath().toString())));
        return Component.translatable("demotivator.message.saved", name).append(" ").append(folder);
    }

    private void fail(Minecraft minecraft, Exception exception) {
        String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        abort(minecraft, Component.translatable("demotivator.message.failed", reason));
    }

    private void abort(Minecraft minecraft, Component message) {
        restoreHud(minecraft);
        phase = Phase.IDLE;
        pendingText = null;
        MinecraftAccess.addChatMessage(minecraft, message.copy().withStyle(ChatFormatting.RED));
    }

    private void restoreHud(Minecraft minecraft) {
        if (hudChanged) {
            hudChanged = false;
            try {
                MinecraftAccess.setHudHidden(minecraft, false);
            } catch (RuntimeException exception) {
                DemotivatorClient.LOGGER.warn("Could not restore HUD visibility", exception);
            }
        }
    }

    private static Path outputDirectory(Minecraft minecraft, DemotivatorConfig config) {
        return minecraft.gameDirectory.toPath()
                .resolve(Screenshot.SCREENSHOT_DIR)
                .resolve(OutputFiles.sanitizeFolder(config.outputFolder));
    }

    private static int[] toAbgr(int[] argb) {
        int[] result = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int pixel = argb[i];
            result[i] = 0xFF000000 | (pixel & 0x0000FF00) | ((pixel & 0x00FF0000) >>> 16) | ((pixel & 0x000000FF) << 16);
        }
        return result;
    }

    private enum Phase {
        IDLE,
        WAITING,
        CAPTURING,
        PROCESSING
    }
}
