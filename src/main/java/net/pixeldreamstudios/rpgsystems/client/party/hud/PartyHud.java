package net.pixeldreamstudios.rpgsystems.client.party.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyStatusEffects;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;
import net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

@Environment(EnvType.CLIENT)
public final class PartyHud implements HudRenderCallback {
    private static final boolean DEBUG_BOXES = false;
    private static final boolean DEBUG_EFFECTS = false;

    private static final int HUD_OFFSET_X = -10;

    private static final int PANEL_W = 128;
    private static final int PANEL_H = 32;
    private static final int ROW_GAP  = 1;
    private static final float BG_SCALE = 0.75f;

    private static final int PLATE_W = 20;
    private static final int PLATE_INSET = 5;
    private static final int NAME_CHAR_MAX = 20;
    private static final int NAME_PAD_X = 2;
    private static final int NAME_PAD_Y = 1;
    private static final float TEXT_SCALE = 0.25f;

    private static final int PLATE_SHIFT_X = 1;
    private static final int PLATE_SHIFT_Y = -2;

    private static final float LVL_INSET_X = -0.3f;
    private static final int   LVL_INSET_Y = 3;
    private static final int   LVL_SLOT_W  = 14;
    private static final int   LVL_PAD_Y   = 1;
    private static final int   LVL_COLOR   = 0xFFFFFFFF;

    private static final int MODEL_LEFT_MARGIN = 15;
    private static final int MODEL_SIZE = 4;

    private static final int BAR_GAP_FROM_MODEL = 3;
    private static final int BAR_RIGHT_PAD = 6;
    private static final int BAR_END_SHORTEN = 5;
    private static final int BAR_H_BASE = 3;
    private static final int BAR_BG         = 0xFF3E3E3E;
    private static final int BAR_BORDER     = 0x00000000;
    private static final int BAR_HP_FILL    = 0xFF2ECC71;
    private static final int BAR_HUNGER_FILL= 0xFFE6C200;
    private static final int BAR_STAM_FILL  = 0xFFE6C200;
    private static final int BAR_MANA_FILL  = 0xFF3498DB;
    private static final int BAR_TEXT_COLOR = 0xFFFFFFFF;
    private static final int BAR_RPGMANA_FILL = 0xFF3498DB;
    private static final Identifier ARROW_UP    = Identifier.of("rpg-systems","textures/gui/party_hud/up.png");
    private static final Identifier ARROW_DOWN  = Identifier.of("rpg-systems","textures/gui/party_hud/down.png");
    private static final Identifier ARROW_LEFT  = Identifier.of("rpg-systems","textures/gui/party_hud/left.png");
    private static final Identifier ARROW_RIGHT = Identifier.of("rpg-systems","textures/gui/party_hud/right.png");
    private static final int ARROW_W = 11;
    private static final int ARROW_H = 11;
    private static final int ARROW_INSET = 3;

    private static final Identifier PARTY_BG = Identifier.of("rpg-systems", "textures/gui/party_hud.png");
    private static boolean bgFiltered = false;

    private static boolean staminaAvailableCached = FabricLoader.getInstance().isModLoaded("staminaattributes");
    private static boolean manaAvailableCached = FabricLoader.getInstance().isModLoaded("manaattributes");
    private static boolean rpgManaAvailableCached = FabricLoader.getInstance().isModLoaded("rpgmana"); // NEW

    private static final float EFFECT_ICON_SIZE = 3f;
    private static final float EFFECT_ICON_GAP  = 1f;
    private static final int   EFFECTS_PER_COLUMN = 3;
    private static final float POTION_RIGHT_OFFSET = -2f;
    private static final int   EFFECT_BASE_PX = 18;

