package net.pbil.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.pbil.client.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    // Zoom und Custom FOV greifen hier ein.
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void pbil$modifyFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ModuleManager.modifyFov(cir.getReturnValueF()));
    }
}
