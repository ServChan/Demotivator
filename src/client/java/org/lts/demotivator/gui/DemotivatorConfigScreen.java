package org.lts.demotivator.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.demotivator.DemotivatorConfig;
import org.lts.demotivator.DemotivatorManager;
import org.lts.demotivator.render.ColorCodec;
import org.lts.demotivator.render.DemotivatorComposer;
import org.lts.demotivator.render.OutputFiles;
import org.lwjgl.glfw.GLFW;

public class DemotivatorConfigScreen extends Screen {
    private static final int BG_CONTAINER = 0xF00D141F;
    private static final int BG_HEADER = 0xFF182638;
    private static final int BORDER_PRIMARY = 0xFF2E435E;
    private static final int ACCENT_CYAN = 0xFF00E5FF;
    private static final int TEXT_DIM = 0xFF8FA7BF;
    private static final int TEXT_NORMAL = 0xFFE0E0E0;
    private static final int STATUS_ON = 0xFF55FF55;
    private static final int STATUS_OFF = 0xFFFF5555;
    private static final int STATUS_WARN = 0xFFFFC857;

    private static final int FIELD_HEIGHT = 20;
    private static final int CONTENT_TOP = 66;
    private static final int NUMBER_WIDTH = 70;
    private static final int TEXT_WIDTH = 120;

    private final Screen parent;
    private final DemotivatorConfig config;
    private final Map<String, String> drafts = new HashMap<>();
    private final Map<String, FieldSpec> specs = new LinkedHashMap<>();
    private final Map<String, EditBox> fields = new HashMap<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();

    private Tab tab = Tab.QUICK;
    private int rowStep = 24;
    private Button saveButton;
    private String validationError = "";
    private String warning = "";

    public DemotivatorConfigScreen(Screen parent) {
        super(Component.translatable("demotivator.screen.title"));
        this.parent = parent;
        this.config = DemotivatorConfig.currentCopy();
        defineFields();
    }

    @Override
    protected void init() {
        fields.clear();
        labels.clear();
        swatches.clear();
        rowStep = this.height < 300 ? 22 : 24;
        int contentWidth = contentWidth();
        int left = (this.width - contentWidth) / 2;

        int tabWidth = Math.min(160, (contentWidth - 10) / 2);
        addRenderableWidget(Button.builder(tabLabel(Tab.QUICK), button -> switchTab(Tab.QUICK))
                .bounds(left, 36, tabWidth, FIELD_HEIGHT).build()).active = tab != Tab.QUICK;
        addRenderableWidget(Button.builder(tabLabel(Tab.STYLE), button -> switchTab(Tab.STYLE))
                .bounds(left + tabWidth + 10, 36, tabWidth, FIELD_HEIGHT).build()).active = tab != Tab.STYLE;

        if (tab == Tab.QUICK) {
            buildQuickTab(left, contentWidth);
        } else {
            buildStyleTab(left, contentWidth);
        }

        int buttonY = this.height - 30;
        int buttonWidth = Math.min(150, (contentWidth - 20) / 3);
        int buttonsLeft = (this.width - (buttonWidth * 3 + 20)) / 2;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("demotivator.button.save"),
                button -> saveAndClose()).bounds(buttonsLeft, buttonY, buttonWidth, FIELD_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.reset"), button -> resetDefaults())
                .bounds(buttonsLeft + buttonWidth + 10, buttonY, buttonWidth, FIELD_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.cancel"), button -> onClose())
                .bounds(buttonsLeft + (buttonWidth + 10) * 2, buttonY, buttonWidth, FIELD_HEIGHT).build());
        validate();
    }

