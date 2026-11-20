package ua.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.client.module.ModuleManager;
import ua.client.module.Modules;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "hasExtendedReach", at = @At("HEAD"), cancellable = true)
    private void ukr$reach(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.on(Modules.Reach.class)) cir.setReturnValue(true);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void ukr$crit(PlayerEntity player, Entity target, CallbackInfo ci) {
        ua.client.Target.hit(target);
        ua.client.module.Extras.HitEffects fx = ModuleManager.get(ua.client.module.Extras.HitEffects.class);
        if (fx != null && fx.enabled) fx.onHit(target);
        if (!ModuleManager.on(Modules.Criticals.class)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isInLava()) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y + 0.0625, z, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, false));
    }
}
