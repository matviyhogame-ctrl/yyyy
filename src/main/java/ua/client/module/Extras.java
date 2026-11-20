package ua.client.module;

import java.util.Random;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import ua.client.RenderUtil;
import ua.client.Target;

/** Custom HUD / hit effects / target outline. HUD parts are drawn in ua.client.Hud. */
public class Extras {

    public static class Watermark extends Module {
        public final Setting size = add(Setting.num("Logo size", 120, 60, 240, 10));
        public Watermark() { super("Watermark", Category.RENDER); }
    }

    public static class Keystrokes extends Module {
        public Keystrokes() { super("Keystrokes", Category.RENDER); }
    }

    public static class TargetHUD extends Module {
        public TargetHUD() { super("TargetHUD", Category.RENDER); }
    }

    public static class HitEffects extends Module {
        static final ParticleEffect[] FX = {
            ParticleTypes.CRIT, ParticleTypes.ENCHANTED_HIT, ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME,
            ParticleTypes.HEART, ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.FIREWORK, ParticleTypes.HAPPY_VILLAGER
        };
        public final Setting mode = add(Setting.mode("Effect", 0,
                "Crit", "Enchant", "Flame", "Soul", "Heart", "Totem", "Firework", "Villager"));
        public final Setting count = add(Setting.num("Amount", 25, 5, 80, 5));
        public final Setting sound = add(Setting.bool("Sound", true));
        public final Setting marker = add(Setting.bool("Hitmarker", true));

        public HitEffects() { super("HitEffects", Category.RENDER); }

        public void onHit(Entity e) {
            if (!(e instanceof LivingEntity) || mc().world == null) return;
            Box b = e.getBoundingBox();
            Random r = mc().world.random;
            ParticleEffect fx = FX[Math.max(0, Math.min(FX.length - 1, mode.i()))];
            for (int i = 0; i < count.i(); i++) {
                double x = b.minX + r.nextDouble() * (b.maxX - b.minX);
                double y = b.minY + r.nextDouble() * (b.maxY - b.minY);
                double z = b.minZ + r.nextDouble() * (b.maxZ - b.minZ);
                mc().world.addParticle(fx, x, y, z,
                        (r.nextDouble() - 0.5) * 0.5, r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.5);
            }
            if (sound.on) {
                mc().world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_ARROW_HIT_PLAYER,
                        SoundCategory.PLAYERS, 0.6f, 1.2f, false);
            }
        }
    }

    public static class TargetESP extends Module {
        public final Setting mode = add(Setting.mode("Mode", 2, "Box", "Glow", "Both"));
        private Entity glowed;

        public TargetESP() { super("TargetESP", Category.RENDER); }

        private void clearGlow() {
            if (glowed != null) { glowed.setGlowing(false); glowed = null; }
        }

        @Override
        public void onTick() {
            LivingEntity t = Target.recent();
            if (mode.i() != 0 && t != null) {
                if (glowed != t) { clearGlow(); t.setGlowing(true); glowed = t; }
            } else {
                clearGlow();
            }
        }

        @Override public void onDisable() { clearGlow(); }

        @Override
        public void onRender(MatrixStack ms, Camera cam, float td) {
            LivingEntity t = Target.recent();
            if (t == null || mode.i() == 1) return;
            Vec3d c = cam.getPos();
            double dx = MathHelper.lerp(td, t.lastRenderX, t.getX()) - t.getX();
            double dy = MathHelper.lerp(td, t.lastRenderY, t.getY()) - t.getY();
            double dz = MathHelper.lerp(td, t.lastRenderZ, t.getZ()) - t.getZ();
            Box b = t.getBoundingBox().expand(0.05).offset(dx - c.x, dy - c.y, dz - c.z);
            float ratio = MathHelper.clamp(t.getHealth() / Math.max(1f, t.getMaxHealth()), 0f, 1f);
            float r = Math.min(1f, 2f * (1f - ratio)), g = Math.min(1f, 2f * ratio);
            Matrix4f m = ms.peek().getModel();
            RenderUtil.begin();
            RenderUtil.box(m, b, r, g, 0.2f, 1f);
            RenderUtil.end();
        }
    }

    public static class KillEffect extends Module {
        static final ParticleEffect[] FX = {
            ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.FLAME, ParticleTypes.TOTEM_OF_UNDYING,
            ParticleTypes.FIREWORK, ParticleTypes.CRIT, ParticleTypes.HEART
        };
        public final Setting mode = add(Setting.mode("Effect", 0,
                "SoulFlame", "Flame", "Totem", "Firework", "Crit", "Heart"));
        public final Setting count = add(Setting.num("Amount", 40, 10, 120, 10));
        public final Setting lightning = add(Setting.bool("LightningSound", true));
        private final java.util.Set<Integer> deadIds = new java.util.HashSet<>();

        public KillEffect() { super("KillEffect", Category.RENDER); }

        @Override
        public void onTick() {
            if (mc().world == null) { deadIds.clear(); return; }
            for (net.minecraft.entity.Entity e : mc().world.getEntities()) {
                if (!(e instanceof LivingEntity) || e == mc().player) continue;
                LivingEntity le = (LivingEntity) e;
                if (!le.isAlive() || le.getHealth() <= 0f) {
                    if (!deadIds.contains(le.getId())) {
                        deadIds.add(le.getId());
                        spawnKill(le);
                    }
                } else {
                    deadIds.remove(le.getId());
                }
            }
        }

        private void spawnKill(LivingEntity le) {
            Box b = le.getBoundingBox();
            Random r = mc().world.random;
            ParticleEffect fx = FX[Math.max(0, Math.min(FX.length - 1, mode.i()))];
            for (int i = 0; i < count.i(); i++) {
                double x = b.minX + r.nextDouble() * (b.maxX - b.minX);
                double y = b.minY + r.nextDouble() * (b.maxY - b.minY);
                double z = b.minZ + r.nextDouble() * (b.maxZ - b.minZ);
                mc().world.addParticle(fx, x, y, z,
                        (r.nextDouble() - 0.5) * 0.6, r.nextDouble() * 0.5, (r.nextDouble() - 0.5) * 0.6);
            }
            if (lightning.on) {
                mc().world.playSound(le.getX(), le.getY(), le.getZ(),
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.7f, 1.0f, false);
            }
        }
    }
}
