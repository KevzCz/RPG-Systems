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
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.screen.PartyScreen;
import net.pixeldreamstudios.rpgsystems.client.title.screen.TitleScreen;
import net.pixeldreamstudios.rpgsystems.network.SystemNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow protected T handler;

    @Unique
    private static final Identifier TITLES_BTN_NORMAL = Identifier.of("rpg-systems", "textures/gui/title/title_normal.png");
    @Unique
    private static final Identifier TITLES_BTN_HOVER  = Identifier.of("rpg-systems", "textures/gui/title/title_hover.png");

    @Unique
    private static final Identifier PARTY_BTN_NORMAL = Identifier.of("rpg-systems", "textures/gui/button/party_button_normal.png");
    @Unique
    private static final Identifier PARTY_BTN_HOVER  = Identifier.of("rpg-systems", "textures/gui/button/party_button_hover.png");

    @Unique
    private static final Identifier PARTY_CREATE_BTN_NORMAL = Identifier.of("rpg-systems", "textures/gui/button/party_create_normal.png");
    @Unique
    private static final Identifier PARTY_CREATE_BTN_HOVER  = Identifier.of("rpg-systems", "textures/gui/button/party_create_hover.png");

    @Unique private int titles$btnSize = 10;
    @Unique private int titles$btnX;
    @Unique private int titles$btnY;

    @Unique private int party$btnSize = 10;
    @Unique private int party$btnX;
    @Unique private int party$btnY;

    @Unique
    private boolean base$shouldAttach() {
        return this.handler instanceof PlayerScreenHandler;
    }

    @Unique
    private boolean titles$shouldAttach() {
        if (!SystemNet.SystemsClientState.titleEnabled()) return false;
        return base$shouldAttach();
    }

    @Unique
    private boolean party$shouldAttach() {
        if (!SystemNet.SystemsClientState.partyEnabled()) return false;
        return base$shouldAttach();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void titles$onInit(CallbackInfo ci) {
        var misc = net.pixeldreamstudios.rpgsystems.client.config.MiscClientConfig.get();

        if (titles$shouldAttach()) {
            this.titles$btnX = this.x + misc.handledTitlesBtnOffsetX;
            this.titles$btnY = this.y + misc.handledTitlesBtnOffsetY;
        }
        if (party$shouldAttach()) {
            this.party$btnX = this.x + misc.handledPartyBtnOffsetX;
            this.party$btnY = this.y + misc.handledPartyBtnOffsetY;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void titles$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (titles$shouldAttach()) {
            int s = this.titles$btnSize;
            boolean hovered = mouseX >= titles$btnX && mouseX <= titles$btnX + s &&
                    mouseY >= titles$btnY && mouseY <= titles$btnY + s;

            Identifier tex = hovered ? TITLES_BTN_HOVER : TITLES_BTN_NORMAL;
            context.drawTexture(tex, titles$btnX, titles$btnY, 0, 0, s, s, s, s);

            if (hovered) {
                context.drawTooltip(this.textRenderer, Text.translatable("screen.rpgsystems.titles"), mouseX, mouseY);
            }
        }

        if (party$shouldAttach()) {
            boolean hasParty = ClientPartyHudData.partyId != null;
            int s = this.party$btnSize;
            boolean hovered = mouseX >= party$btnX && mouseX <= party$btnX + s &&
                    mouseY >= party$btnY && mouseY <= party$btnY + s;

            Identifier tex = hasParty
                    ? (hovered ? PARTY_BTN_HOVER : PARTY_BTN_NORMAL)
                    : (hovered ? PARTY_CREATE_BTN_HOVER : PARTY_CREATE_BTN_NORMAL);

            context.drawTexture(tex, party$btnX, party$btnY, 0, 0, s, s, s, s);

            if (hovered) {
                Text tip = hasParty
                        ? Text.translatable("screen.rpgsystems.party")
                        : Text.translatable("screen.rpgsystems.party.create");
                context.drawTooltip(this.textRenderer, tip, mouseX, mouseY);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void titles$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (titles$shouldAttach()) {
            int s = this.titles$btnSize;
            if (mouseX >= titles$btnX && mouseX <= titles$btnX + s &&
                    mouseY >= titles$btnY && mouseY <= titles$btnY + s) {
                MinecraftClient.getInstance().setScreen(new TitleScreen());
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (party$shouldAttach()) {
            int s = this.party$btnSize;
            if (mouseX >= party$btnX && mouseX <= party$btnX + s &&
                    mouseY >= party$btnY && mouseY <= party$btnY + s) {
                boolean hasParty = ClientPartyHudData.partyId != null;
                if (hasParty) {
                    MinecraftClient.getInstance().setScreen(new PartyScreen());
                } else {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendChatCommand("party create");
                    }
                }
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}
