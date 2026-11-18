package dev.jello.vmb;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.jello.vmb.mixin.PostChainAccessor;
import dev.jello.vmb.mixin.PostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Exponential frame blending: out = mix(current, history, w).
 * With FPS scaling, w = exp(-dt / tau) so the visible blur length (tau) is the same at 144 or 1000+ fps.
 * Runs after the world (and vanilla post effects) but before the GUI, so the HUD stays sharp.
 */
public final class BlurRenderer {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath(VulkanMotionBlur.MOD_ID, "motion_blur");
    private static final double MAX_TAU_SECONDS = 0.200; // strength 1.0 = 200 ms blur window (10% = 20 ms)
    private static final float MAX_WEIGHT = 0.96f;

    private static long lastNanos;
    private static boolean historyValid;
    private static float lastYaw, lastPitch;
    private static double smoothedTurnSpeed;
    private static ByteBuffer scratch;
    private static boolean failedOnce;

    private BlurRenderer() {}

    public static void apply(Minecraft mc, GraphicsResourceAllocator allocator) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 1.0 / 144.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        dt = Mth.clamp(dt, 1.0e-5, 0.25);

        BlurConfig cfg = BlurConfig.get();
        if (!cfg.enabled || mc.level == null || mc.player == null || cfg.strength <= 0.0) {
            historyValid = false;
            return;
        }

        float w;
        if (cfg.fpsScaling) {
            double tau = cfg.strength * MAX_TAU_SECONDS;
            w = (float) Math.exp(-dt / tau);
        } else {
            w = (float) (cfg.strength * 0.9);
        }

        // Camera turn speed (deg/s), smoothed so the blur fades in/out instead of popping.
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        double dYaw = Math.abs(Mth.wrapDegrees(yaw - lastYaw));
        double dPitch = Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;
        double speed = Math.sqrt(dYaw * dYaw + dPitch * dPitch) / dt;
        double k = 1.0 - Math.exp(-dt / 0.04);
        smoothedTurnSpeed += (speed - smoothedTurnSpeed) * k;

        if (cfg.onlyWhileTurning) {
            double f = Mth.clamp((smoothedTurnSpeed - 15.0) / cfg.turnSpeedForFull, 0.0, 1.0);
            w *= (float) f;
        }

        if (w < 0.003f) {
            // Nothing visible to blend: skip all passes (zero cost), re-seed history when blur resumes.
            historyValid = false;
            return;
        }
        if (!historyValid) {
            w = 0.0f; // first active frame: just copy the current frame into history
        }
        w = Math.min(w, MAX_WEIGHT);

        PostChain chain = mc.getShaderManager().getPostChain(CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) {
            if (!failedOnce) {
                VulkanMotionBlur.LOGGER.error("Motion blur post chain failed to load");
                failedOnce = true;
            }
            return;
        }

        try {
            List<PostPass> passes = ((PostChainAccessor) chain).vmb$getPasses();
            GpuBuffer ubo = ((PostPassAccessor) passes.get(0)).vmb$getCustomUniforms().get("MotionBlurConfig");
            if (ubo != null) {
                int size = (int) ubo.size();
                if (scratch == null || scratch.capacity() < size) {
                    if (scratch != null) MemoryUtil.memFree(scratch);
                    scratch = MemoryUtil.memAlloc(Math.max(size, 16));
                }
                scratch.clear();
                for (int i = 0; i < size; i++) scratch.put(i, (byte) 0);
                scratch.putFloat(0, w);
                scratch.limit(size);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(ubo.slice(), scratch);
            }
            chain.process(mc.getMainRenderTarget(), allocator);
            historyValid = true;
        } catch (Exception e) {
            if (!failedOnce) {
                VulkanMotionBlur.LOGGER.error("Motion blur pass failed, disabling for this session", e);
                failedOnce = true;
            }
            cfg.enabled = false;
            historyValid = false;
        }
    }

    /** Called when the window resizes or resources reload: old history is invalid. */
    public static void invalidate() {
        historyValid = false;
    }
}
