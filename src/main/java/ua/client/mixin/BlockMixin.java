package ua.client.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ua.client.module.ModuleManager;
import ua.client.module.Modules;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void ukr$xray(BlockState state, BlockView world, BlockPos pos, Direction side,
                                 CallbackInfoReturnable<Boolean> cir) {
        Modules.Xray x = ModuleManager.get(Modules.Xray.class);
        if (x != null && x.enabled) cir.setReturnValue(x.visible(state.getBlock()));
    }
}
