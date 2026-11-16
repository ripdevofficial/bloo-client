package com.blood;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class Hud {
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        g.drawString(mc.font, "Blood Client", 4, 4, 0xFFC00000, true);
        int y = 16;
        for (Module m : Modules.ALL) {
            if (!m.enabled) continue;
            g.drawString(mc.font, m.name, 4, y, 0xFFFF4040, true);
            y += 10;
        }
        g.drawString(mc.font, "XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ(),
                4, y + 6, 0xFFFFFFFF, true);
    }
}
