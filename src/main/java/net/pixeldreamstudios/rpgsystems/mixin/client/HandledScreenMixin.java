package net.pixeldreamstudios.rpgsystems.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.title.screen.TitleScreen;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    protected HandledScreenMixin(Text title) { super(title); }

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow protected T handler;

    @Unique private static final Identifier TITLES_BTN_NORMAL =
            Identifier.of("rpg-systems", "textures/gui/title/title_normal.png");
    @Unique private static final Identifier TITLES_BTN_HOVER =
            Identifier.of("rpg-systems", "textures/gui/title/title_hover.png");

    @Unique private int titles$btnSize = 10;
    @Unique private int titles$btnX;
    @Unique private int titles$btnY;

    @Unique
    private boolean titles$shouldAttach() {
        if (!RPGSystemsConfig.get().systems.title) return false;
        return this.handler instanceof PlayerScreenHandler;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void titles$onInit(CallbackInfo ci) {
        if (!titles$shouldAttach()) return;
        this.titles$btnX = this.x + 64;
        this.titles$btnY = this.y + 67;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void titles$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!titles$shouldAttach()) return;

        int s = this.titles$btnSize;
        boolean hovered = mouseX >= titles$btnX && mouseX <= titles$btnX + s &&
                mouseY >= titles$btnY && mouseY <= titles$btnY + s;

        Identifier tex = hovered ? TITLES_BTN_HOVER : TITLES_BTN_NORMAL;
        context.drawTexture(tex, titles$btnX, titles$btnY, 0, 0, s, s, s, s);

        if (hovered) {
            context.drawTooltip(this.textRenderer, Text.translatable("screen.rpgsystems.titles"), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void titles$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!titles$shouldAttach()) return;

        int s = this.titles$btnSize;
        if (mouseX >= titles$btnX && mouseX <= titles$btnX + s &&
                mouseY >= titles$btnY && mouseY <= titles$btnY + s) {
            MinecraftClient.getInstance().setScreen(new TitleScreen());
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
