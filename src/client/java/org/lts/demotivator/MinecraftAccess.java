package org.lts.demotivator;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MinecraftAccess {
    private static final ScreenAccess SCREEN = createScreenAccess();
    private static final HudAccess HUD = createHudAccess();
    private static final ChatAccess CHAT = createChatAccess();
    private static final TargetAccess TARGET = createTargetAccess();

    private MinecraftAccess() {
    }

    public static Screen getScreen(Minecraft minecraft) {
        return SCREEN.get(minecraft);
    }

    public static boolean isHudHidden(Minecraft minecraft) {
        return HUD.isHidden(minecraft);
    }

    public static void setHudHidden(Minecraft minecraft, boolean hidden) {
        if (HUD.isHidden(minecraft) != hidden) {
            HUD.setHidden(minecraft, hidden);
        }
    }

    public static RenderTarget mainRenderTarget(Minecraft minecraft) {
        return TARGET.get(minecraft);
    }

    public static void addChatMessage(Minecraft minecraft, Component message) {
        if (minecraft == null || message == null) {
            return;
        }
        try {
            CHAT.chat(minecraft).addClientSystemMessage(message);
        } catch (RuntimeException exception) {
            DemotivatorClient.LOGGER.warn("Could not add chat message", exception);
        }
    }

    private static ScreenAccess createScreenAccess() {
        try {
            Field field = Minecraft.class.getField("screen");
            return minecraft -> (Screen) field.get(minecraft);
        } catch (NoSuchFieldException ignored) {
            try {
                Field gui = Minecraft.class.getField("gui");
                Method screen = gui.getType().getMethod("screen");
                return minecraft -> (Screen) screen.invoke(gui.get(minecraft));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unsupported Minecraft screen API", exception);
            }
        }
    }

    private static HudAccess createHudAccess() {
        try {
            Field options = Minecraft.class.getField("options");
            Field hideGui = options.getType().getField("hideGui");
            return new HudAccess() {
                @Override
                public boolean isHidden(Minecraft minecraft) {
                    try {
                        return hideGui.getBoolean(options.get(minecraft));
                    } catch (ReflectiveOperationException exception) {
                        throw failed("read HUD visibility", exception);
                    }
                }

                @Override
                public void setHidden(Minecraft minecraft, boolean hidden) {
                    try {
                        hideGui.setBoolean(options.get(minecraft), hidden);
                    } catch (ReflectiveOperationException exception) {
                        throw failed("change HUD visibility", exception);
                    }
                }
            };
        } catch (NoSuchFieldException ignored) {
            try {
                Field gui = Minecraft.class.getField("gui");
                Field hud = gui.getType().getField("hud");
                Method isHidden = hud.getType().getMethod("isHidden");
                Method toggle = hud.getType().getMethod("toggle");
                return new HudAccess() {
                    @Override
                    public boolean isHidden(Minecraft minecraft) {
                        try {
                            return (Boolean) isHidden.invoke(hud.get(gui.get(minecraft)));
                        } catch (ReflectiveOperationException exception) {
                            throw failed("read HUD visibility", exception);
                        }
                    }

                    @Override
                    public void setHidden(Minecraft minecraft, boolean hidden) {
                        try {
                            toggle.invoke(hud.get(gui.get(minecraft)));
                        } catch (ReflectiveOperationException exception) {
                            throw failed("change HUD visibility", exception);
                        }
                    }
                };
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unsupported Minecraft HUD API", exception);
            }
        }
    }

    private static ChatAccess createChatAccess() {
        try {
            Field gui = Minecraft.class.getField("gui");
            try {
                Field hud = gui.getType().getField("hud");
                Method getChat = hud.getType().getMethod("getChat");
                return minecraft -> (ChatComponent) getChat.invoke(hud.get(gui.get(minecraft)));
            } catch (NoSuchFieldException | NoSuchMethodException ignored) {
                Method getChat = gui.getType().getMethod("getChat");
                return minecraft -> (ChatComponent) getChat.invoke(gui.get(minecraft));
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Minecraft chat API", exception);
        }
    }

    private static TargetAccess createTargetAccess() {
        try {
            Method method = Minecraft.class.getMethod("getMainRenderTarget");
            return minecraft -> (RenderTarget) method.invoke(minecraft);
        } catch (NoSuchMethodException ignored) {
            try {
                Field gameRenderer = Minecraft.class.getField("gameRenderer");
                Method method = gameRenderer.getType().getMethod("mainRenderTarget");
                return minecraft -> (RenderTarget) method.invoke(gameRenderer.get(minecraft));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unsupported Minecraft render target API", exception);
            }
        }
    }

    private static RuntimeException failed(String action, ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException && exception.getCause() != null
                ? exception.getCause()
                : exception;
        return new IllegalStateException("Could not " + action, cause);
    }

    @FunctionalInterface
    private interface ScreenAccess {
        Screen resolve(Minecraft minecraft) throws ReflectiveOperationException;

        default Screen get(Minecraft minecraft) {
            try {
                return resolve(minecraft);
            } catch (ReflectiveOperationException exception) {
                throw failed("read the active screen", exception);
            }
        }
    }

    private interface HudAccess {
        boolean isHidden(Minecraft minecraft);

        void setHidden(Minecraft minecraft, boolean hidden);
    }

    @FunctionalInterface
    private interface ChatAccess {
        ChatComponent resolve(Minecraft minecraft) throws ReflectiveOperationException;

        default ChatComponent chat(Minecraft minecraft) {
            try {
                return resolve(minecraft);
            } catch (ReflectiveOperationException exception) {
                throw failed("access chat", exception);
            }
        }
    }

    @FunctionalInterface
    private interface TargetAccess {
        RenderTarget resolve(Minecraft minecraft) throws ReflectiveOperationException;

        default RenderTarget get(Minecraft minecraft) {
            try {
                return resolve(minecraft);
            } catch (ReflectiveOperationException exception) {
                throw failed("access the main render target", exception);
            }
        }
    }
}
