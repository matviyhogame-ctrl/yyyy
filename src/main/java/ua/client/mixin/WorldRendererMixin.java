package ua.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.client.module.ModuleManager;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void ukr$render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                            Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmap,
                            Matrix4f matrix4f, CallbackInfo ci) {
        ModuleManager.render(matrices, camera, tickDelta);
    }
}
