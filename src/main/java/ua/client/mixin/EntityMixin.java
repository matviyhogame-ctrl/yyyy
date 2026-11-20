package ua.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.client.module.ModuleManager;
import ua.client.module.Modules;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void ukr$hitbox(CallbackInfoReturnable<Float> cir) {
        Modules.Hitbox h = ModuleManager.get(Modules.Hitbox.class);
        if (h != null && h.enabled && (Object) this instanceof LivingEntity
                && (Object) this != MinecraftClient.getInstance().player) {
            cir.setReturnValue(h.size.f());
        }
    }
}
