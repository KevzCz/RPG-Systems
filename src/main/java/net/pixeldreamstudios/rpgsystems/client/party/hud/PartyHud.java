package net.pixeldreamstudios.rpgsystems.client.party.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyPins;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyStatusEffects;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;
import net.pixeldreamstudios.rpgsystems.compat.RpgManaCompat;
import net.pixeldreamstudios.rpgsystems.compat.TrbAttributesCompat;
import net.pixeldreamstudios.rpgsystems.network.party.PartyStatusEffectsPayloads;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Environment(EnvType.CLIENT)
public final class PartyHud implements HudRenderCallback {
    public static final Identifier PARTY_BG = Identifier.of("rpg-systems", "textures/gui/party_hud.png");
    public static final Identifier PARTY_BG_LEADER = Identifier.of("rpg-systems", "textures/gui/party_hud_leader.png");
    public static final Identifier ARROW_UP    = Identifier.of("rpg-systems","textures/gui/party_hud/up.png");
    public static final Identifier ARROW_DOWN  = Identifier.of("rpg-systems","textures/gui/party_hud/down.png");
    public static final Identifier ARROW_LEFT  = Identifier.of("rpg-systems","textures/gui/party_hud/left.png");
    public static final Identifier ARROW_RIGHT = Identifier.of("rpg-systems","textures/gui/party_hud/right.png");
    public static final Identifier LEADER_ICON = Identifier.of("rpg-systems","textures/gui/party_hud/party_leader.png");
    public static final int ARROW_W = 11;
    public static final int ARROW_H = 11;
    public static final boolean DEBUG_FORCE_DUMMY = false;
    public static final int DEBUG_DUMMY_COUNT = 10;

    public static final Identifier TEX_BAR_OUTLINE_HUD = Identifier.of("rpg-systems","textures/gui/party_hud/bar_outline_hud.png");
    public static final Identifier TEX_HP_FILL_HUD     = Identifier.of("rpg-systems","textures/gui/party_hud/health_bar_hud.png");
    public static final Identifier TEX_ABSORB_FILL_HUD = Identifier.of("rpg-systems","textures/gui/party_hud/absorption_bar_hud.png");
    public static final Identifier TEX_MANA_FILL_HUD   = Identifier.of("rpg-systems","textures/gui/party_hud/mana_bar_hud.png");
    public static final Identifier TEX_HUNGER_FILL_HUD = Identifier.of("rpg-systems","textures/gui/party_hud/hunger_bar_hud.png");
    public static final int HUD_TEX_SRC_W = 108;
    public static final int HUD_TEX_SRC_H = 8;

    private static boolean bgFiltered = false;
    private static boolean hudBarTexturesFiltered = false;

    private static final boolean staminaAvailableCached = FabricLoader.getInstance().isModLoaded("staminaattributes");
    private static final boolean manaAvailableCached    = FabricLoader.getInstance().isModLoaded("manaattributes");
    private static final boolean rpgManaAvailableCached = FabricLoader.getInstance().isModLoaded("rpgmana");

    private static final Map<UUID, OtherClientPlayerEntity> DUMMIES = new HashMap<>();

    private static final PartyHudRenderer ORIGINAL = new OriginalHud();
    private static final PartyHudRenderer SIMPLE   = new SimpleHud();
    public static final Identifier SIMPLE_OUTLINE =
            Identifier.of("rpg-systems", "textures/gui/party_hud/party_hud_outline_simple.png");

    public static final int SIMPLE_OUTLINE_TEX_W = 92;
    public static final int SIMPLE_OUTLINE_TEX_H = 18;
    public enum Kind { HP, HUNGER, STAMINA, MANA, RPGMANA }

    public static final class RenderBar {
        public final Kind kind;
        public final float pct;
        public final float extraPct;
        public final String text;
        public final int color;
        public RenderBar(Kind kind, float pct, float extraPct, String text, int color) {
            this.kind = kind;
            this.pct = clamp01(pct);
            this.extraPct = clamp01(extraPct);
            this.text = text;
            this.color = color;
        }
    }

