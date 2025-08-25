package net.pixeldreamstudios.rpgsystems.client.party.screen.box;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyMemberInfoClientConfig;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatPayloads;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

@Environment(EnvType.CLIENT)
public final class PartyMemberInfoBox implements PartyBox {

    private static final boolean DEBUG_BOX = false;

    private static final Identifier INFO_BUTTON = Identifier.of("rpg-systems", "textures/gui/button/info_button.png");
    private static final int ICON_W = 16, ICON_H = 16, ICON_PAD = 4;
    private static boolean iconFiltered = false;

    private int infoBtnX = -1, infoBtnY = -1, infoBtnW = 0, infoBtnH = 0;

    private static final int TL_BOX_W = 120;
    private static final int TL_BOX_MARGIN = 8;
    private static final int TL_BOX_OFFSET_Y = 15;

    private static final int GAP_FROM_LEFT_COLUMN = 12;

    private static final boolean USE_FIXED_SIZE = true;
    private static final int BOX_W = 250;
    private static final int BOX_H_ONE_ROW = 41;

    private static final int PAD = 4;
    private static final int BAR_H = 8;
    private static final int ROW_VGAP = 6;
    private static final int CENTER_GAP_DEFAULT = 26;
    private static final float TEXT_SCALE = 0.6f;

