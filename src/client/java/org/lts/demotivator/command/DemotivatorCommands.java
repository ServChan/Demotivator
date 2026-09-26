package org.lts.demotivator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lts.demotivator.DemotivatorConfig;
import org.lts.demotivator.DemotivatorManager;
import org.lts.demotivator.DemotivatorResult;
import org.lts.demotivator.gui.DemotivatorConfigScreen;
import org.lts.demotivator.gui.DemotivatorPreviewScreen;
import org.lts.demotivator.render.DemotivatorText;

public final class DemotivatorCommands {
    private DemotivatorCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("demotivate")
                .executes(DemotivatorCommands::usage)
                .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> create(context, StringArgumentType.getString(context, "text")))));

        dispatcher.register(ClientCommands.literal("demotivator")
                .executes(DemotivatorCommands::openConfig)
                .then(ClientCommands.literal("config").executes(DemotivatorCommands::openConfig))
                .then(ClientCommands.literal("last").executes(DemotivatorCommands::openLast))
                .then(ClientCommands.literal("folder").executes(context -> {
                    DemotivatorManager.getInstance().openFolder();
                    return 1;
                }))
                .then(ClientCommands.literal("help").executes(DemotivatorCommands::usage)));
    }

    private static int create(CommandContext<FabricClientCommandSource> context, String raw) {
        DemotivatorText text = DemotivatorText.parse(raw, DemotivatorConfig.current().defaultSubtitle);
        if (text.isEmpty()) {
            context.getSource().sendError(Component.translatable("demotivator.message.empty"));
            return 0;
        }
        if (!DemotivatorManager.getInstance().request(text)) {
            context.getSource().sendError(Component.translatable("demotivator.message.busy"));
            return 0;
        }
        if (DemotivatorConfig.current().hideHud) {
            context.getSource().sendFeedback(Component.translatable("demotivator.message.capturing")
                    .withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        DemotivatorManager.getInstance().openNextTick(() -> new DemotivatorConfigScreen(null));
        return 1;
    }

    private static int openLast(CommandContext<FabricClientCommandSource> context) {
        DemotivatorResult last = DemotivatorManager.getInstance().lastResult();
        if (last == null || last.texture() == null) {
            context.getSource().sendError(Component.translatable("demotivator.message.no_last"));
            return 0;
        }
        DemotivatorManager.getInstance().openNextTick(() -> new DemotivatorPreviewScreen(null, last));
        return 1;
    }

    private static int usage(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        source.sendFeedback(Component.translatable("demotivator.help.header").withStyle(ChatFormatting.GOLD));
        source.sendFeedback(Component.translatable("demotivator.help.create"));
        source.sendFeedback(Component.translatable("demotivator.help.subtitle"));
        source.sendFeedback(Component.translatable("demotivator.help.newline"));
        source.sendFeedback(Component.translatable("demotivator.help.config"));
        source.sendFeedback(Component.translatable("demotivator.help.last"));
        source.sendFeedback(Component.translatable("demotivator.help.folder"));
        return 1;
    }
}