    private void buildQuickTab(int left, int contentWidth) {
        int y = CONTENT_TOP;
        int half = (contentWidth - 10) / 2;
        addToggle(left, y, half, "demotivator.option.hide_hud", () -> config.hideHud, v -> config.hideHud = v);
        addToggle(left + half + 10, y, half, "demotivator.option.show_preview",
                () -> config.showPreview, v -> config.showPreview = v);
        y += rowStep;
        addToggle(left, y, half, "demotivator.option.chat_message", () -> config.chatMessage, v -> config.chatMessage = v);
        addRenderableWidget(Button.builder(Component.translatable("demotivator.button.open_folder"),
                        button -> DemotivatorManager.getInstance().openFolder())
                .bounds(left + half + 10, y, half, FIELD_HEIGHT).build());
        y += rowStep + 6;
        placeField("delay", left, y, contentWidth, NUMBER_WIDTH);
        y += rowStep;
        placeField("folder", left, y, contentWidth, TEXT_WIDTH + 40);
        y += rowStep;
        placeField("default_subtitle", left, y, contentWidth, Math.min(260, contentWidth / 2));
        y += rowStep + 8;
        List<Supplier<Component>> hints = List.of(
                () -> Component.translatable("demotivator.hint.usage"),
                () -> Component.translatable("demotivator.hint.subtitle"),
                () -> Component.translatable("demotivator.hint.output",
                        "screenshots/" + OutputFiles.sanitizeFolder(config.outputFolder)));
        for (Supplier<Component> hint : hints) {
            if (y + 9 >= this.height - 48) {
                break;
            }
            labels.add(new Label(hint, left, y, TEXT_DIM, contentWidth));
            y += 12;
        }
    }

    private void buildStyleTab(int left, int contentWidth) {
        int half = (contentWidth - 10) / 2;
        int right = left + half + 10;
        int y = CONTENT_TOP;
        for (String id : List.of("width", "margin", "frame", "gap", "title_size", "subtitle_size")) {
            placeField(id, left, y, half, NUMBER_WIDTH);
            y += rowStep;
        }
        addToggle(left, y, half, "demotivator.option.uppercase",
                () -> config.uppercaseTitle, v -> config.uppercaseTitle = v);

        y = CONTENT_TOP;
        for (String id : List.of("title_font", "subtitle_font")) {
            placeField(id, right, y, half, Math.min(TEXT_WIDTH, half / 2));
            y += rowStep;
        }
        for (String id : List.of("background", "frame_color", "title_color", "subtitle_color")) {
            int fieldWidth = NUMBER_WIDTH;
            EditBox box = placeField(id, right, y, half, fieldWidth + 18);
            swatches.add(new Swatch(id, box.getX() + fieldWidth + 4, y + 3));
            box.setWidth(fieldWidth);
            y += rowStep;
        }
    }

    private void defineFields() {
        intField("delay", "demotivator.option.delay", DemotivatorConfig.MIN_DELAY, DemotivatorConfig.MAX_DELAY,
                () -> config.captureDelayTicks, v -> config.captureDelayTicks = v);
        textField("folder", "demotivator.option.folder", 64, () -> config.outputFolder, value -> {
            String stripped = value.strip();
            return !stripped.isEmpty() && OutputFiles.sanitizeFolder(stripped).equals(stripped)
                    ? null : "demotivator.error.folder";
        }, v -> config.outputFolder = v.strip());
        textField("default_subtitle", "demotivator.option.default_subtitle", DemotivatorConfig.MAX_DEFAULT_SUBTITLE,
                () -> config.defaultSubtitle, value -> null, v -> config.defaultSubtitle = v.strip());
        intField("width", "demotivator.option.width", DemotivatorConfig.MIN_WIDTH, DemotivatorConfig.MAX_WIDTH,
                () -> config.imageWidth, v -> config.imageWidth = v);
        doubleField("margin", "demotivator.option.margin", DemotivatorConfig.MIN_MARGIN, DemotivatorConfig.MAX_MARGIN,
                () -> config.marginPercent, v -> config.marginPercent = v);
        intField("frame", "demotivator.option.frame", 0, DemotivatorConfig.MAX_FRAME,
                () -> config.frameThickness, v -> config.frameThickness = v);
        intField("gap", "demotivator.option.gap", 0, DemotivatorConfig.MAX_FRAME,
                () -> config.frameGap, v -> config.frameGap = v);
        doubleField("title_size", "demotivator.option.title_size", DemotivatorConfig.MIN_TITLE,
                DemotivatorConfig.MAX_TITLE, () -> config.titleSizePercent, v -> config.titleSizePercent = v);
        doubleField("subtitle_size", "demotivator.option.subtitle_size", DemotivatorConfig.MIN_SUBTITLE,
                DemotivatorConfig.MAX_SUBTITLE, () -> config.subtitleSizePercent, v -> config.subtitleSizePercent = v);
        fontField("title_font", "demotivator.option.title_font", () -> config.titleFont, v -> config.titleFont = v);
        fontField("subtitle_font", "demotivator.option.subtitle_font",
                () -> config.subtitleFont, v -> config.subtitleFont = v);
        colorField("background", "demotivator.option.background",
                () -> config.backgroundColor, v -> config.backgroundColor = v);
        colorField("frame_color", "demotivator.option.frame_color", () -> config.frameColor, v -> config.frameColor = v);
        colorField("title_color", "demotivator.option.title_color", () -> config.titleColor, v -> config.titleColor = v);
        colorField("subtitle_color", "demotivator.option.subtitle_color",
                () -> config.subtitleColor, v -> config.subtitleColor = v);
    }

