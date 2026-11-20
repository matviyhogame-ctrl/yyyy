package ua.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class Target {
    private static LivingEntity last;
    private static long time;
    public static long hitTime;

    public static void set(Entity e) {
        if (e instanceof LivingEntity) { last = (LivingEntity) e; time = System.currentTimeMillis(); }
    }

    public static void hit(Entity e) {
        if (e instanceof LivingEntity) { set(e); hitTime = System.currentTimeMillis(); }
    }

    /** Last attacked / aura target, valid for 4 seconds. */
    public static LivingEntity recent() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (last == null || mc.world == null) return null;
        if (!last.isAlive() || last.world != mc.world || System.currentTimeMillis() - time > 4000) return null;
        return last;
    }

    /** Target for the HUD: recent target, otherwise whatever the crosshair is on. */
    public static LivingEntity hud() {
        LivingEntity r = recent();
        if (r != null) return r;
        HitResult h = MinecraftClient.getInstance().crosshairTarget;
        if (h != null && h.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) h).getEntity();
            if (Util.living(e)) return (LivingEntity) e;
        }
        return null;
    }
}