    public static void init() {
        HudRenderCallback.EVENT.register(new PartyHud());

        PayloadTypeRegistry.playS2C().register(
                PartyStatusEffectsPayloads.MemberEffects.ID,
                PartyStatusEffectsPayloads.MemberEffects.CODEC
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PartyStatusEffectsPayloads.MemberEffects.ID,
                (payload, context) -> {
                    List<Identifier> ids = new ArrayList<>();
                    for (String s : payload.effectIds()) {
                        try { ids.add(Identifier.of(s)); } catch (Throwable ignored) {}
                    }
                    if (DEBUG_EFFECTS) {
                        log("recv effects for " + payload.memberId() + " -> size=" + ids.size() + " " + ids);
                    }
                    context.client().execute(() -> ClientPartyStatusEffects.update(payload.memberId(), ids));
                }
        );

        if (DEBUG_EFFECTS) log("PartyHud init: payload + receiver registered");
    }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        var cfg = PartyHudClientConfig.get();
        if (!cfg.hudEnabled) return;

        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden || mc.player == null) return;

        ensureBgNearest();

        List<ClientPartyHudData.Member> list = ClientPartyHudData.membersSortedExcludingSelf();

        final int x = 8 + HUD_OFFSET_X;
        final int yStart = 10;
        final int yStep = Math.round(PANEL_H * BG_SCALE) + ROW_GAP;

        if (list.isEmpty()) return;

        int y = yStart;
        for (var m : list) {
            if (DEBUG_EFFECTS) log("row anchor for " + m.name + " [" + m.uuid + "]: x=" + x + " y=" + y);
            drawRow(ctx, x, y, m.uuid, m.name, m.totalLevel, cfg);
            y += yStep;
        }
    }

    private void drawRow(DrawContext ctx, int x, int y, UUID uuid, String name, int level, PartyHudClientConfig cfg) {
        drawTextureScaled(ctx, PARTY_BG, x, y, PANEL_W, PANEL_H, 128, 32, BG_SCALE);

        final int scaledW = Math.round(PANEL_W * BG_SCALE);
        final int scaledH = Math.round(PANEL_H * BG_SCALE);
        var tr = MinecraftClient.getInstance().textRenderer;

        final int modelCenterX = x + MODEL_LEFT_MARGIN;
        final int modelBottomY = y + (scaledH / 2) + (MODEL_SIZE / 2) + 2;
        drawMemberModel(ctx, modelCenterX, modelBottomY, MODEL_SIZE, uuid, name);

        final float lvlSlotXf = x + LVL_INSET_X;
        final float lvlSlotYf = y + LVL_INSET_Y;
        final int   lvlSlotW  = LVL_SLOT_W;
        final int   lvlSlotH  = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE)) + LVL_PAD_Y * 2;

        String lvlText = (level >= 0) ? ("" + level) : "?";
        int lvlTextW = Math.round(tr.getWidth(lvlText) * TEXT_SCALE);
        int lvlTextH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
        float lvlTextX = lvlSlotXf + (lvlSlotW - lvlTextW) / 2f;
        float lvlTextY = lvlSlotYf + (lvlSlotH - lvlTextH) / 2f;
        drawScaledTextF(ctx, tr, lvlText, lvlTextX, lvlTextY, LVL_COLOR, TEXT_SCALE);

        final int slotW = PLATE_W;
        final int slotH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE)) + NAME_PAD_Y * 2;
        final int slotX = x + scaledW - PLATE_INSET - slotW + PLATE_SHIFT_X;
        final int slotY = y + PLATE_INSET + PLATE_SHIFT_Y;

        String display = (name == null ? "Unknown" : name);
        if (display.length() > NAME_CHAR_MAX) display = display.substring(0, NAME_CHAR_MAX);
        int innerMaxPx = Math.max(0, slotW - NAME_PAD_X * 2);
        display = ellipsizeScaled(tr, display, innerMaxPx, TEXT_SCALE);

        int textW = Math.round(tr.getWidth(display) * TEXT_SCALE);
        int textH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
        int textX = slotX + (slotW - textW) / 2;
        int textY = slotY + (slotH - textH) / 2;
        drawScaledText(ctx, tr, display, textX, textY, 0xFFFFFFFF, TEXT_SCALE);

        final int modelRight = modelCenterX + MODEL_SIZE / 2;
        final int barX = modelRight + BAR_GAP_FROM_MODEL;
        final int barRight = x + scaledW - BAR_RIGHT_PAD - BAR_END_SHORTEN;
        final int barW = Math.max(0, barRight - barX);
        final int barH = Math.max(1, Math.round(BAR_H_BASE * BG_SCALE));

        List<RenderBar> bars = collectBars(uuid, cfg);
        if (bars.size() > 3) bars = bars.subList(0, 3);

        if (barW > 0 && !bars.isEmpty()) {
            int totalH = bars.size() * (barH + 1) - 1;
            int topY = y + (scaledH - totalH) / 2 + 1;

            for (int i = 0; i < bars.size(); i++) {
                RenderBar b = bars.get(i);
                int by = topY + i * (barH + 1);
                drawBarPretty(ctx, barX, by, barW, barH, b.pct, BAR_BG, BAR_BORDER, b.color);
                String txt = b.text;
                int tW = Math.round(tr.getWidth(txt) * TEXT_SCALE);
                int tH = Math.max(1, Math.round(tr.fontHeight * TEXT_SCALE));
                int tX = barX + barW - 1 - tW;
                int tY = by + (barH - tH) / 2;
                drawScaledText(ctx, tr, txt, tX, tY, BAR_TEXT_COLOR, TEXT_SCALE);
                if (DEBUG_BOXES) drawBorder(ctx, barX, by, barW, barH, 0x80FF00FF);
            }
        }

        if (cfg.showArrows) {
            Identifier arrow = pickArrow(uuid);
            if (arrow != null) {
                int aw = Math.round(ARROW_W * BG_SCALE);
                int ah = Math.round(ARROW_H * BG_SCALE);
                int ax = x + scaledW - ARROW_INSET - aw;
                int ay = y + scaledH - ARROW_INSET - ah;
                drawTextureScaled(ctx, arrow, ax - 1, ay + 3, ARROW_W, ARROW_H, ARROW_W, ARROW_H, BG_SCALE);
                if (DEBUG_BOXES) drawBorder(ctx, ax, ay, aw, ah, 0x80FFFF00);
            }
        }

        List<Identifier> theirEffects = ClientPartyStatusEffects.get(uuid);
        if (DEBUG_EFFECTS) {
            log("effects for " + name + " [" + uuid + "]: count=" + (theirEffects == null ? 0 : theirEffects.size()) + " " + theirEffects);
        }
        if (theirEffects != null && !theirEffects.isEmpty()) {
            float anchorX = x + scaledW + POTION_RIGHT_OFFSET;
            float boxH = scaledH;
            if (DEBUG_EFFECTS) log("effect anchor for " + name + ": anchorX=" + anchorX + " topY=" + y + " boxH=" + boxH);
            drawMemberEffectIcons(ctx, anchorX, y, boxH, theirEffects);
        }

        if (DEBUG_BOXES) {
            drawBorder(ctx, x, y, scaledW, Math.round(PANEL_H * BG_SCALE), 0x8040FFFF);
            drawBorder(ctx, modelCenterX - MODEL_SIZE / 2, modelBottomY - MODEL_SIZE, MODEL_SIZE, MODEL_SIZE, 0x80FF4040);
        }
    }

    private Identifier pickArrow(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return null;

        PlayerEntity self = mc.player;
        PlayerEntity target = mc.world.getPlayerByUuid(uuid);
        if (target == null) return null;

        double dx = target.getX() - self.getX();
        double dz = target.getZ() - self.getZ();

        double yawRad = Math.toRadians(self.getYaw());
        double forwardDot = (-Math.sin(yawRad)) * dx + (Math.cos(yawRad)) * dz;
        double rightDot   = ( Math.cos(yawRad)) * dx + (Math.sin(yawRad)) * dz;

        double af = Math.abs(forwardDot);
        double ar = Math.abs(rightDot);

        if (af >= ar) {
            return forwardDot >= 0 ? ARROW_UP : ARROW_DOWN;
        } else {
            return rightDot >= 0 ? ARROW_LEFT : ARROW_RIGHT;
        }
    }

    private void drawMemberModel(DrawContext ctx, int centerX, int bottomY, int size, UUID uuid, String fallbackName) {
        PlayerEntity pe = getRenderablePlayer(uuid, fallbackName);
        if (pe == null) return;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0.0F, 0.0F, 400.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        Vector3f translation = new Vector3f(0f, 0f, 0f);
        Quaternionf rotation  = new Quaternionf().rotationZ((float) Math.PI);
        Quaternionf bodyRot   = new Quaternionf();
        InventoryScreen.drawEntity(ctx, centerX, bottomY, size, translation, rotation, bodyRot, pe);

        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        ctx.getMatrices().pop();
    }

    private static final Map<UUID, OtherClientPlayerEntity> DUMMIES = new HashMap<>();

    private static PlayerEntity getRenderablePlayer(UUID uuid, String fallbackName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return null;
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

    private static void drawTextureScaled(DrawContext ctx, Identifier tex, int x, int y,
                                          int w, int h, int texW, int texH, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawTexture(tex, 0, 0, 0, 0, w, h, texW, texH);
        m.pop();
    }

    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static void drawScaledTextF(DrawContext ctx, TextRenderer tr, String text, float x, float y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private static String ellipsizeScaled(TextRenderer tr, String s, int maxPx, float scale) {
        if (Math.round(tr.getWidth(s) * scale) <= maxPx) return s;
        String dots = "...";
        int dw = Math.round(tr.getWidth(dots) * scale);
        StringBuilder b = new StringBuilder(s);
        while (b.length() > 0 && Math.round(tr.getWidth(b.toString()) * scale) + dw > maxPx) b.deleteCharAt(b.length() - 1);
        return b + dots;
    }

    private static void ensureBgNearest() {
        if (bgFiltered) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();

        AbstractTexture tex = tm.getTexture(PARTY_BG);
        if (tex != null) tex.setFilter(false, false);

        AbstractTexture a1 = tm.getTexture(ARROW_UP);    if (a1 != null) a1.setFilter(false, false);
        AbstractTexture a2 = tm.getTexture(ARROW_DOWN);  if (a2 != null) a2.setFilter(false, false);
        AbstractTexture a3 = tm.getTexture(ARROW_LEFT);  if (a3 != null) a3.setFilter(false, false);
        AbstractTexture a4 = tm.getTexture(ARROW_RIGHT); if (a4 != null) a4.setFilter(false, false);

        bgFiltered = true;
    }

    private static final class RenderBar {
        final float pct;
        final String text;
        final int color;
        RenderBar(float pct, String text, int color) {
            this.pct = Math.max(0f, Math.min(1f, pct));
            this.text = text;
            this.color = color;
        }
    }

    private static List<RenderBar> collectBars(UUID uuid, PartyHudClientConfig cfg) {
        List<RenderBar> out = new ArrayList<>();
        ClientPartyHudData.Member mem = ClientPartyHudData.getByUuid(uuid);

        if (cfg.showHpBar) {
            float pct = 0f;
            int now = -1, max = -1;
            if (mem != null) {
                pct = mem.maxHealth > 0 ? Math.max(0f, Math.min(1f, mem.health / mem.maxHealth)) : 0f;
                now = Math.round(mem.health);
                max = Math.round(mem.maxHealth);
            }
            String txt = (now >= 0 && max >= 0) ? (now + " / " + max) : "?? / ??";
            out.add(new RenderBar(pct, txt, BAR_HP_FILL));
        }

        if (cfg.showHungerBar) {
            float pct = 0f;
            int now = -1;
            if (mem != null && mem.hunger >= 0) {
                pct = Math.max(0f, Math.min(1f, mem.hunger / 20f));
                now = mem.hunger;
            }
            String txt = (now >= 0) ? (now + " / 20") : "?? / 20";
            out.add(new RenderBar(pct, txt, BAR_HUNGER_FILL));
        }

        if (cfg.showStaminaBar && staminaAvailableCached) {
            StackedValue v = queryTracked(uuid, true);
            float pct = v.max > 0 ? v.now / v.max : 0f;
            String txt = (v.now >= 0 && v.max > 0) ? ((int)v.now + " / " + (int)v.max) : "?? / ??";
            out.add(new RenderBar(pct, txt, BAR_STAM_FILL));
        }

        if (cfg.showManaBar && manaAvailableCached) {
            StackedValue v = queryTracked(uuid, false);
            float pct = v.max > 0 ? Math.max(0f, Math.min(1f, v.now / v.max)) : 0f;
            int nowI = v.now >= 0 ? (int)v.now : 0;     // now already clamped, but harmless
            int maxI = v.max > 0 ? (int)v.max : 0;
            String txt = (maxI > 0) ? (nowI + " / " + maxI) : "?? / ??";
            out.add(new RenderBar(pct, txt, BAR_MANA_FILL));
        }

        if (cfg.showRpgManaBar && rpgManaAvailableCached) {
            StackedValue v = queryRpgMana(uuid);
            float pct = v.max > 0 ? Math.max(0f, Math.min(1f, v.now / v.max)) : 0f;
            int nowI = v.now >= 0 ? (int)v.now : 0;
            int maxI = v.max > 0 ? (int)v.max : 0;
            String txt = (maxI > 0) ? (nowI + " / " + maxI) : "?? / ??";
            out.add(new RenderBar(pct, txt, BAR_RPGMANA_FILL));
        }
        return out;
    }

    private static final class StackedValue {
        final float now;
        final float max;
        StackedValue(float now, float max) { this.now = now; this.max = max; }
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

    private static void drawBarPretty(DrawContext ctx, int x, int y, int w, int h, float pct,
                                      int bgARGB, int borderARGB, int fillARGB) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, bgARGB);
        int iw = Math.max(0, Math.round(w * Math.max(0f, Math.min(1f, pct))));
        if (iw > 0) ctx.fill(x, y, x + iw, y + h, fillARGB);
    }

    private void drawMemberEffectIcons(DrawContext ctx, float anchorX, float topY, float boxHeight, List<Identifier> effectIds) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        int total = effectIds.size();
        if (total <= 0) return;

        int z = 400;
        int index = 0;
        int col = 0;

        while (index < total) {
            int remaining = total - index;
            int thisColCount = Math.min(EFFECTS_PER_COLUMN, remaining);

            float colHeight = thisColCount * EFFECT_ICON_SIZE + (thisColCount - 1) * EFFECT_ICON_GAP;
            float startY = topY + (boxHeight - colHeight) / 2f;
            float startX = anchorX + col * (EFFECT_ICON_SIZE + EFFECT_ICON_GAP);

            for (int r = 0; r < thisColCount; r++) {
                Identifier id = effectIds.get(index++);
                if (id == null) continue;

                StatusEffect effect = Registries.STATUS_EFFECT.get(id);
                if (effect == null) continue;

                RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
                if (entry == null) continue;

                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(entry);
                if (sprite == null) continue;

                float x = startX;
                float y = startY + r * (EFFECT_ICON_SIZE + EFFECT_ICON_GAP);

                drawSpriteScaledF(ctx, x, y, z, EFFECT_ICON_SIZE, sprite);

                if (DEBUG_BOXES) {
                    drawBorder(ctx, Math.round(x), Math.round(y), Math.round(EFFECT_ICON_SIZE), Math.round(EFFECT_ICON_SIZE), 0x80FF00FF);
                }
            }

            col++;
        }
    }

    private static void drawSpriteScaledF(DrawContext ctx, float x, float y, int z, float size, Sprite sprite) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        float s = size / EFFECT_BASE_PX;
        m.scale(s, s, 1f);
        ctx.drawSprite(0, 0, z, EFFECT_BASE_PX, EFFECT_BASE_PX, sprite);
        m.pop();
    }
    private static StackedValue queryTracked(UUID uuid, boolean stamina) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new StackedValue(-1, -1);
        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new StackedValue(-1, -1);
        String getNow = stamina ? "staminaattributes$getStamina" : "manaattributes$getMana";
        String getMax = stamina ? "staminaattributes$getUnreservedStamina" : "manaattributes$getUnreservedMana";
        Float nowF = reflectFloat(e, getNow);
        Float maxF = reflectFloat(e, getMax);
        if (nowF == null || maxF == null) return new StackedValue(-1, -1);
        float now = Math.max(0f, nowF);  // clamp negatives to zero
        float max = Math.max(0f, maxF);
        return new StackedValue(now, max);
    }

    private static StackedValue queryRpgMana(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new StackedValue(-1, -1);
        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new StackedValue(-1, -1);
        Float nowF = reflectFloat(e, "getMana");
        Float maxF = reflectFloat(e, "getMaxMana");
        if (nowF == null || maxF == null) return new StackedValue(-1, -1);
        float now = Math.max(0f, nowF);  // clamp negatives to zero
        float max = Math.max(0f, maxF);
        return new StackedValue(now, max);
    }

    private static void log(String msg) {
        try {
            RPGSystems.LOGGER.info("[PartyHud] " + msg);
        } catch (Throwable t) {
            System.out.println("[PartyHud] " + msg);
        }
    }
}
