package net.pixeldreamstudios.rpgsystems.client.enemy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.EnemyHudClientConfig;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class EnemyHealthBarRenderer {

    private static final Identifier TEX_BAR    = Identifier.of("rpg-systems", "textures/gui/enemy/hp_bar.png");
    private static final Identifier TEX_BORDER = Identifier.of("rpg-systems", "textures/gui/enemy/hp_border.png");

    private static final float BASE_W_PX = 192f;
    private static final float BASE_H_PX = 44f;

    private static final float BASE_SCALE = 2.0f;
    private static final float BASE_WORLD_W = 0.9f;

    private static final float MIN_ALPHA_FAR = 0.25f;

    private static final int VISIBLE_TICKS   = 20 * 5;
    private static final int FADE_IN_TICKS   = 8;
    private static final int FADE_OUT_TICKS  = 12;

    private static final float DRAIN_RATE_PER_SEC = 0.6f;

    private static final float Y_OFFSET = 0.3f;
    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;

    private static final float MIN_SLIVER_TEXELS = 2f;
    private static final float MIN_SLIVER_PCT    = MIN_SLIVER_TEXELS / BASE_W_PX;

    private static final Map<Integer, HudState> HUD = new ConcurrentHashMap<>();

    private EnemyHealthBarRenderer() {}

    private static final class HudState {
        long firstShownTick;
        long lastHitTick;
        long lastWorldTick;
        float delayedDamagePct;
        float delayedHealPct;
        float lastInstantPct;

        HudState(long nowTick, float pct) {
            this.firstShownTick = nowTick;
            this.lastHitTick = nowTick;
            this.lastWorldTick = nowTick;
            this.delayedDamagePct = pct;
            this.delayedHealPct = pct;
            this.lastInstantPct = pct;
        }
    }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (!EnemyHudClientConfig.get().showHealthbarOnNearbyHpChanges) return ActionResult.PASS;
            var mc = MinecraftClient.getInstance();
            if (mc == null || mc.player != player) return ActionResult.PASS;
            if (entity instanceof LivingEntity living) {
                if (living.hasStatusEffect(StatusEffects.INVISIBILITY)) return ActionResult.PASS;
                long now = world.getTime();
                int id = entity.getId();
                float pct = clamp01((float) (living.getHealth() / Math.max(1e-6, living.getMaxHealth())));
                HUD.compute(id, (k, st) -> st == null ? new HudState(now, pct) : setHitTime(st, now));
            }
            return ActionResult.PASS;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> HUD.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HUD.clear());

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var world = context.world();
            var matrices = context.matrixStack();
            var camera = context.camera();
            if (world == null) return;

            var client = MinecraftClient.getInstance();
            if (client == null || client.options.hudHidden) return;

            var camPos = camera.getPos();
            float tickDelta = client.getRenderTickCounter().getTickDelta(false);
            long nowTick = world.getTime();

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            for (var entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (!living.isAlive()) { HUD.remove(entity.getId()); continue; }
                if (living instanceof PlayerEntity && client.player != null && living.getId() == client.player.getId()) {
                    continue;
                }
                if (living.hasStatusEffect(StatusEffects.INVISIBILITY)) { HUD.remove(entity.getId()); continue; }

                HudState st = HUD.get(entity.getId());
                if (st == null) continue;

                long sinceFirst = nowTick - st.firstShownTick;
                long sinceHit   = nowTick - st.lastHitTick;

                float visAlpha;
                if (sinceFirst <= FADE_IN_TICKS) {
                    visAlpha = clamp01(sinceFirst / (float) FADE_IN_TICKS);
                } else if (sinceHit <= VISIBLE_TICKS) {
                    visAlpha = 1f;
                } else if (sinceHit <= VISIBLE_TICKS + FADE_OUT_TICKS) {
                    visAlpha = clamp01(1f - (sinceHit - VISIBLE_TICKS) / (float) FADE_OUT_TICKS);
                } else {
                    HUD.remove(entity.getId());
                    continue;
                }
                if (visAlpha <= 0.01f) continue;

                double distSq = entity.squaredDistanceTo(camPos.x, camPos.y, camPos.z);
                if (distSq > MAX_DISTANCE_SQ) continue;

                float instantPct = clamp01((float) (living.getHealth() / Math.max(1e-6, living.getMaxHealth())));

                long dtTicks = Math.max(0, nowTick - st.lastWorldTick);
                if (dtTicks > 0) {
                    float dtSec = dtTicks / 20f;

                    if (instantPct + 0.0005f < st.lastInstantPct) {
                        st.delayedDamagePct = Math.max(st.delayedDamagePct, st.lastInstantPct);
                        st.delayedHealPct = instantPct;
                    } else if (instantPct - 0.0005f > st.lastInstantPct) {
                        st.delayedHealPct = Math.min(st.delayedHealPct, st.lastInstantPct);
                        st.delayedDamagePct = instantPct;
                    }

                    if (st.delayedDamagePct > instantPct) {
                        st.delayedDamagePct = Math.max(instantPct, st.delayedDamagePct - DRAIN_RATE_PER_SEC * dtSec);
                    } else {
                        st.delayedDamagePct = instantPct;
                    }

                    if (st.delayedHealPct < instantPct) {
                        st.delayedHealPct = Math.min(instantPct, st.delayedHealPct + DRAIN_RATE_PER_SEC * dtSec);
                    } else {
                        st.delayedHealPct = instantPct;
                    }

                    st.lastWorldTick = nowTick;
                    st.lastInstantPct = instantPct;
                }

                if (instantPct <= 0f && st.delayedDamagePct <= 0f && st.delayedHealPct <= 0f) continue;

                Box box = living.getBoundingBox();
                if (!isOnScreen(context, box)) continue;

                int extra = Math.max(0, (int) Math.floor(box.getLengthY() - 2.0));
                float dynScale = BASE_SCALE + extra;

                float worldW = BASE_WORLD_W * dynScale;
                float worldH = worldW * (BASE_H_PX / BASE_W_PX);

                float dist = (float) Math.sqrt(distSq);
                float maxDist = (float) Math.sqrt(MAX_DISTANCE_SQ);
                float t = clamp01(dist / maxDist);
                float nearFactor = 1f - t;
                float distanceAlpha = MIN_ALPHA_FAR + (1f - MIN_ALPHA_FAR) * nearFactor;
                float finalAlpha = clamp01(visAlpha * distanceAlpha);
                if (finalAlpha <= 0.01f) continue;

                double ex = lerp(tickDelta, living.prevX, living.getX());
                double ey = lerp(tickDelta, living.prevY, living.getY());
                double ez = lerp(tickDelta, living.prevZ, living.getZ());
                double head = box.getLengthY() + Y_OFFSET;

                matrices.push();
                matrices.translate(ex - camPos.x, ey - camPos.y + head, ez - camPos.z);
                faceCamera(matrices, camera);
                matrices.scale(worldW, worldH, 1f);

                drawBars(matrices, instantPct, st.delayedDamagePct, st.delayedHealPct, finalAlpha);

                matrices.pop();
            }
        });
    }

    private static HudState setHitTime(HudState st, long now) {
        st.lastHitTick = now;
        return st;
    }

    public static void pokeOnHpChange(int targetEntityId) {
        if (!EnemyHudClientConfig.get().showHealthbarOnNearbyHpChanges) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return;
        var e = mc.world.getEntityById(targetEntityId);
        if (!(e instanceof LivingEntity living)) return;

        if (living instanceof PlayerEntity && living.getId() == mc.player.getId()) return;
        if (living.hasStatusEffect(StatusEffects.INVISIBILITY)) { HUD.remove(living.getId()); return; }

        double dSq = living.squaredDistanceTo(mc.player);
        if (dSq > MAX_DISTANCE_SQ) return;

        long now = mc.world.getTime();
        float pct = clamp01((float) (living.getHealth() / Math.max(1e-6, living.getMaxHealth())));
        HUD.compute(living.getId(), (k, st) -> st == null ? new HudState(now, pct) : setHitTime(st, now));
    }

    private static boolean isOnScreen(WorldRenderContext context, Box box) {
        var frustum = context.frustum();
        return frustum == null || frustum.isVisible(box);
    }

    private static void faceCamera(MatrixStack matrices, Camera camera) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
    }

    private static void drawBars(MatrixStack matrices, float instantPct, float delayedDamagePct, float delayedHealPct, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        float halfW = 0.5f;
        float halfH = 0.5f;
        int light = LightmapTextureManager.pack(15, 15);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        float leftX  = -halfW;
        float rightX =  halfW;
        float width  = rightX - leftX;

        float vTop = 1f, vBot = 0f;

        float instDrawPct = instantPct > 0f ? clamp01(Math.max(instantPct, MIN_SLIVER_PCT)) : 0f;

        float dmgFrom = Math.max(instDrawPct, delayedDamagePct);
        if (dmgFrom - instDrawPct > MIN_SLIVER_PCT) {
            float x0 = rightX - width * dmgFrom;
            float x1 = rightX - width * instDrawPct;
            float u0 = dmgFrom;
            float u1 = instDrawPct;

            drawTexturedQuad(TEX_BAR, mat, x0, -halfH, x1, halfH, u0, vTop, u1, vBot, light, 1f, 0.35f, 0.35f, alpha);
        }

        float healTo = Math.min(instDrawPct, Math.max(delayedHealPct, 0f));
        if (instDrawPct - healTo > MIN_SLIVER_PCT) {
            float x0 = rightX - width * instDrawPct;
            float x1 = rightX - width * healTo;
            float u0 = instDrawPct;
            float u1 = healTo;

            drawTexturedQuad(TEX_BAR, mat, x0, -halfH, x1, halfH, u0, vTop, u1, vBot, light, 0.35f, 1f, 0.35f, alpha);
        }

        if (instDrawPct > 0f) {
            float x0 = rightX - width * instDrawPct;
            float x1 = rightX;
            float u0 = instDrawPct;
            float u1 = 0f;

            drawTexturedQuad(TEX_BAR, mat, x0, -halfH, x1, halfH, u0, vTop, u1, vBot, light, 1f, 1f, 1f, alpha);
        }

        drawTexturedQuad(TEX_BORDER, mat, leftX, -halfH, rightX, halfH, 1f, vTop, 0f, vBot, light, 1f, 1f, 1f, alpha);
    }

    private static void drawTexturedQuad(Identifier tex, Matrix4f mat,
                                         float x0, float y0, float x1, float y1,
                                         float u0, float v0, float u1, float v1,
                                         int light,
                                         float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);
        RenderSystem.setShaderTexture(0, tex);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);

        buf.vertex(mat, x0, y1, 0f).color(r, g, b, a).texture(u0, v1).light(light);
        buf.vertex(mat, x1, y1, 0f).color(r, g, b, a).texture(u1, v1).light(light);
        buf.vertex(mat, x1, y0, 0f).color(r, g, b, a).texture(u1, v0).light(light);
        buf.vertex(mat, x0, y0, 0f).color(r, g, b, a).texture(u0, v0).light(light);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static double lerp(float t, double a, double b) { return a + (b - a) * t; }
}
