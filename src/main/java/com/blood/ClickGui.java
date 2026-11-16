package com.blood;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClickGui extends Screen {
    public ClickGui() { super(Component.literal("Blood Client")); }

    @Override
    protected void init() {
        int w = 150, x = width / 2 - w / 2, y = 40;
        for (Module m : Modules.ALL) {
            Button b = Button.builder(label(m), btn -> {
                m.toggle(minecraft);
                btn.setMessage(label(m));
            }).bounds(x, y, w, 20).build();
            addRenderableWidget(b);
            y += 24;
        }
    }

    private static Component label(Module m) {
        int color = m.enabled ? 0xFF3030 : 0xAAAAAA;
        return Component.literal(m.name + ": " + (m.enabled ? "ON" : "OFF")).withStyle(s -> s.withColor(color));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xCC2A0000); // dark blood-red background
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, "BLOOD CLIENT", width / 2, 20, 0xFFC00000);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