    private void intField(String id, String labelKey, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter) {
        specs.put(id, new FieldSpec(id, labelKey, 8, () -> Integer.toString(getter.get()), value -> {
            try {
                int parsed = Integer.parseInt(value.strip());
                return parsed >= min && parsed <= max ? null : "demotivator.error.range";
            } catch (NumberFormatException exception) {
                return "demotivator.error.number";
            }
        }, value -> setter.accept(Integer.parseInt(value.strip())), min + "-" + max));
    }

    private void doubleField(String id, String labelKey, double min, double max,
                             Supplier<Double> getter, Consumer<Double> setter) {
        specs.put(id, new FieldSpec(id, labelKey, 8, () -> formatDouble(getter.get()), value -> {
            try {
                double parsed = Double.parseDouble(value.strip().replace(',', '.'));
                return Double.isFinite(parsed) && parsed >= min && parsed <= max ? null : "demotivator.error.range";
            } catch (NumberFormatException exception) {
                return "demotivator.error.number";
            }
        }, value -> setter.accept(Double.parseDouble(value.strip().replace(',', '.'))),
                formatDouble(min) + "-" + formatDouble(max)));
    }

    private void textField(String id, String labelKey, int maxLength, Supplier<String> getter,
                           Function<String, String> validator, Consumer<String> setter) {
        specs.put(id, new FieldSpec(id, labelKey, maxLength, getter, validator, setter, ""));
    }

    private void fontField(String id, String labelKey, Supplier<String> getter, Consumer<String> setter) {
        textField(id, labelKey, DemotivatorConfig.MAX_FONT_NAME, getter,
                value -> value.isBlank() ? "demotivator.error.font" : null, v -> setter.accept(v.strip()));
    }

    private void colorField(String id, String labelKey, Supplier<String> getter, Consumer<String> setter) {
        specs.put(id, new FieldSpec(id, labelKey, 9, getter,
                value -> ColorCodec.parse(value).isPresent() ? null : "demotivator.error.color",
                value -> setter.accept(ColorCodec.normalize(value, value)), "#RRGGBB"));
    }

    private EditBox placeField(String id, int x, int y, int rowWidth, int fieldWidth) {
        FieldSpec spec = specs.get(id);
        Component label = Component.translatable(spec.labelKey());
        int fieldX = x + rowWidth - fieldWidth;
        labels.add(new Label(() -> label, x, y + 6, ACCENT_CYAN, Math.max(20, rowWidth - fieldWidth - 6)));
        EditBox box = new EditBox(this.font, fieldX, y, fieldWidth, FIELD_HEIGHT, label);
        box.setMaxLength(spec.maxLength());
        box.setValue(drafts.getOrDefault(id, spec.getter().get()));
        if (!spec.hint().isEmpty()) {
            box.setHint(Component.literal(spec.hint()));
        }
        box.setResponder(value -> {
            drafts.put(id, value);
            validate();
        });
        fields.put(id, addRenderableWidget(box));
        return box;
    }