    private static final int BAR_BG = 0xFF202020;
    private static final int HP_COLOR = 0xFF2ECC71;
    private static final int HUNGER_COLOR = 0xFFF39C12;
    private static final int STAMINA_COLOR = 0xFFF39C12;
    private static final int MANA_COLOR = 0xFF3498DB;
    private static final int RPG_MANA_COLOR = 0xFF3498DB;
    @Override
    public void render(DrawContext ctx, TextRenderer tr, int canvasX, int canvasY, int canvasW, int canvasH) {
        int contentLeft  = canvasX + TL_BOX_MARGIN + 18;
        int contentRight = canvasX + canvasW - TL_BOX_MARGIN - 18;

        int infoTop  = canvasY + TL_BOX_MARGIN + TL_BOX_OFFSET_Y;
        int leftColW = TL_BOX_W;

        int x = contentLeft + leftColW + GAP_FROM_LEFT_COLUMN - 60;
        int y = infoTop;

        int autoW = Math.max(0, contentRight - x);
        int autoBottom = canvasY + canvasH - TL_BOX_MARGIN;
        int autoH = Math.max(0, autoBottom - y);

        var bars = collectBars();

        int w = USE_FIXED_SIZE ? BOX_W : autoW;
        int h = USE_FIXED_SIZE ? BOX_H_ONE_ROW : autoH;

        if (DEBUG_BOX) {
            ctx.fill(x, y, x + w, y + h, 0x402196F3);
            drawBorder(ctx, x, y, w, h, 0xFF2196F3);
        }

        ClientPartyHudData.Member m = ClientPartyHudData.getSelectedOrDefault();
        if (m == null) {
            String none = "Select a member";
            int tw = Math.round(tr.getWidth(none) * TEXT_SCALE);
            drawScaledText(ctx, tr, none, x + (w - tw)/2, y + (h - Math.round(tr.fontHeight * TEXT_SCALE))/2, 0xFFBEBEBE, TEXT_SCALE);
            infoBtnW = infoBtnH = 0;
            return;
        }

        if (!iconFiltered) {
            TextureManager tm = MinecraftClient.getInstance().getTextureManager();
            AbstractTexture t1 = tm.getTexture(INFO_BUTTON);
            if (t1 != null) t1.setFilter(false, false);
            iconFiltered = true;
        }

        int topY = y + PAD + 6;

        int minBarW = 50;
        for (Bar b : bars) {
            int wTxt = Math.round(tr.getWidth(b.valueText) * TEXT_SCALE);
            minBarW = Math.max(minBarW, wTxt + 6);
        }

        int centerGap = Math.max(10, Math.min(CENTER_GAP_DEFAULT, w - PAD * 2 - 2 * minBarW));
        int maxBarWEach = Math.max(0, (w - PAD * 2 - centerGap) / 2);
        int barW = Math.max(minBarW, maxBarWEach);

        int leftX  = x + PAD;
        int rightX = x + w - PAD - barW;

        if (bars.size() >= 1) {
            Bar b = bars.get(0);
            drawBar(ctx, leftX, topY, barW, BAR_H, b.pct, b.color);
            drawScaledText(ctx, tr, b.label, leftX, topY - 7, 0xFFFFFFFF, TEXT_SCALE);
            drawScaledText(ctx, tr, b.valueText, leftX + 3, topY + 2, 0xFFFFFFFF, TEXT_SCALE);
        }
        if (bars.size() >= 2) {
            Bar b = bars.get(1);
            drawBar(ctx, rightX, topY, barW, BAR_H, b.pct, b.color);
            drawRightScaledText(ctx, tr, b.label, rightX + barW, topY - 7, 0xFFFFFFFF, TEXT_SCALE);
            drawRightScaledText(ctx, tr, b.valueText, rightX + barW - 3, topY + 2, 0xFFFFFFFF, TEXT_SCALE);
        }
        if (bars.size() >= 3) {
            Bar b = bars.get(2);
            int y2 = topY + BAR_H + ROW_VGAP + 2;
            drawBar(ctx, leftX, y2, barW, BAR_H, b.pct, b.color);
            drawScaledText(ctx, tr, b.label, leftX, y2 - 7, 0xFFFFFFFF, TEXT_SCALE);
            drawScaledText(ctx, tr, b.valueText, leftX + 3, y2 + 2, 0xFFFFFFFF, TEXT_SCALE);
        }

        int barsBottom  = topY + BAR_H;
        int modelTop    = barsBottom + 2;
        int modelBottom = y + h - PAD;
        int availH      = Math.max(0, modelBottom - modelTop);

        infoBtnW = infoBtnH = 0;

        if (availH > 0) {
            int modelX    = x + w / 2;
            int modelSize = Math.max(10, Math.min(16, availH));

            PlayerEntity pe = getRenderablePlayer(m.uuid, m.name);
            if (pe != null) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(0.0F, 0.0F, 400.0F);
                RenderSystem.enableDepthTest();
                RenderSystem.disableCull();

                Vector3f translation = new Vector3f(0f, 0f, 0f);
                Quaternionf rotation  = new Quaternionf().rotationZ((float) Math.PI);
                Quaternionf bodyRot   = new Quaternionf();

                InventoryScreen.drawEntity(ctx, modelX, modelBottom, modelSize, translation, rotation, bodyRot, pe);

                RenderSystem.enableCull();
                RenderSystem.disableDepthTest();
                ctx.getMatrices().pop();
            }

            if (FabricLoader.getInstance().isModLoaded("showmeyourbuild")) {
                int idealX = modelX + modelSize / 2 + ICON_PAD;
                int btnX = Math.min(x + w - PAD - ICON_W, idealX);
                int btnY = modelBottom - modelSize + (modelSize - ICON_H) / 2;
                btnY = Math.max(y + PAD, Math.min(y + h - PAD - ICON_H, btnY));

                boolean hover = isMouseOverRect(btnX, btnY, ICON_W, ICON_H);
                float angle = 0f;
                if (hover) {
                    double t = (System.currentTimeMillis() % 800) / 800.0;
                    angle = (float) (Math.sin(t * Math.PI * 2.0) * 12.0);
                }

                var ma = ctx.getMatrices();
                ma.push();
                ma.translate(btnX + ICON_W / 2f, btnY + ICON_H / 2f, 0f);
                if (angle != 0f) ma.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
                ctx.drawTexture(INFO_BUTTON, -ICON_W / 2, -ICON_H / 2, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
                ma.pop();

                infoBtnX = btnX; infoBtnY = btnY; infoBtnW = ICON_W; infoBtnH = ICON_H;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                int canvasX, int canvasY, int canvasW, int canvasH, TextRenderer tr) {
        if (infoBtnW > 0 &&
                mouseX >= infoBtnX && mouseX < infoBtnX + infoBtnW &&
                mouseY >= infoBtnY && mouseY < infoBtnY + infoBtnH) {

            var member = ClientPartyHudData.getSelectedOrDefault();
            if (member != null && member.name != null && !member.name.isBlank()) {
                ClientPlayNetworking.send(new ShowBuildCompatPayloads.OpenBuildRequest(member.name));
            } else {
                var mc = MinecraftClient.getInstance();
                if (mc != null && mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal("No member selected."), false);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isMouseOverRect(int x, int y, int w, int h) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static final Map<UUID, OtherClientPlayerEntity> DUMMIES = new HashMap<>();

    private static void drawBar(DrawContext ctx, int x, int y, int w, int h, float pct, int fillARGB) {
        ctx.fill(x, y, x + w, y + h, BAR_BG);
        int iw = Math.max(0, Math.min(w - 2, Math.round((w - 2) * Math.max(0f, Math.min(1f, pct)))));
        ctx.fill(x + 1, y + 1, x + 1 + iw, y + h - 1, fillARGB);
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static void drawRightScaledText(DrawContext ctx, TextRenderer tr, String text, int rightX, int y, int color, float scale) {
        int w = Math.round(tr.getWidth(text) * scale);
        drawScaledText(ctx, tr, text, rightX - w, y, color, scale);
    }

    private static PlayerEntity getRenderablePlayer(UUID uuid, String fallbackName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return null;

        PlayerEntity live = world.getPlayerByUuid(uuid);
        if (live != null) return live;

        OtherClientPlayerEntity cached = DUMMIES.get(uuid);
        if (cached != null && cached.clientWorld == world) return cached;

        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        GameProfile profile = null;
        if (nh != null && nh.getPlayerListEntry(uuid) != null) {
            profile = nh.getPlayerListEntry(uuid).getProfile();
        }
        if (profile == null) profile = new GameProfile(uuid, fallbackName != null ? fallbackName : uuid.toString());

        OtherClientPlayerEntity dummy = new OtherClientPlayerEntity(world, profile);
        dummy.bodyYaw = 180f;
        DUMMIES.put(uuid, dummy);
        return dummy;
    }

    public static int getPreferredHeight() {
        return BOX_H_ONE_ROW;
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private record Bar(String label, String valueText, float pct, int color) {}

    private static List<Bar> collectBars() {
        var cfg = PartyMemberInfoClientConfig.get();
        boolean staminaAvail = FabricLoader.getInstance().isModLoaded("staminaattributes");
        boolean manaAvail = FabricLoader.getInstance().isModLoaded("manaattributes");
        boolean rpgmanaAvail = FabricLoader.getInstance().isModLoaded("rpgmana");

        ClientPartyHudData.Member m = ClientPartyHudData.getSelectedOrDefault();
        if (m == null) return List.of();

        List<Bar> out = new ArrayList<>();

        if (cfg.showHpBar) {
            float pct = m.maxHealth > 0 ? Math.max(0f, Math.min(1f, m.health / m.maxHealth)) : 0f;
            String txt = (m.health >= 0 && m.maxHealth > 0) ? (Math.round(m.health) + "/" + Math.round(m.maxHealth)) : "??";
            out.add(new Bar("HP", txt, pct, HP_COLOR));
        }
        if (cfg.showHungerBar) {
            float pct = Math.max(0f, Math.min(1f, m.hunger / 20f));
            String txt = (m.hunger >= 0) ? (m.hunger + "/20") : "??";
            out.add(new Bar("Hunger", txt, pct, HUNGER_COLOR));
        }
        if (cfg.showStaminaBar && staminaAvail) {
            var v = queryTracked(m.uuid, true);
            float pct = v.max > 0 ? Math.max(0f, Math.min(1f, v.now / v.max)) : 0f;
            String txt = (v.now >= 0 && v.max > 0) ? ((int)v.now + "/" + (int)v.max) : "??";
            out.add(new Bar("Stamina", txt, pct, STAMINA_COLOR));
        }
        if (cfg.showManaBar && manaAvail) {
            var v = queryTracked(m.uuid, false);
            float pct = v.max > 0 ? Math.max(0f, Math.min(1f, v.now / v.max)) : 0f;
            int nowI = v.now >= 0 ? (int)v.now : 0;
            int maxI = v.max > 0 ? (int)v.max : 0;
            String txt = (maxI > 0) ? (nowI + "/" + maxI) : "??";
            out.add(new Bar("Mana", txt, pct, MANA_COLOR));
        }

        if (cfg.showRpgManaBar && rpgmanaAvail) {
            var v = queryRpgMana(m.uuid);
            float pct = v.max > 0 ? Math.max(0f, Math.min(1f, v.now / v.max)) : 0f;
            int nowI = v.now >= 0 ? (int)v.now : 0;
            int maxI = v.max > 0 ? (int)v.max : 0;
            String txt = (maxI > 0) ? (nowI + "/" + maxI) : "??";
            out.add(new Bar("Mana (RPGMana)", txt, pct, RPG_MANA_COLOR));
        }

        if (out.size() > 3) out = out.subList(0, 3);
        return out;
    }
    private record Gauge(float now, float max) {}
    private static Gauge queryTracked(UUID uuid, boolean stamina) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new Gauge(-1, -1);
        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new Gauge(-1, -1);
        String getNow = stamina ? "staminaattributes$getStamina" : "manaattributes$getMana";
        String getMax = stamina ? "staminaattributes$getUnreservedStamina" : "manaattributes$getUnreservedMana";
        Float nowF = reflectFloat(e, getNow);
        Float maxF = reflectFloat(e, getMax);
        if (nowF == null || maxF == null) return new Gauge(-1, -1);
        float now = Math.max(0f, nowF);
        float max = Math.max(0f, maxF);
        return new Gauge(now, max);
    }

    private static Gauge queryRpgMana(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new Gauge(-1, -1);
        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new Gauge(-1, -1);
        Float nowF = reflectFloat(e, "getMana");
        Float maxF = reflectFloat(e, "getMaxMana");
        if (nowF == null || maxF == null) return new Gauge(-1, -1);
        float now = Math.max(0f, nowF);
        float max = Math.max(0f, maxF);
        return new Gauge(now, max);
    }


    private static Float reflectFloat(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object o = m.invoke(obj);
            if (o instanceof Float f) return f;
            if (o instanceof Number n) return n.floatValue();
        } catch (Throwable ignored) {
        }
        return null;
    }
}
