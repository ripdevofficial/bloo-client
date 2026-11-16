package com.blood;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier; // if this errors, use net.minecraft.resources.ResourceLocation instead
import org.lwjgl.glfw.GLFW;

public class BloodClient implements ClientModInitializer {
    public static KeyMapping openGui;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bloodclient", "main"));
        openGui = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.bloodclient.gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        Modules.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.consumeClick()) mc.setScreen(new ClickGui());
            if (mc.player == null || mc.level == null) return;
            for (Module m : Modules.ALL) if (m.enabled) m.onTick(mc);
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("bloodclient", "hud"), Hud::render);
    }
}
