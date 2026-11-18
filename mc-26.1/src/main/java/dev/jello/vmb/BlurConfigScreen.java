package dev.jello.vmb;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

/** Lightweight vanilla-widget config screen (no YACL/Cloth dependency). */
public final class BlurConfigScreen extends Screen {
    private final Screen parent;
    private final BlurConfig cfg = BlurConfig.get();

    public BlurConfigScreen(Screen parent) {
        super(Component.literal("Vulkan Blur - Motion Blur Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 220, h = 20, x = this.width / 2 - w / 2;
        int y = this.height / 6;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
                .create(x, y, w, h, Component.literal("Motion Blur"), (b, v) -> cfg.enabled = v));
        y += 24;

        addRenderableWidget(new AbstractSliderButton(x, y, w, h, Component.empty(), cfg.strength) {
            { updateMessage(); }
            @Override protected void updateMessage() {
                setMessage(Component.literal("Strength: " + Math.round(value * 100) + "%"
                        + (cfg.fpsScaling ? "  (" + Math.round(value * 200) + " ms)" : "")));
            }
            @Override protected void applyValue() { cfg.strength = value; }
        });
        y += 24;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.fpsScaling)
                .create(x, y, w, h, Component.literal("FPS Scaling"), (b, v) -> { cfg.fpsScaling = v; rebuildWidgets(); }));
        y += 24;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.onlyWhileTurning)
                .create(x, y, w, h, Component.literal("Only While Turning"), (b, v) -> cfg.onlyWhileTurning = v));
        y += 24;

        double norm = (cfg.turnSpeedForFull - 30.0) / (720.0 - 30.0);
        addRenderableWidget(new AbstractSliderButton(x, y, w, h, Component.empty(), norm) {
            { updateMessage(); }
            private double deg() { return 30.0 + value * (720.0 - 30.0); }
            @Override protected void updateMessage() {
                setMessage(Component.literal("Full Blur At: " + Math.round(deg()) + " deg/s turn"));
            }
            @Override protected void applyValue() { cfg.turnSpeedForFull = deg(); }
        });
        y += 32;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(x, y, w, h).build());
    }

    @Override
    public void onClose() {
        cfg.save();
        BlurRenderer.invalidate();
        this.minecraft.setScreen(parent);
    }
}
