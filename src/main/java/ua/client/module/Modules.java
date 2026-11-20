package ua.client.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import ua.client.RenderUtil;
import ua.client.Util;
import ua.client.mixin.InteractionManagerAccessor;
import ua.client.mixin.MinecraftClientAccessor;

public class Modules {

    // ======================= COMBAT =======================

    public static class KillAura extends Module {
        final Setting range = add(Setting.num("Range", 4.5, 2, 6, 0.1));
        final Setting players = add(Setting.bool("Players", true));
        final Setting mobs = add(Setting.bool("Mobs", true));
        final Setting animals = add(Setting.bool("Animals", false));
        final Setting rotate = add(Setting.bool("Rotate", true));

        public KillAura() { super("KillAura", Category.COMBAT); }

        private boolean valid(Entity e) {
            if (!Util.living(e)) return false;
            if (e instanceof PlayerEntity) return players.on;
            if (e instanceof Monster) return mobs.on;
            if (e instanceof AnimalEntity) return animals.on;
            return false;
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            Entity best = null;
            double bd = 1e9;
            for (Entity e : mc().world.getEntities()) {
                if (!valid(e)) continue;
                double d = p.distanceTo(e);
                if (d > range.value || d >= bd) continue;
                bd = d; best = e;
            }
            if (best == null) return;
            ua.client.Target.set(best);
            if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
            if (rotate.on) {
                float[] r = Util.rotations(best);
                p.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(r[0], r[1], p.isOnGround()));
            }
            mc().interactionManager.attackEntity(p, best);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    public static class TriggerBot extends Module {
        final Setting players = add(Setting.bool("Players only", false));
        public TriggerBot() { super("TriggerBot", Category.COMBAT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (mc().currentScreen != null || mc().crosshairTarget == null) return;
            if (mc().crosshairTarget.getType() != HitResult.Type.ENTITY) return;
            Entity e = ((EntityHitResult) mc().crosshairTarget).getEntity();
            if (!Util.living(e) || (players.on && !(e instanceof PlayerEntity))) return;
            if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
            mc().interactionManager.attackEntity(p, e);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    public static class AimAssist extends Module {
        final Setting range = add(Setting.num("Range", 5, 2, 8, 0.5));
        final Setting fov = add(Setting.num("FOV", 60, 10, 180, 5));
        final Setting smooth = add(Setting.num("Smooth", 0.25, 0.05, 1, 0.05));
        public AimAssist() { super("AimAssist", Category.COMBAT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (mc().currentScreen != null) return;
            Entity best = null;
            double bd = fov.value;
            for (Entity e : mc().world.getEntities()) {
                if (!(e instanceof PlayerEntity) || !Util.living(e) || p.distanceTo(e) > range.value) continue;
                float[] r = Util.rotations(e);
                double ang = Math.hypot(MathHelper.wrapDegrees(r[0] - p.yaw), r[1] - p.pitch);
                if (ang < bd) { bd = ang; best = e; }
            }
            if (best == null) return;
            float[] r = Util.rotations(best);
            p.yaw += MathHelper.wrapDegrees(r[0] - p.yaw) * smooth.f();
            p.pitch += (r[1] - p.pitch) * smooth.f();
        }
    }

    public static class Criticals extends Module {
        public Criticals() { super("Criticals", Category.COMBAT); }
    }

    public static class Velocity extends Module {
        public Velocity() { super("Velocity", Category.COMBAT); }
    }

    public static class Reach extends Module {
        public Reach() { super("Reach", Category.COMBAT); }
    }

    public static class Hitbox extends Module {
        public final Setting size = add(Setting.num("Expand", 0.3, 0.1, 1.0, 0.1));
        public Hitbox() { super("Hitbox", Category.COMBAT); }
    }

    public static class AutoTotem extends Module {
        final Setting health = add(Setting.num("Health", 20, 1, 20, 1));
        int delay;
        public AutoTotem() { super("AutoTotem", Category.COMBAT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (p == null || mc().interactionManager == null) return;
            if (delay > 0) { delay--; return; }
            if (p.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
            float hp = p.getHealth() + p.getAbsorptionAmount();
            if (hp > health.value) return;
            for (int i = 0; i < 36; i++) {
                ItemStack s = p.inventory.main.get(i);
                if (s.getItem() == Items.TOTEM_OF_UNDYING) {
                    int handlerSlot = i < 9 ? 36 + i : i;
                    mc().interactionManager.clickSlot(p.playerScreenHandler.syncId, handlerSlot, 0, SlotActionType.PICKUP, p);
                    mc().interactionManager.clickSlot(p.playerScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, p);
                    if (!p.inventory.getCursorStack().isEmpty()) {
                        mc().interactionManager.clickSlot(p.playerScreenHandler.syncId, handlerSlot, 0, SlotActionType.PICKUP, p);
                    }
                    delay = 1;
                    return;
                }
            }
        }
    }

    public static class ClickPearl extends Module {
        final Setting autoDisable = add(Setting.bool("AutoDisable", true));
        public ClickPearl() { super("ClickPearl", Category.MOVEMENT); }

        @Override
        public void onEnable() {
            ClientPlayerEntity p = mc().player;
            if (p == null || mc().world == null || mc().interactionManager == null) {
                setEnabled(false);
                return;
            }
            int pearlSlot = -1;
            for (int i = 0; i < 9; i++) {
                if (p.inventory.getStack(i).getItem() == Items.ENDER_PEARL) {
                    pearlSlot = i;
                    break;
                }
            }
            if (pearlSlot == -1) {
                if (autoDisable.on) setEnabled(false);
                return;
            }
            int oldSlot = p.inventory.selectedSlot;
            p.inventory.selectedSlot = pearlSlot;
            mc().interactionManager.interactItem(p, mc().world, Hand.MAIN_HAND);
            p.swingHand(Hand.MAIN_HAND);
            p.inventory.selectedSlot = oldSlot;
            if (autoDisable.on) setEnabled(false);
        }

        @Override
        public void onTick() {
            if (autoDisable.on) return;
            ClientPlayerEntity p = mc().player;
            if (p == null || mc().world == null || mc().interactionManager == null) return;
            if (mc().options.keyUse.isPressed()) {
                int pearlSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (p.inventory.getStack(i).getItem() == Items.ENDER_PEARL) {
                        pearlSlot = i;
                        break;
                    }
                }
                if (pearlSlot != -1) {
                    int oldSlot = p.inventory.selectedSlot;
                    p.inventory.selectedSlot = pearlSlot;
                    mc().interactionManager.interactItem(p, mc().world, Hand.MAIN_HAND);
                    p.swingHand(Hand.MAIN_HAND);
                    p.inventory.selectedSlot = oldSlot;
                }
            }
        }
    }

    public static class CrystalBreaker extends Module {
        final Setting range = add(Setting.num("Range", 5, 2, 6, 0.5));
        int ticks;
        public CrystalBreaker() { super("CrystalBreaker", Category.COMBAT); }

        @Override
        public void onTick() {
            if (++ticks % 2 != 0) return;
            ClientPlayerEntity p = mc().player;
            for (Entity e : mc().world.getEntities()) {
                if (e instanceof EndCrystalEntity && p.distanceTo(e) <= range.value) {
                    mc().interactionManager.attackEntity(p, e);
                    p.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        }
    }

    // ======================= MOVEMENT =======================

    public static class Flight extends Module {
        final Setting speed = add(Setting.num("Speed", 1.0, 0.1, 5, 0.1));
        final Setting abilities = add(Setting.bool("Creative-style", false));
        public Flight() { super("Flight", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (abilities.on) {
                p.abilities.allowFlying = true;
                p.abilities.flying = true;
                p.abilities.setFlySpeed(speed.f() * 0.05f);
            } else {
                double vy = 0;
                if (mc().options.keyJump.isPressed()) vy = speed.value * 0.5;
                else if (mc().options.keySneak.isPressed()) vy = -speed.value * 0.5;
                p.setVelocity(0, vy, 0);
                if (Util.moving()) Util.setSpeed(speed.value);
                p.fallDistance = 0;
            }
        }

        @Override
        public void onDisable() {
            ClientPlayerEntity p = mc().player;
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                p.abilities.flying = false;
                p.abilities.allowFlying = false;
                p.abilities.setFlySpeed(0.05f);
            }
        }
    }

    public static class Speed extends Module {
        final Setting speed = add(Setting.num("Speed", 0.4, 0.1, 2, 0.05));
        final Setting autoJump = add(Setting.bool("AutoJump", true));
        final Setting air = add(Setting.bool("In air", true));
        public Speed() { super("Speed", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (!Util.moving() || p.isSneaking()) return;
            if (autoJump.on && p.isOnGround()) p.jump();
            if (p.isOnGround() || air.on) Util.setSpeed(speed.value);
        }
    }

    public static class Sprint extends Module {
        public Sprint() { super("Sprint", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (Util.moving() && !p.isSneaking() && !p.horizontalCollision
                    && p.getHungerManager().getFoodLevel() > 6) p.setSprinting(true);
        }
    }

    public static class Step extends Module {
        final Setting height = add(Setting.num("Height", 1.0, 0.6, 2.5, 0.1));
        public Step() { super("Step", Category.MOVEMENT); }

        @Override public void onTick() { mc().player.stepHeight = height.f(); }
        @Override public void onDisable() { if (mc().player != null) mc().player.stepHeight = 0.6f; }
    }

    public static class NoFall extends Module {
        public NoFall() { super("NoFall", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (p.fallDistance > 2.5f && !p.isFallFlying()) {
                p.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
            }
        }
    }

    public static class Spider extends Module {
        public Spider() { super("Spider", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (p.horizontalCollision) {
                Vec3d v = p.getVelocity();
                p.setVelocity(v.x, 0.2, v.z);
            }
        }
    }

    public static class Glide extends Module {
        final Setting fall = add(Setting.num("Fall speed", 0.05, 0.01, 0.2, 0.01));
        public Glide() { super("Glide", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            Vec3d v = p.getVelocity();
            if (!p.isOnGround() && v.y < -fall.value && !p.isTouchingWater()) {
                p.setVelocity(v.x, -fall.value, v.z);
                p.fallDistance = 0;
            }
        }
    }

    public static class Scaffold extends Module {
        public Scaffold() { super("Scaffold", Category.MOVEMENT); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            BlockPos below = p.getBlockPos().down();
            if (!mc().world.getBlockState(below).getMaterial().isReplaceable()) return;
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.inventory.getStack(i);
                if (s.getItem() instanceof BlockItem
                        && ((BlockItem) s.getItem()).getBlock().getDefaultState().isFullCube(mc().world, BlockPos.ORIGIN)) {
                    slot = i; break;
                }
            }
            if (slot == -1) return;
            for (Direction d : Direction.values()) {
                BlockPos n = below.offset(d);
                if (mc().world.getBlockState(n).getMaterial().isReplaceable()) continue;
                Direction face = d.getOpposite();
                Vec3d hit = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                int old = p.inventory.selectedSlot;
                p.inventory.selectedSlot = slot;
                mc().interactionManager.interactBlock(p, mc().world, Hand.MAIN_HAND, new BlockHitResult(hit, face, n, false));
                p.swingHand(Hand.MAIN_HAND);
                p.inventory.selectedSlot = old;
                return;
            }
        }
    }

    // ======================= PLAYER =======================

    public static class FastPlace extends Module {
        public FastPlace() { super("FastPlace", Category.PLAYER); }
        @Override public void onTick() { ((MinecraftClientAccessor) mc()).setItemUseCooldown(0); }
    }

    public static class FastBreak extends Module {
        public FastBreak() { super("FastBreak", Category.PLAYER); }
        @Override public void onTick() { ((InteractionManagerAccessor) mc().interactionManager).setBlockBreakingCooldown(0); }
    }

    public static class Nuker extends Module {
        final Setting radius = add(Setting.num("Radius", 4, 1, 6, 1));
        public Nuker() { super("Nuker", Category.PLAYER); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            BlockPos pp = p.getBlockPos();
            int r = radius.i();
            BlockPos best = null;
            int bd = Integer.MAX_VALUE;
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                BlockPos b = pp.add(x, y, z);
                BlockState s = mc().world.getBlockState(b);
                if (s.isAir() || !s.getFluidState().isEmpty() || s.getHardness(mc().world, b) < 0) continue;
                int d = x * x + y * y + z * z;
                if (d < bd) { bd = d; best = b; }
            }
            if (best == null) return;
            mc().interactionManager.updateBlockBreakingProgress(best, Direction.UP);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    public static class ChestStealer extends Module {
        final Setting delay = add(Setting.num("Delay (ticks)", 2, 0, 10, 1));
        final Setting autoClose = add(Setting.bool("AutoClose", true));
        int wait;
        public ChestStealer() { super("ChestStealer", Category.PLAYER); }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc().player;
            if (!(p.currentScreenHandler instanceof GenericContainerScreenHandler)) return;
            if (wait > 0) { wait--; return; }
            GenericContainerScreenHandler h = (GenericContainerScreenHandler) p.currentScreenHandler;
            int size = h.getRows() * 9;
            for (int i = 0; i < size; i++) {
                if (!h.getSlot(i).getStack().isEmpty()) {
                    mc().interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, p);
                    wait = delay.i();
                    return;
                }
            }
            if (autoClose.on) p.closeHandledScreen();
        }
    }

    // ======================= RENDER =======================

    public static class ESP extends Module {
        final Setting players = add(Setting.bool("Players", true));
        final Setting mobs = add(Setting.bool("Mobs", true));
        final Setting animals = add(Setting.bool("Animals", false));
        public ESP() { super("ESP", Category.RENDER); }

        @Override
        public void onTick() {
            for (Entity e : mc().world.getEntities()) {
                if (!Util.living(e)) continue;
                boolean want = (e instanceof PlayerEntity && players.on)
                        || (e instanceof Monster && mobs.on)
                        || (e instanceof AnimalEntity && animals.on);
                e.setGlowing(want);
            }
        }

        @Override
        public void onDisable() {
            if (mc().world == null) return;
            for (Entity e : mc().world.getEntities()) if (e != mc().player) e.setGlowing(false);
        }
    }

    public static class Tracers extends Module {
        final Setting players = add(Setting.bool("Players", true));
        final Setting mobs = add(Setting.bool("Mobs", false));
        public Tracers() { super("Tracers", Category.RENDER); }

        @Override
        public void onRender(MatrixStack ms, Camera cam, float td) {
            Matrix4f m = ms.peek().getModel();
            Vec3d c = cam.getPos();
            Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
            RenderUtil.begin();
            for (Entity e : mc().world.getEntities()) {
                if (!Util.living(e)) continue;
                boolean pl = e instanceof PlayerEntity;
                if (pl ? !players.on : !mobs.on) continue;
                double x = MathHelper.lerp(td, e.lastRenderX, e.getX()) - c.x;
                double y = MathHelper.lerp(td, e.lastRenderY, e.getY()) + e.getHeight() / 2 - c.y;
                double z = MathHelper.lerp(td, e.lastRenderZ, e.getZ()) - c.z;
                float[] col = pl ? new float[]{1f, 0.2f, 0.2f}
                        : (e instanceof Monster ? new float[]{1f, 0.6f, 0f} : new float[]{0.3f, 1f, 0.3f});
                RenderUtil.line(m, look.x, look.y, look.z, x, y, z, col[0], col[1], col[2], 1f);
            }
            RenderUtil.end();
        }
    }

    public static class StorageESP extends Module {
        public StorageESP() { super("StorageESP", Category.RENDER); }

        @Override
        public void onRender(MatrixStack ms, Camera cam, float td) {
            Matrix4f m = ms.peek().getModel();
            Vec3d c = cam.getPos();
            RenderUtil.begin();
            for (BlockEntity be : new ArrayList<>(mc().world.blockEntities)) {
                float[] col;
                if (be instanceof ChestBlockEntity) col = new float[]{1f, 0.65f, 0f};
                else if (be instanceof EnderChestBlockEntity) col = new float[]{0.7f, 0.2f, 1f};
                else if (be instanceof ShulkerBoxBlockEntity) col = new float[]{1f, 0.4f, 0.8f};
                else if (be instanceof BarrelBlockEntity) col = new float[]{0.6f, 0.4f, 0.2f};
                else continue;
                Box b = new Box(be.getPos()).offset(-c.x, -c.y, -c.z);
                RenderUtil.box(m, b, col[0], col[1], col[2], 1f);
            }
            RenderUtil.end();
        }
    }

    public static class Fullbright extends Module {
        double old = 1.0;
        public Fullbright() { super("Fullbright", Category.RENDER); }
        @Override public void onEnable() { old = mc().options.gamma; }
        @Override public void onTick() { mc().options.gamma = 15.0; }
        @Override public void onDisable() { mc().options.gamma = old; }
    }

    public static class Xray extends Module {
        final Set<Block> blocks = new HashSet<>(Arrays.asList(
                Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
                Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
                Blocks.ANCIENT_DEBRIS, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
                Blocks.SPAWNER, Blocks.LAVA, Blocks.OBSIDIAN));
        double old = 1.0;
        public Xray() { super("Xray", Category.RENDER); }

        public boolean visible(Block b) { return blocks.contains(b); }

        @Override
        public void onEnable() {
            old = mc().options.gamma;
            if (mc().worldRenderer != null) mc().worldRenderer.reload();
        }

        @Override public void onTick() { mc().options.gamma = 15.0; }

        @Override
        public void onDisable() {
            mc().options.gamma = old;
            if (mc().worldRenderer != null) mc().worldRenderer.reload();
        }
    }

    public static class HUD extends Module {
        public HUD() { super("HUD", Category.RENDER); }
    }

    // ======================= MISC =======================

    public static class AutoRespawn extends Module {
        public AutoRespawn() { super("AutoRespawn", Category.MISC); }

        @Override
        public void onTick() {
            if (mc().currentScreen instanceof DeathScreen) mc().player.requestRespawn();
        }
    }
}
