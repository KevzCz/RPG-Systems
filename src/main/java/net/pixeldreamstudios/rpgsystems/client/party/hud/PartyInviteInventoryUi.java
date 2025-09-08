package net.pixeldreamstudios.rpgsystems.client.party.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.client.config.MiscClientConfig;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyInvites;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class PartyInviteInventoryUi {
    private PartyInviteInventoryUi() {}

    private static final Identifier TEX_PLATE = Identifier.of("rpg-systems","textures/gui/party_accept_decline.png");

    private static final Identifier TEX_ACC_NORMAL = Identifier.of("rpg-systems","textures/gui/button/party_invite_accept_normal.png");
    private static final Identifier TEX_ACC_HOVER  = Identifier.of("rpg-systems","textures/gui/button/party_invite_accept_hover.png");
    private static final Identifier TEX_DEC_NORMAL = Identifier.of("rpg-systems","textures/gui/button/party_invite_decline_normal.png");
    private static final Identifier TEX_DEC_HOVER  = Identifier.of("rpg-systems","textures/gui/button/party_invite_decline_hover.png");

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof InventoryScreen)) return;

            final int PLATE_W = 96, PLATE_H = 64;
            final int BTN_W = 14, BTN_H = 14;
            final int PAD = 6;
            final int STACK_GAP = 6;

            var cfg = MiscClientConfig.get();

            final int plateBaseX = cfg.inviteInventoryX;
            final int plateBaseBottom = h - cfg.inviteInventoryY;

            Drawable plateDrawable = (ctx, mx, my, dt) -> {
                var invites = ClientPartyInvites.invites();
                if (invites.isEmpty()) return;

                int i = 0;
                for (var inv : invites) {
                    int pyBottom = plateBaseBottom - i * (PLATE_H + STACK_GAP);
                    int py = pyBottom - PLATE_H;
                    ctx.drawTexture(TEX_PLATE, plateBaseX, py, 0, 0, PLATE_W, PLATE_H, PLATE_W, PLATE_H);

                    String partyName = (inv.partyName == null || inv.partyName.isBlank()) ? "Party" : inv.partyName;
                    drawCenteredScaled(ctx, partyName, plateBaseX, py + PLATE_H - 33, PLATE_W, 0.5f);

                    long ttl = ClientPartyInvites.inviteTtlMs();
                    long now = System.currentTimeMillis();
                    long rem = Math.max(0L, ttl - (now - inv.createdAt));
                    drawCenteredScaled(ctx, formatMmSs(rem), plateBaseX, py + PLATE_H - 15, PLATE_W, 0.5f);

                    i++;
                }
            };
            var acc = (net.pixeldreamstudios.rpgsystems.mixin.client.ScreenAccessor) screen;
            acc.rpgsystems$addDrawable(plateDrawable);

            final java.util.List<ClickableWidget> liveButtons = new java.util.ArrayList<>();
            final java.util.List<UUID> lastInviteIds = new java.util.ArrayList<>();

            Runnable rebuildButtons = () -> {
                for (var wgt : liveButtons) {
                    if (wgt == null) continue;
                    wgt.visible = false;
                    wgt.active = false;
                    acc.rpgsystems$remove(wgt);
                }
                liveButtons.clear();

                var invites = new java.util.ArrayList<>(ClientPartyInvites.invites());
                if (invites.isEmpty()) return;

                for (int i = 0; i < invites.size(); i++) {
                    var inv = invites.get(i);

                    int pyBottom = plateBaseBottom - i * (PLATE_H + STACK_GAP);
                    int py = pyBottom - PLATE_H;

                    int accX = plateBaseX + PAD;
                    int accY = py + PLATE_H - PAD - BTN_H;
                    int decX = plateBaseX + PLATE_W - PAD - BTN_W;
                    int decY = accY;

                    var accBtn = new ScaledIconButton(accX, accY, BTN_W, BTN_H, 0.75f,
                            TEX_ACC_NORMAL, TEX_ACC_HOVER, b -> {
                        ClientPartyInvites.sendResponse(inv.partyId, true);
                        b.active = b.visible = false;
                    });

                    var decBtn = new ScaledIconButton(decX, decY, BTN_W, BTN_H, 0.75f,
                            TEX_DEC_NORMAL, TEX_DEC_HOVER, b -> {
                        ClientPartyInvites.sendResponse(inv.partyId, false);
                        b.active = b.visible = false;
                    });

                    acc.rpgsystems$addDrawableChild(accBtn);
                    acc.rpgsystems$addDrawableChild(decBtn);
                    liveButtons.add(accBtn);
                    liveButtons.add(decBtn);
                }
            };

            {
                var ids = ClientPartyInvites.invites().stream().map(inv -> inv.partyId).toList();
                lastInviteIds.clear();
                lastInviteIds.addAll(ids);
                rebuildButtons.run();
            }

            ScreenEvents.beforeRender(screen).register((scr, ctx, mx, my, dt) -> {
                var idsNow = ClientPartyInvites.invites().stream().map(inv -> inv.partyId).toList();
                if (!idsNow.equals(lastInviteIds)) {
                    lastInviteIds.clear();
                    lastInviteIds.addAll(idsNow);
                    rebuildButtons.run();
                }
            });
        });
    }

    private static void drawCenteredScaled(DrawContext ctx, String text, int x, int y, int plateW, float scale) {
        var mc = MinecraftClient.getInstance();
        int twScaled = Math.round(mc.textRenderer.getWidth(text) * scale);
        int tx = x + Math.max(4, (plateW - twScaled) / 2);
        var m = ctx.getMatrices();
        m.push();
        m.translate(tx, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(mc.textRenderer, text, 0, 0, 0xFFFFFF, false);
        m.pop();
    }

    private static final class ScaledIconButton extends ButtonWidget {
        private final Identifier normal, hover;
        private final float scale;

        ScaledIconButton(int x, int y, int w, int h, float scale,
                         Identifier normal, Identifier hover,
                         PressAction onPress) {
            super(x, y, w, h, Text.empty(), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
            this.normal = normal;
            this.hover  = hover;
            this.scale  = scale;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean isHover = this.isMouseOver(mouseX, mouseY);
            Identifier tex = isHover ? hover : normal;

            int w = getWidth(), h = getHeight();
            int ox = getX() + Math.round((w - w * scale) / 2f);
            int oy = getY() + Math.round((h - h * scale) / 2f);

            var m = ctx.getMatrices();
            m.push();
            m.translate(ox, oy, 0);
            m.scale(scale, scale, 1f);
            ctx.drawTexture(tex, 0, 0, 0, 0, w, h, w, h);
            m.pop();
        }
    }

    private static String formatMmSs(long ms) {
        long sec = Math.max(0L, (ms + 999) / 1000);
        long m = sec / 60;
        long s = sec % 60;
        return m + ":" + (s < 10 ? "0" + s : String.valueOf(s));
    }
}
