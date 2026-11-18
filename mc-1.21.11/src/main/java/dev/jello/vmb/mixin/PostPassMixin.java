package dev.jello.vmb.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Post-pass custom uniform buffers are created UNIFORM-only. Add COPY_DST so our pass's
 * BlendFactor can be rewritten every frame with CommandEncoder.writeToBuffer.
 * Only affects small per-pass uniform buffers; no behaviour change for vanilla effects.
 */
@Mixin(PostPass.class)
public abstract class PostPassMixin {
    @ModifyArg(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuDevice;createBuffer(Ljava/util/function/Supplier;ILjava/nio/ByteBuffer;)Lcom/mojang/blaze3d/buffers/GpuBuffer;"),
            index = 1)
    private int vmb$makeUniformWritable(int usage) {
        return (usage & GpuBuffer.USAGE_UNIFORM) != 0 ? usage | GpuBuffer.USAGE_COPY_DST : usage;
    }
}
