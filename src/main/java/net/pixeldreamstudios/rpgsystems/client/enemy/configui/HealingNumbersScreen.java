package net.pixeldreamstudios.rpgsystems.client.enemy.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.HealingNumbersClientConfig;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

@Environment(EnvType.CLIENT)
public final class HealingNumbersScreen extends Screen {
    private final Screen parent;

    // current, unsaved selection
    private HealingNumbersClientConfig.Mode pendingMode;
    // normalized slider value in [0..1]
    private double normDistance;

    // widgets
    private CheckboxWidget none;
    private CheckboxWidget players;
    private CheckboxWidget spells;
    private CheckboxWidget allEntities;
    private DistanceSlider  distanceSlider;

    public HealingNumbersScreen(Screen parent) {
        super(Text.literal("Healing Numbers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var cfg = HealingNumbersClientConfig.get();
        this.pendingMode  = cfg.mode;
        this.normDistance = norm(cfg.allEntitiesViewDistance);
        rebuildUi();
    }

    private void rebuildUi() {
        this.clearChildren();

        int cx = this.width / 2;
        int y  = 56;
        int h  = 20;
        int w  = 280;

        none = CheckboxWidget.builder(Text.literal("None"), this.textRenderer)
                .pos(cx - w/2, y)
                .checked(pendingMode == HealingNumbersClientConfig.Mode.NONE)
                .callback((cb, checked) -> { select(HealingNumbersClientConfig.Mode.NONE); })
                .build();
        y += 22;

        players = CheckboxWidget.builder(Text.literal("Players"), this.textRenderer)
                .pos(cx - w/2, y)
                .checked(pendingMode == HealingNumbersClientConfig.Mode.PLAYERS)
                .callback((cb, checked) -> { select(HealingNumbersClientConfig.Mode.PLAYERS); })
                .build();
        y += 22;

        spells = CheckboxWidget.builder(Text.literal("Spells"), this.textRenderer)
                .pos(cx - w/2, y)
                .checked(pendingMode == HealingNumbersClientConfig.Mode.SPELLS)
                .callback((cb, checked) -> { select(HealingNumbersClientConfig.Mode.SPELLS); })
                .build();
        y += 22;

        allEntities = CheckboxWidget.builder(Text.literal("All entities"), this.textRenderer)
                .pos(cx - w/2, y)
                .checked(pendingMode == HealingNumbersClientConfig.Mode.ALL_ENTITIES)
                .callback((cb, checked) -> { select(HealingNumbersClientConfig.Mode.ALL_ENTITIES); })
                .build();
        y += 28;

        // Distance slider — ALWAYS visible and applies to every mode
        distanceSlider = new DistanceSlider(
                cx - w/2, y, w, 20,
                () -> normDistance,
                v -> normDistance = clamp01(v)
        );
        this.addDrawableChild(none);
        this.addDrawableChild(players);
        this.addDrawableChild(spells);
        this.addDrawableChild(allEntities);
        this.addDrawableChild(distanceSlider);
        y += 36;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            var cfg = HealingNumbersClientConfig.get();
            cfg.mode = pendingMode;
            cfg.allEntitiesViewDistance = denorm(normDistance);
            HealingNumbersClientConfig.save();
            this.client.setScreen(parent);
        }).size(100, h).position(cx - 110, y).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> this.client.setScreen(parent))
                .size(100, h).position(cx + 10, y).build());
    }

    private void select(HealingNumbersClientConfig.Mode newMode) {
        this.pendingMode = newMode;
        rebuildUi(); // refresh which box is checked
    }

    private static String sliderLabel(double blocks) {
        int b = (int)Math.round(Math.max(2, Math.min(256, blocks)));
        return "Visibility distance: " + b + " blocks";
    }
    private static double norm(double d) { d = Math.max(2, Math.min(256, d)); return (d - 2.0) / (256.0 - 2.0); }
    private static double denorm(double v) { v = clamp01(v); return 2.0 + v * (256.0 - 2.0); }
    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Choose one mode"), this.width / 2, 36, 0xAAAAAA);
    }

    /** Slider wrapper so we never touch protected SliderWidget.value directly. */
    private static final class DistanceSlider extends SliderWidget {
        private final DoubleSupplier getter;
        private final DoubleConsumer onChange;

        DistanceSlider(int x, int y, int w, int h, DoubleSupplier getter, DoubleConsumer onChange) {
            super(x, y, w, h, Text.empty(), clamp01(getter.getAsDouble()));
            this.getter = getter;
            this.onChange = onChange;
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Text.literal(sliderLabel(denorm(this.value))));
        }
        @Override protected void applyValue() {
            onChange.accept(this.value);
            updateMessage();
        }
    }
}
