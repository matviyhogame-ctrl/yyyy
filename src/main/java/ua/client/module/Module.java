package ua.client.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

public abstract class Module {
    public final String name;
    public final Category category;
    public boolean enabled;
    public boolean expanded;
    public boolean down;
    public int key = -1;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    protected Setting add(Setting s) { settings.add(s); return s; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (enabled == e) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender(MatrixStack ms, Camera camera, float tickDelta) {}
}
