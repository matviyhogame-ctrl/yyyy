package ua.client.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.addAll(Arrays.<Module>asList(
            // Combat
            new Modules.KillAura(), new Modules.TriggerBot(), new Modules.AimAssist(), new Modules.Criticals(),
            new Modules.Velocity(), new Modules.Reach(), new Modules.Hitbox(), new Modules.AutoTotem(),
            new Modules.CrystalBreaker(),
            // Movement
            new Modules.Flight(), new Modules.Speed(), new Modules.Sprint(), new Modules.Step(),
            new Modules.NoFall(), new Modules.Spider(), new Modules.Glide(), new Modules.Scaffold(), new Modules.ClickPearl(),
            // Player
            new Modules.FastPlace(), new Modules.FastBreak(), new Modules.Nuker(), new Modules.ChestStealer(),
            // Render
            new Modules.ESP(), new Modules.Tracers(), new Modules.StorageESP(), new Modules.Fullbright(),
            new Modules.Xray(), new Modules.HUD(),
            new Extras.Watermark(), new Extras.Keystrokes(), new Extras.TargetHUD(),
            new Extras.HitEffects(), new Extras.TargetESP(), new Extras.KillEffect(),
            // Misc
            new Modules.AutoRespawn()
        ));
        get(Modules.HUD.class).enabled = true;
        get(Extras.Watermark.class).enabled = true;
        get(Extras.Keystrokes.class).enabled = true;
        get(Extras.TargetHUD.class).enabled = true;
        get(Extras.HitEffects.class).enabled = true;
        get(Extras.TargetESP.class).enabled = true;
        get(Extras.KillEffect.class).enabled = true;
    }

    public static <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (m.getClass() == c) return c.cast(m);
        return null;
    }

    public static boolean on(Class<? extends Module> c) {
        Module m = get(c);
        return m != null && m.enabled;
    }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    public static void disableAll() {
        for (Module m : modules) if (m.enabled && !(m instanceof Modules.HUD)) m.setEnabled(false);
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen == null) {
            long w = mc.getWindow().getHandle();
            for (Module m : modules) {
                if (m.key < 0) continue;
                boolean d = InputUtil.isKeyPressed(w, m.key);
                if (d && !m.down) m.toggle();
                m.down = d;
            }
        }
        for (Module m : modules) {
            if (!m.enabled) continue;
            try { m.onTick(); } catch (Throwable t) { t.printStackTrace(); }
        }
    }

    public static void render(MatrixStack ms, Camera camera, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (Module m : modules) {
            if (!m.enabled) continue;
            try { m.onRender(ms, camera, td); } catch (Throwable t) { t.printStackTrace(); }
        }
    }
}
