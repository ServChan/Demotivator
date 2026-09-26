package org.lts.demotivator.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lts.demotivator.DemotivatorClient;
import org.lts.demotivator.DemotivatorManager;
import org.lts.demotivator.DemotivatorResult;
import org.lwjgl.glfw.GLFW;

public class DemotivatorPreviewScreen extends Screen {
    private static final int BG_CONTAINER = 0xF00D141F;
    private static final int BG_HEADER = 0xFF182638;
    private static final int BORDER_PRIMARY = 0xFF2E435E;
    private static final int TEXT_DIM = 0xFF8FA7BF;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 44;

    private final Screen parent;
    private final DemotivatorResult result;

    public DemotivatorPreviewScreen(Screen parent, DemotivatorResult result) {
        super(Component.translatable("demotivator.preview.title"));
        this.parent = parent;
        this.result = result;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(110, (this.width - 50) / 4);
        int total = buttonWidth * 4 + 30;
        int x = (this.width - total) / 2;
        int y = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.open_file"), button -> openFile())
                .bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.open_folder"),
                        button -> DemotivatorManager.getInstance().openFolder())
                .bounds(x + (buttonWidth + 10), y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.settings"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(new DemotivatorConfigScreen(this));
            }
        }).bounds(x + (buttonWidth + 10) * 2, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.close"), button -> onClose())
                .bounds(x + (buttonWidth + 10) * 3, y, buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.width, this.height, BG_CONTAINER);
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, BG_HEADER);
        graphics.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, BORDER_PRIMARY);
        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int areaTop = HEADER_HEIGHT + 8;
        int areaBottom = this.height - FOOTER_HEIGHT - 4;
        int areaWidth = this.width - 24;
        int areaHeight = Math.max(10, areaBottom - areaTop);
        double scale = Math.min(areaWidth / (double) result.width(), areaHeight / (double) result.height());
        int drawWidth = Math.max(1, (int) Math.floor(result.width() * scale));
        int drawHeight = Math.max(1, (int) Math.floor(result.height() * scale));
        int drawX = (this.width - drawWidth) / 2;
        int drawY = areaTop + (areaHeight - drawHeight) / 2;
        graphics.fill(drawX - 1, drawY - 1, drawX + drawWidth + 1, drawY + drawHeight + 1, BORDER_PRIMARY);
        if (DemotivatorManager.getInstance().isTextureLive(result.texture())) {
            graphics.blit(result.texture(), drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            graphics.fill(drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0xFF000000);
            graphics.centeredText(this.font, Component.translatable("demotivator.preview.unavailable"),
                    this.width / 2, drawY + drawHeight / 2 - 4, TEXT_DIM);
        }

        Component info = Component.translatable("demotivator.preview.info",
                result.file().getFileName().toString(), result.width(), result.height());
        graphics.centeredText(this.font, info, this.width / 2, this.height - FOOTER_HEIGHT + 2, TEXT_DIM);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(parent);
        }
    }

    private void openFile() {
        try {
            Util.getPlatform().openPath(result.file());
        } catch (RuntimeException exception) {
            DemotivatorClient.LOGGER.warn("Could not open {}", result.file(), exception);
        }
    }
}
