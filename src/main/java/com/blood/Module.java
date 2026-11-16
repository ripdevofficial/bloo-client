package com.blood;

import net.minecraft.client.Minecraft;

/** One toggleable feature. Override onTick for things that run every game tick. */
public abstract class Module {
    public final String name;
    public boolean enabled;

    protected Module(String name) { this.name = name; }

    public void toggle(Minecraft mc) {
        enabled = !enabled;
        if (enabled) onEnable(mc); else onDisable(mc);
    }

    public void onEnable(Minecraft mc) {}
    public void onDisable(Minecraft mc) {}
    public void onTick(Minecraft mc) {}
}