    public static void init() {
        HudRenderCallback.EVENT.register(new PartyHud());
        ClientPlayNetworking.registerGlobalReceiver(
                PartyStatusEffectsPayloads.MemberEffects.ID,
                (payload, context) -> {
                    List<Identifier> ids = new ArrayList<>();
                    for (String s : payload.effectIds()) {
                        try { ids.add(Identifier.of(s)); } catch (Throwable ignored) {}
                    }
                    context.client().execute(() -> ClientPartyStatusEffects.update(payload.memberId(), ids));
                }
        );
    }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        var cfg = PartyHudClientConfig.get();
        if (!cfg.hudEnabled) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options.hudHidden || mc.player == null) return;

        ensureBgNearest();
        ensureHudBarTexturesNearest();

        PartyHudRenderer renderer = (cfg.hudStyle == PartyHudClientConfig.HudStyle.SIMPLE) ? SIMPLE : ORIGINAL;
        renderer.render(ctx, tickCounter, cfg);
    }

    public static void ensureBgNearest() {
        if (bgFiltered) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();
        AbstractTexture t = tm.getTexture(PARTY_BG);         if (t  != null) t.setFilter(false, false);
        AbstractTexture tL = tm.getTexture(PARTY_BG_LEADER); if (tL != null) tL.setFilter(false, false);
        AbstractTexture a1 = tm.getTexture(ARROW_UP);    if (a1 != null) a1.setFilter(false, false);
        AbstractTexture a2 = tm.getTexture(ARROW_DOWN);  if (a2 != null) a2.setFilter(false, false);
        AbstractTexture a3 = tm.getTexture(ARROW_LEFT);  if (a3 != null) a3.setFilter(false, false);
        AbstractTexture a4 = tm.getTexture(ARROW_RIGHT); if (a4 != null) a4.setFilter(false, false);
        AbstractTexture ld = tm.getTexture(LEADER_ICON); if (ld != null) ld.setFilter(false, false);
        AbstractTexture so = tm.getTexture(SIMPLE_OUTLINE); if (so != null) so.setFilter(false, false);
        bgFiltered = true;
    }

    public static void ensureHudBarTexturesNearest() {
        if (hudBarTexturesFiltered) return;
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();
        AbstractTexture o = tm.getTexture(TEX_BAR_OUTLINE_HUD); if (o != null) o.setFilter(false, false);
        AbstractTexture h = tm.getTexture(TEX_HP_FILL_HUD);     if (h != null) h.setFilter(false, false);
        AbstractTexture a = tm.getTexture(TEX_ABSORB_FILL_HUD); if (a != null) a.setFilter(false, false);
        AbstractTexture m = tm.getTexture(TEX_MANA_FILL_HUD);   if (m != null) m.setFilter(false, false);
        AbstractTexture g = tm.getTexture(TEX_HUNGER_FILL_HUD); if (g != null) g.setFilter(false, false);
        hudBarTexturesFiltered = true;
    }

    public static List<ClientPartyHudData.Member> partyMembers() {
        var cfg = PartyHudClientConfig.get();

        List<ClientPartyHudData.Member> list;
        UUID leaderForSort;

        boolean useDummy = DEBUG_FORCE_DUMMY && ClientPartyHudData.partyId != null;

        if (useDummy) {
            int count = DEBUG_DUMMY_COUNT;
            list = new ArrayList<>(count);

            UUID leaderDummy = UUID.nameUUIDFromBytes(
                    ("rpgsystems:dummy:leader").getBytes(StandardCharsets.UTF_8));
            leaderForSort = leaderDummy;

            for (int i = 0; i < count; i++) {
                boolean isLeader = (i == 6);
                UUID uuid = isLeader
                        ? leaderDummy
                        : UUID.nameUUIDFromBytes(("rpgsystems:dummy:" + i).getBytes(StandardCharsets.UTF_8));
                String name = isLeader ? "Leader_Dummy" : ("Dummy_" + (i + 1));

                ClientPartyHudData.Member m = new ClientPartyHudData.Member(uuid, name);
                m.online = true;
                m.totalLevel = (i + 1) * 3;
                m.maxHealth = 20f;
                m.health = 10f + (i % 11);
                m.absorption = (i % 3 == 0) ? 2f : 0f;
                m.hunger = 12 + (i % 8);
                m.staminaMax = 20f;  m.staminaNow = 10f + (i % 10);
                m.manaMax    = 20f;  m.manaNow    =  8f + (i % 12);
                m.rpgManaMax = 20f;  m.rpgManaNow =  6f + (i % 14);
                list.add(m);
            }
        } else {
            leaderForSort = ClientPartyHudData.leaderUuid;
            list = new ArrayList<>(ClientPartyHudData.membersSortedExcludingSelf());
        }

        int limit = cfg.maxVisiblePartyHuds;
        return ClientPartyPins.orderedForHud(list, leaderForSort, limit);
    }





    public static List<RenderBar> collectBars(UUID uuid, PartyHudClientConfig cfg,
                                              int hpColor, int hungerColor, int staminaColor, int manaColor, int rpgManaColor) {
        List<RenderBar> out = new ArrayList<>();
        ClientPartyHudData.Member mem = ClientPartyHudData.getByUuid(uuid);

        if (cfg.showHpBar) {
            float pct = 0f, absPct = 0f;
            int now = -1, max = -1;

            if (mem != null) {
                max = Math.round(mem.maxHealth);
                pct    = mem.maxHealth > 0 ? mem.health     / mem.maxHealth : 0f;
                absPct = mem.maxHealth > 0 ? mem.absorption / mem.maxHealth : 0f;
                now = Math.round(mem.health + mem.absorption);
            }

            String txt = (now >= 0 && max > 0) ? (now + " / " + max) : "?? / ??";
            out.add(new RenderBar(Kind.HP, pct, absPct, txt, hpColor));
        }

        if (cfg.showHungerBar) {
            float pct = (mem != null && mem.hunger >= 0) ? Math.max(0f, Math.min(1f, mem.hunger / 20f)) : 0f;
            String txt = (mem != null && mem.hunger >= 0) ? (mem.hunger + " / 20") : "?? / 20";
            out.add(new RenderBar(Kind.HUNGER, pct, 0f, txt, hungerColor));
        }

        if (cfg.showStaminaBar && staminaAvailableCached) {
            float now = -1f, max = -1f;
            if (mem != null && mem.staminaMax > 0f) { now = mem.staminaNow; max = mem.staminaMax; }
            else {
                StackedValue v = queryTracked(uuid, true);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? now / max : 0f;
            String txt = (now >= 0 && max > 0) ? ((int)now + " / " + (int)max) : "?? / ??";
            out.add(new RenderBar(Kind.STAMINA, pct, 0f, txt, staminaColor));
        }

        if (cfg.showManaBar && manaAvailableCached) {
            float now = -1f, max = -1f;
            if (mem != null && mem.manaMax > 0f) { now = mem.manaNow; max = mem.manaMax; }
            else {
                StackedValue v = queryTracked(uuid, false);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? now / max : 0f;
            String txt = (max > 0) ? ((int)now + " / " + (int)max) : "?? / ??";
            out.add(new RenderBar(Kind.MANA, pct, 0f, txt, manaColor));
        }

        if (cfg.showRpgManaBar && rpgManaAvailableCached) {
            float now = -1f, max = -1f;
            if (mem != null && mem.rpgManaMax > 0f) { now = mem.rpgManaNow; max = mem.rpgManaMax; }
            else {
                StackedValue v = queryRpgMana(uuid);
                now = v.now; max = v.max;
            }
            float pct = max > 0 ? now / max : 0f;
            String txt = (max > 0) ? ((int)now + " / " + (int)max) : "?? / ??";
            out.add(new RenderBar(Kind.RPGMANA, pct, 0f, txt, rpgManaColor));
        }

        return out;
    }

    public static void drawHudBarSmart(DrawContext ctx, int x, int y, int w, int h, RenderBar b) {
        switch (b.kind) {
            case HP -> {
                float hp  = clamp01(b.pct);
                float abs = clamp01(b.extraPct);
                drawHudTexturedSegment(ctx, TEX_HP_FILL_HUD, x, y, w, h, 0f, hp);
                if (abs > 0f) drawHudTexturedSegmentWithAlpha(ctx, TEX_ABSORB_FILL_HUD, x, y, w, h, 0f, abs, 0.75f);
                drawHudTexturedWhole(ctx, TEX_BAR_OUTLINE_HUD, x, y, w, h);
            }
            case HUNGER -> {
                drawHudTexturedSegment(ctx, TEX_HUNGER_FILL_HUD, x, y, w, h, 0f, clamp01(b.pct));
                drawHudTexturedWhole(ctx, TEX_BAR_OUTLINE_HUD, x, y, w, h);
            }
            case MANA, RPGMANA -> {
                drawHudTexturedSegment(ctx, TEX_MANA_FILL_HUD, x, y, w, h, 0f, clamp01(b.pct));
                drawHudTexturedWhole(ctx, TEX_BAR_OUTLINE_HUD, x, y, w, h);
            }
            case STAMINA -> {
                drawBarPretty(ctx, x, y, w, h, clamp01(b.pct), 0xFF3E3E3E, 0x00000000, b.color);
            }
        }
    }

    public static void drawHudTexturedWhole(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) HUD_TEX_SRC_W, h / (float) HUD_TEX_SRC_H, 1f);
        ctx.drawTexture(tex, 0, 0, 0, 0, HUD_TEX_SRC_W, HUD_TEX_SRC_H, HUD_TEX_SRC_W, HUD_TEX_SRC_H);
        m.pop();
    }

    public static void drawHudTexturedSegment(DrawContext ctx, Identifier tex, int x, int y, int w, int h, float startPct, float endPct) {
        float s = Math.max(0f, Math.min(1f, startPct));
        float e = Math.max(0f, Math.min(1f, endPct));
        if (e <= s) return;

        int srcU = Math.round(HUD_TEX_SRC_W * s);
        int srcW = Math.max(0, Math.round(HUD_TEX_SRC_W * (e - s)));

        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) HUD_TEX_SRC_W, h / (float) HUD_TEX_SRC_H, 1f);
        ctx.drawTexture(tex, srcU, 0, srcU, 0, srcW, HUD_TEX_SRC_H, HUD_TEX_SRC_W, HUD_TEX_SRC_H);
        m.pop();
    }

    public static void drawHudTexturedSegmentWithAlpha(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                                                       float startPct, float endPct, float alpha) {
        float s = Math.max(0f, Math.min(1f, startPct));
        float e = Math.max(0f, Math.min(1f, endPct));
        if (e <= s) return;

        int srcU = Math.round(HUD_TEX_SRC_W * s);
        int srcW = Math.max(0, Math.round(HUD_TEX_SRC_W * (e - s)));

        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(w / (float) HUD_TEX_SRC_W, h / (float) HUD_TEX_SRC_H, 1f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, alpha)));
        ctx.drawTexture(tex, srcU, 0, srcU, 0, srcW, HUD_TEX_SRC_H, HUD_TEX_SRC_W, HUD_TEX_SRC_H);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        m.pop();
    }

    public static void drawBarPretty(DrawContext ctx, int x, int y, int w, int h, float pct,
                                     int bgARGB, int borderARGB, int fillARGB) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, bgARGB);
        int iw = Math.max(0, Math.round(w * Math.max(0f, Math.min(1f, pct))));
        if (iw > 0) ctx.fill(x, y, x + iw, y + h, fillARGB);
    }

    public static void drawScaledText(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, String text, int x, int y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    public static void drawScaledTextF(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, String text, float x, float y, int color, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0f);
        m.scale(scale, scale, 1f);
        ctx.drawText(tr, text, 0, 0, color, false);
        m.pop();
    }

    public static String ellipsizeScaled(net.minecraft.client.font.TextRenderer tr, String s, int maxPx, float scale) {
        if (Math.round(tr.getWidth(s) * scale) <= maxPx) return s;
        String dots = "...";
        int dw = Math.round(tr.getWidth(dots) * scale);
        StringBuilder b = new StringBuilder(s);
        while (b.length() > 0 && Math.round(tr.getWidth(b.toString()) * scale) + dw > maxPx) b.deleteCharAt(b.length() - 1);
        return b + dots;
    }

    public static Identifier pickArrow(UUID uuid) {
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

    public static PlayerEntity getRenderablePlayer(UUID uuid, String fallbackName) {
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

    public static boolean isLeader(UUID uuid) {
        if (uuid == null) return false;
        return Objects.equals(ClientPartyHudData.leaderUuid, uuid);
    }


    public static void drawTextureScaled(DrawContext ctx, Identifier tex, int x, int y, int w, int h, int texW, int texH, float scale) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        ctx.drawTexture(tex, 0, 0, 0, 0, w, h, texW, texH);
        m.pop();
    }

    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + 1, argb);
        ctx.fill(x, y + h - 1, x + w, y + h, argb);
        ctx.fill(x, y + 1, x + 1, y + h - 1, argb);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    public static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    public static final class StackedValue {
        public final float now;
        public final float max;
        public StackedValue(float now, float max) { this.now = now; this.max = max; }
    }

    public static StackedValue queryTracked(UUID uuid, boolean stamina) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new StackedValue(-1, -1);

        if (stamina && !staminaAvailableCached) return new StackedValue(-1, -1);
        if (!stamina && !manaAvailableCached)   return new StackedValue(-1, -1);

        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new StackedValue(-1, -1);

        if (stamina) {
            float[] pair = TrbAttributesCompat.readStamina(e);
            return new StackedValue(pair[0], pair[1]);
        } else {
            float[] pair = TrbAttributesCompat.readMana(e);
            return new StackedValue(pair[0], pair[1]);
        }
    }

    public static StackedValue queryRpgMana(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new StackedValue(-1, -1);
        if (!rpgManaAvailableCached) return new StackedValue(-1, -1);

        PlayerEntity e = mc.world.getPlayerByUuid(uuid);
        if (e == null) return new StackedValue(-1, -1);

        float[] pair = RpgManaCompat.readMana(e);
        return new StackedValue(pair[0], pair[1]);
    }

    public static void log(String msg) {
        try {
            RPGSystems.LOGGER.info("[PartyHud] " + msg);
        } catch (Throwable t) {
            System.out.println("[PartyHud] " + msg);
        }
    }
}
