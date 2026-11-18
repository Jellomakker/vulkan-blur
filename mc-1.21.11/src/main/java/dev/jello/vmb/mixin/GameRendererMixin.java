package dev.jello.vmb.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import dev.jello.vmb.BlurRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private CrossFrameResourcePool resourcePool;

    /** After world + vanilla post effects, before the GUI is drawn (HUD stays sharp). */
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void vmb$applyMotionBlur(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        BlurRenderer.apply(this.minecraft, this.resourcePool);
    }

    @Inject(method = "resize(II)V", at = @At("TAIL"))
    private void vmb$onResize(int width, int height, CallbackInfo ci) {
        BlurRenderer.invalidate();
    }
}
