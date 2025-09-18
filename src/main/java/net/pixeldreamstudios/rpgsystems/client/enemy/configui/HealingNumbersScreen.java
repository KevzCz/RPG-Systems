package net.pixeldreamstudios.rpgsystems.client.enemy.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.HealingNumbersClientConfig;

@Environment(EnvType.CLIENT)
public final class HealingNumbersScreen extends Screen {
    private final Screen parent;
    private CheckboxWidget enabled;
    private ButtonWidget amountButton;

    private HealingNumbersClientConfig.AmountMode amountMode;

    public HealingNumbersScreen(Screen parent) {
        super(Text.literal("Healing Numbers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var cfg = HealingNumbersClientConfig.get();
        int cx = this.width / 2;
        int y = 56;
        int w = 280;

        amountMode = cfg.amountMode;

        enabled = CheckboxWidget.builder(Text.literal("Enable healing numbers"), this.textRenderer)
                .pos(cx - w / 2, y).checked(cfg.mode != HealingNumbersClientConfig.Mode.NONE).build();
        this.addDrawableChild(enabled);
        y += 24;

        amountButton = ButtonWidget.builder(Text.literal(amountLabel(amountMode)), b -> {
            amountMode = next(amountMode);
            b.setMessage(Text.literal(amountLabel(amountMode)));
        }).dimensions(cx - w / 2, y, w, 20).build();
        this.addDrawableChild(amountButton);
        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            cfg.mode = enabled.isChecked() ? HealingNumbersClientConfig.Mode.ALL : HealingNumbersClientConfig.Mode.NONE;
            cfg.amountMode = amountMode;
            HealingNumbersClientConfig.save();
            this.client.setScreen(parent);
        }).dimensions(cx - 60, y, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Healing Numbers", this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Display and filtering", this.width / 2, 36, 0xAAAAAA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String amountLabel(HealingNumbersClientConfig.AmountMode m) {
        return switch (m) {
            case ATTEMPTED -> "Amount: Attempted (raw heal)";
            case APPLIED   -> "Amount: Applied (after overheal)";
        };
    }

    private static HealingNumbersClientConfig.AmountMode next(HealingNumbersClientConfig.AmountMode m) {
        return (m == HealingNumbersClientConfig.AmountMode.ATTEMPTED)
                ? HealingNumbersClientConfig.AmountMode.APPLIED
                : HealingNumbersClientConfig.AmountMode.ATTEMPTED;
    }
}
