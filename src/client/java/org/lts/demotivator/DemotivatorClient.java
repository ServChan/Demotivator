package org.lts.demotivator;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lts.demotivator.command.DemotivatorCommands;
import org.lts.demotivator.gui.DemotivatorConfigScreen;
import org.lts.demotivator.gui.DemotivatorPreviewScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DemotivatorClient implements ClientModInitializer {
    public static final String MOD_ID = "demotivator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping settingsKey;
    private static KeyMapping lastKey;

    @Override
    public void onInitializeClient() {
        DemotivatorConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.demotivator.settings", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
        lastKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.demotivator.last", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                DemotivatorCommands.register(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(DemotivatorClient::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DemotivatorManager.getInstance().shutdown());
        LOGGER.info("Demotivator initialized");
    }

    private static void tick(Minecraft minecraft) {
        DemotivatorManager manager = DemotivatorManager.getInstance();
        while (settingsKey.consumeClick()) {
            if (MinecraftAccess.getScreen(minecraft) == null) {
                manager.openNextTick(() -> new DemotivatorConfigScreen(null));
            }
        }
        while (lastKey.consumeClick()) {
            DemotivatorResult last = manager.lastResult();
            if (MinecraftAccess.getScreen(minecraft) == null && last != null && last.texture() != null) {
                manager.openNextTick(() -> new DemotivatorPreviewScreen(null, last));
            }
        }
        manager.tick(minecraft);
    }
}
