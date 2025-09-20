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
import net.pixeldreamstudios.rpgsystems.compat.RpgManaCompat;
import net.pixeldreamstudios.rpgsystems.compat.TrbAttributesCompat;
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
    private static final int VALUE_TEXT_COLOR        = 0xFFFFFFFF;
    private static final int VALUE_TEXT_HP_ABS_COLOR = 0xFFEEC72A;
    private static final Identifier TEX_BAR_OUTLINE = Identifier.of("rpg-systems", "textures/gui/party_hud/bar_outline_memberinfo.png");
    private static final Identifier TEX_HP_FILL      = Identifier.of("rpg-systems", "textures/gui/party_hud/health_bar_memberinfo.png");
    private static final Identifier TEX_ABSORB_FILL  = Identifier.of("rpg-systems", "textures/gui/party_hud/absorption_bar_memberinfo.png");
    private static final Identifier TEX_MANA_FILL    = Identifier.of("rpg-systems", "textures/gui/party_hud/mana_bar_memberinfo.png");
    private static final Identifier TEX_HUNGER_FILL  = Identifier.of("rpg-systems", "textures/gui/party_hud/hunger_bar_memberinfo.png");
    private static boolean barTexturesFiltered = false;
    private static final int TEX_SRC_W = 108;
    private static final int TEX_SRC_H = 8;

    private static void ensureBarTexturesNearest() {
        if (barTexturesFiltered) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();
        AbstractTexture o = tm.getTexture(TEX_BAR_OUTLINE); if (o != null) o.setFilter(false, false);
        AbstractTexture h = tm.getTexture(TEX_HP_FILL);      if (h != null) h.setFilter(false, false);
        AbstractTexture a = tm.getTexture(TEX_ABSORB_FILL);  if (a != null) a.setFilter(false, false);
        AbstractTexture m = tm.getTexture(TEX_MANA_FILL);    if (m != null) m.setFilter(false, false);
        AbstractTexture g = tm.getTexture(TEX_HUNGER_FILL);  if (g != null) g.setFilter(false, false);
        barTexturesFiltered = true;
    }
    private static int valueColor(Bar b) {
        if (b.kind == Kind.HP && b.extraPct > 0.001f) return VALUE_TEXT_HP_ABS_COLOR;
        return VALUE_TEXT_COLOR;
    }
    private static void drawValueTextLeft(DrawContext ctx, TextRenderer tr, Bar b, int x, int y, float scale) {
        String cur = b.valueText;
        String rest = "";
        if (b.kind == Kind.HP) {
            String[] parts = cur.split("/", 2);
            cur  = parts.length > 0 ? parts[0] : cur;
            rest = parts.length > 1 ? "/" + parts[1] : "";
        }
        int curCol  = (b.kind == Kind.HP && b.extraPct > 0.001f) ? VALUE_TEXT_HP_ABS_COLOR : VALUE_TEXT_COLOR;
        int restCol = VALUE_TEXT_COLOR;

        drawScaledText(ctx, tr, cur, x, y, curCol, scale);
        int curW = Math.round(tr.getWidth(cur) * scale);
        if (!rest.isEmpty()) drawScaledText(ctx, tr, rest, x + curW, y, restCol, scale);
    }

    private static void drawValueTextRight(DrawContext ctx, TextRenderer tr, Bar b, int rightX, int y, float scale) {
        String cur = b.valueText;
        String rest = "";
        if (b.kind == Kind.HP) {
            String[] parts = cur.split("/", 2);
            cur  = parts.length > 0 ? parts[0] : cur;
            rest = parts.length > 1 ? "/" + parts[1] : "";
        }
        int curCol  = (b.kind == Kind.HP && b.extraPct > 0.001f) ? VALUE_TEXT_HP_ABS_COLOR : VALUE_TEXT_COLOR;
        int restCol = VALUE_TEXT_COLOR;

        int curW  = Math.round(tr.getWidth(cur) * scale);
        int restW = Math.round(tr.getWidth(rest) * scale);
        int startX = rightX - (curW + restW);

        drawScaledText(ctx, tr, cur,  startX,           y, curCol,  scale);
        if (!rest.isEmpty()) drawScaledText(ctx, tr, rest, startX + curW, y, restCol, scale);
    }
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
        ensureBarTexturesNearest();

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
            drawBarSmart(ctx, leftX, topY, barW, BAR_H, b);
            drawScaledText(ctx, tr, b.label, leftX, topY - 7, 0xFFFFFFFF, TEXT_SCALE);
            drawValueTextLeft(ctx, tr, b, leftX + 3, topY + 2, TEXT_SCALE);
        }
        if (bars.size() >= 2) {
            Bar b = bars.get(1);
            drawBarSmart(ctx, rightX, topY, barW, BAR_H, b);
            drawRightScaledText(ctx, tr, b.label, rightX + barW, topY - 7, 0xFFFFFFFF, TEXT_SCALE);
            drawValueTextRight(ctx, tr, b, rightX + barW - 3, topY + 2, TEXT_SCALE);
        }
        if (bars.size() >= 3) {
            Bar b = bars.get(2);
            int y2 = topY + BAR_H + ROW_VGAP + 2;
            drawBarSmart(ctx, leftX, y2, barW, BAR_H, b);
            drawScaledText(ctx, tr, b.label, leftX, y2 - 7, 0xFFFFFFFF, TEXT_SCALE);
            drawValueTextLeft(ctx, tr, b, leftX + 3, y2 + 2, TEXT_SCALE);
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

    private static void drawBarFlat(DrawContext ctx, int x, int y, int w, int h, float pct, int fillARGB) {
        ctx.fill(x, y, x + w, y + h, BAR_BG);
        int iw = Math.max(0, Math.min(w - 2, Math.round((w - 2) * Math.max(0f, Math.min(1f, pct)))));
        ctx.fill(x + 1, y + 1, x + 1 + iw, y + h - 1, fillARGB);
    }

    private static void drawTexturedWhole(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) TEX_SRC_W, h / (float) TEX_SRC_H, 1f);
        ctx.drawTexture(tex, 0, 0, 0, 0, TEX_SRC_W, TEX_SRC_H, TEX_SRC_W, TEX_SRC_H);
        m.pop();
    }

    private static void drawTexturedSegment(DrawContext ctx, Identifier tex, int x, int y, int w, int h, float startPct, float endPct) {
        float s = Math.max(0f, Math.min(1f, startPct));
        float e = Math.max(0f, Math.min(1f, endPct));
        if (e <= s) return;

        int srcU = Math.round(TEX_SRC_W * s);
        int srcW = Math.max(0, Math.round(TEX_SRC_W * (e - s)));

        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) TEX_SRC_W, h / (float) TEX_SRC_H, 1f);
        ctx.drawTexture(tex, srcU, 0, srcU, 0, srcW, TEX_SRC_H, TEX_SRC_W, TEX_SRC_H);
        m.pop();
    }

    private static void drawBarSmart(DrawContext ctx, int x, int y, int w, int h, Bar b) {
        switch (b.kind) {
            case HP -> {
                float hp  = clamp01(b.primaryPct);
                float abs = clamp01(b.extraPct);

                drawTexturedSegment(ctx, TEX_HP_FILL, x, y, w, h, 0f, hp);

                if (abs > 0f) {
                    drawTexturedSegmentWithAlpha(ctx, TEX_ABSORB_FILL, x, y, w, h, 0f, abs, 0.75f);
                }

                drawTexturedWhole(ctx, TEX_BAR_OUTLINE, x, y, w, h);
            }
            case MANA, RPGMANA -> {
                drawTexturedSegment(ctx, TEX_MANA_FILL, x, y, w, h, 0f, clamp01(b.primaryPct));
                drawTexturedWhole(ctx, TEX_BAR_OUTLINE, x, y, w, h);
            }
            case HUNGER -> {
                drawTexturedSegment(ctx, TEX_HUNGER_FILL, x, y, w, h, 0f, clamp01(b.primaryPct));
                drawTexturedWhole(ctx, TEX_BAR_OUTLINE, x, y, w, h);
            }
            case STAMINA -> {
                drawBarFlat(ctx, x, y, w, h, clamp01(b.primaryPct), b.color);
            }
        }
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

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

    private enum Kind { HP, HUNGER, STAMINA, MANA, RPGMANA }

    private record Bar(String label, String valueText, float primaryPct, float extraPct, int color, Kind kind) {}

    private static List<Bar> collectBars() {
        var cfg = PartyMemberInfoClientConfig.get();
        boolean staminaAvail = FabricLoader.getInstance().isModLoaded("staminaattributes");
        boolean manaAvail = FabricLoader.getInstance().isModLoaded("manaattributes");
        boolean rpgmanaAvail = FabricLoader.getInstance().isModLoaded("rpgmana");

        ClientPartyHudData.Member m = ClientPartyHudData.getSelectedOrDefault();
        if (m == null) return List.of();

        List<Bar> out = new ArrayList<>();

        if (cfg.showHpBar) {
            float hpPct = m.maxHealth > 0 ? clamp01(m.health / m.maxHealth) : 0f;
            float absPct = m.maxHealth > 0 ? clamp01(m.absorption / m.maxHealth) : 0f;
            int nowNum = Math.round(m.health + m.absorption);
            int maxNum = Math.round(m.maxHealth);
            String txt = (m.maxHealth > 0) ? (nowNum + "/" + maxNum) : "??";
            out.add(new Bar("HP", txt, hpPct, absPct, HP_COLOR, Kind.HP));
        }

        if (cfg.showHungerBar) {
            float pct = clamp01(m.hunger / 20f);
            String txt = (m.hunger >= 0) ? (m.hunger + "/20") : "??";
            out.add(new Bar("Hunger", txt, pct, 0f, HUNGER_COLOR, Kind.HUNGER));
        }

        if (cfg.showStaminaBar && staminaAvail) {
            float now = -1f, max = -1f;
            if (m.staminaMax > 0f) {
                now = m.staminaNow; max = m.staminaMax;
            } else {
                var v = queryTracked(m.uuid, true);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? clamp01(now / max) : 0f;
            String txt = (now >= 0 && max > 0) ? ((int)now + "/" + (int)max) : "??";
            out.add(new Bar("Stamina", txt, pct, 0f, STAMINA_COLOR, Kind.STAMINA));
        }

        if (cfg.showManaBar && manaAvail) {
            float now = -1f, max = -1f;
            if (m.manaMax > 0f) {
                now = m.manaNow; max = m.manaMax;
            } else {
                var v = queryTracked(m.uuid, false);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? clamp01(now / max) : 0f;
            String txt = (max > 0) ? ((int)now + "/" + (int)max) : "??";
            out.add(new Bar("Mana", txt, pct, 0f, MANA_COLOR, Kind.MANA));
        }

        if (cfg.showRpgManaBar && rpgmanaAvail) {
            float now = -1f, max = -1f;
            if (m.rpgManaMax > 0f) {
                now = m.rpgManaNow; max = m.rpgManaMax;
            } else {
                var v = queryRpgMana(m.uuid);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? clamp01(now / max) : 0f;
            String txt = (max > 0) ? ((int)now + "/" + (int)max) : "??";
            out.add(new Bar("Mana", txt, pct, 0f, RPG_MANA_COLOR, Kind.RPGMANA));
        }

        if (out.size() > 3) out = out.subList(0, 3);
        return out;
    }


    private record Gauge(float now, float max) {}
    private static Gauge queryTracked(UUID uuid, boolean stamina) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new Gauge(-1, -1);

        if (stamina && !FabricLoader.getInstance().isModLoaded("staminaattributes")) return new Gauge(-1, -1);
        if (!stamina && !FabricLoader.getInstance().isModLoaded("manaattributes"))   return new Gauge(-1, -1);

        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new Gauge(-1, -1);

        if (stamina) {
            float[] pair = TrbAttributesCompat.readStamina(e);
            return new Gauge(pair[0], pair[1]);
        } else {
            float[] pair = TrbAttributesCompat.readMana(e);
            return new Gauge(pair[0], pair[1]);
        }
    }
    private static void drawTexturedSegmentWithAlpha(DrawContext ctx, Identifier tex,
                                                     int x, int y, int w, int h,
                                                     float startPct, float endPct, float alpha) {
        float s = Math.max(0f, Math.min(1f, startPct));
        float e = Math.max(0f, Math.min(1f, endPct));
        if (e <= s) return;

        int srcU = Math.round(TEX_SRC_W * s);
        int srcW = Math.max(0, Math.round(TEX_SRC_W * (e - s)));

        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) TEX_SRC_W, h / (float) TEX_SRC_H, 1f);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
        ctx.drawTexture(tex, srcU, 0, srcU, 0, srcW, TEX_SRC_H, TEX_SRC_W, TEX_SRC_H);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        m.pop();
    }
    private static Gauge queryRpgMana(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new Gauge(-1, -1);
        if (!FabricLoader.getInstance().isModLoaded("rpgmana")) return new Gauge(-1, -1);

        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new Gauge(-1, -1);

        float[] pair = RpgManaCompat.readMana(e);
        return new Gauge(pair[0], pair[1]);
    }
}
