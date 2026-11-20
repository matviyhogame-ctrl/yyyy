package ua.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Util {
    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static boolean living(Entity e) {
        return e instanceof LivingEntity && e != mc().player && e.isAlive()
                && !(e instanceof ArmorStandEntity) && ((LivingEntity) e).getHealth() > 0;
    }

    public static float[] rotations(Entity e) {
        Vec3d eye = mc().player.getCameraPosVec(1f);
        Vec3d t = e.getBoundingBox().getCenter();
        double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    public static boolean moving() {
        ClientPlayerEntity p = mc().player;
        Input in = p.input;
        return in.movementForward != 0 || in.movementSideways != 0;
    }

    /** Sets horizontal velocity in the direction the player is pressing keys, keeps Y. */
    public static void setSpeed(double speed) {
        ClientPlayerEntity p = mc().player;
        Input in = p.input;
        float f = in.movementForward, s = in.movementSideways;
        Vec3d v = p.getVelocity();
        if (f == 0 && s == 0) { p.setVelocity(0, v.y, 0); return; }
        double yaw = Math.toRadians(p.yaw);
        double dx = s * Math.cos(yaw) - f * Math.sin(yaw);
        double dz = f * Math.cos(yaw) + s * Math.sin(yaw);
        double len = Math.sqrt(dx * dx + dz * dz);
        p.setVelocity(dx / len * speed, v.y, dz / len * speed);
    }
}
