package dev.jello.vmb;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VulkanMotionBlur implements ClientModInitializer {
    public static final String MOD_ID = "vulkanmotionblur";
    public static final Logger LOGGER = LoggerFactory.getLogger("Vulkan Blur");

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        BlurConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.vulkanmotionblur.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                BlurConfig cfg = BlurConfig.get();
                cfg.enabled = !cfg.enabled;
                cfg.save();
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("Motion Blur: " + (cfg.enabled ? "§aON" : "§cOFF")), true);
                }
            }
        });
        LOGGER.info("Vulkan Blur (motion blur) ready");
    }
}