    private void addToggle(int x, int y, int width, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(toggleLabel(key, getter.get()), button -> {
            setter.accept(!getter.get());
            button.setMessage(toggleLabel(key, getter.get()));
        }).bounds(x, y, width, FIELD_HEIGHT).build());
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.translatable(key).append(": ").append(
                Component.translatable(value ? "demotivator.state.on" : "demotivator.state.off")
                        .withStyle(style -> style.withColor(value ? STATUS_ON : STATUS_OFF)));
    }

    private Component tabLabel(Tab target) {
        Component label = Component.translatable(target.key);
        return target == tab ? Component.empty().append(label).append(" *") : label;
    }

    private void switchTab(Tab target) {
        if (tab != target) {
            tab = target;
            rebuildWidgets();
        }
    }

    private boolean validate() {
        validationError = "";
        warning = "";
        boolean valid = true;
        for (FieldSpec spec : specs.values()) {
            String value = drafts.getOrDefault(spec.id(), spec.getter().get());
            String error = spec.validator().apply(value);
            EditBox box = fields.get(spec.id());
            if (box != null) {
                box.setTextColor(error == null ? TEXT_NORMAL : STATUS_OFF);
            }
            if (error != null) {
                if (valid) {
                    validationError = Component.translatable(spec.labelKey()).getString() + ": "
                            + Component.translatable(error).getString();
                }
                valid = false;
            } else if (drafts.containsKey(spec.id())) {
                spec.setter().accept(value);
            }
        }
        for (String id : List.of("title_font", "subtitle_font")) {
            String value = drafts.getOrDefault(id, specs.get(id).getter().get());
            if (!value.isBlank() && !DemotivatorComposer.isFontAvailable(value)) {
                warning = Component.translatable("demotivator.warning.font", value.strip()).getString();
                EditBox box = fields.get(id);
                if (box != null) {
                    box.setTextColor(STATUS_WARN);
                }
                break;
            }
        }
        if (saveButton != null) {
            saveButton.active = valid;
        }
        return valid;
    }

    private void resetDefaults() {
        DemotivatorConfig defaults = DemotivatorConfig.defaultsCopy();
        config.hideHud = defaults.hideHud;
        config.captureDelayTicks = defaults.captureDelayTicks;
        config.showPreview = defaults.showPreview;
        config.chatMessage = defaults.chatMessage;
        config.outputFolder = defaults.outputFolder;
        config.defaultSubtitle = defaults.defaultSubtitle;
        config.imageWidth = defaults.imageWidth;
        config.marginPercent = defaults.marginPercent;
        config.frameThickness = defaults.frameThickness;
        config.frameGap = defaults.frameGap;
        config.titleFont = defaults.titleFont;
        config.subtitleFont = defaults.subtitleFont;
        config.titleSizePercent = defaults.titleSizePercent;
        config.subtitleSizePercent = defaults.subtitleSizePercent;
        config.uppercaseTitle = defaults.uppercaseTitle;
        config.backgroundColor = defaults.backgroundColor;
        config.frameColor = defaults.frameColor;
        config.titleColor = defaults.titleColor;
        config.subtitleColor = defaults.subtitleColor;
        drafts.clear();
        rebuildWidgets();
    }

    private void saveAndClose() {
        if (!validate()) {
            return;
        }
        if (DemotivatorConfig.save(config)) {
            onClose();
        } else {
            validationError = Component.translatable("demotivator.error.save_failed").getString();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.width, this.height, BG_CONTAINER);
        graphics.fill(0, 0, this.width, 28, BG_HEADER);
        graphics.fill(0, 27, this.width, 28, BORDER_PRIMARY);
        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        for (Label label : labels) {
            String text = this.font.plainSubstrByWidth(label.text().get().getString(), label.maxWidth());
            graphics.text(this.font, Component.literal(text), label.x(), label.y(), label.color(), false);
        }
        for (Swatch swatch : swatches) {
            String value = drafts.getOrDefault(swatch.id(), specs.get(swatch.id()).getter().get());
            OptionalInt rgb = ColorCodec.parse(value);
            graphics.fill(swatch.x(), swatch.y(), swatch.x() + 14, swatch.y() + 14, BORDER_PRIMARY);
            graphics.fill(swatch.x() + 1, swatch.y() + 1, swatch.x() + 13, swatch.y() + 13,
                    rgb.isPresent() ? 0xFF000000 | rgb.getAsInt() : BG_CONTAINER);
            if (rgb.isEmpty()) {
                graphics.centeredText(this.font, Component.literal("?"), swatch.x() + 7, swatch.y() + 3, STATUS_OFF);
            }
        }
        if (!validationError.isBlank()) {
            graphics.centeredText(this.font, Component.literal(validationError), this.width / 2, this.height - 45, STATUS_OFF);
        } else if (!warning.isBlank()) {
            graphics.centeredText(this.font, Component.literal(warning), this.width / 2, this.height - 45, STATUS_WARN);
        }
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

    private int contentWidth() {
        return Math.min(560, this.width - 40);
    }

    private static String formatDouble(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private enum Tab {
        QUICK("demotivator.tab.quick"),
        STYLE("demotivator.tab.style");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private record FieldSpec(String id, String labelKey, int maxLength, Supplier<String> getter,
                             Function<String, String> validator, Consumer<String> setter, String hint) {
    }

    private record Label(Supplier<Component> text, int x, int y, int color, int maxWidth) {
    }

    private record Swatch(String id, int x, int y) {
    }
}
