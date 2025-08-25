package net.pixeldreamstudios.rpgsystems.client.enemy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class EnemyHealthBarRenderer {

    private static final Identifier TEX_BAR    = Identifier.of("rpg-systems", "textures/gui/enemy/hp_bar.png");
    private static final Identifier TEX_BORDER = Identifier.of("rpg-systems", "textures/gui/enemy/hp_border.png");

    private static final float BASE_W_PX = 192f;
    private static final float BASE_H_PX = 44f;

    // Base scale for bar & border. Tall mobs add to this dynamically (+1 per full block above 2).
    private static final float BASE_SCALE = 2.0f;
    private static final float BASE_WORLD_W = 0.9f; // width before dynamic mob-height scale

    // Distance transparency: far = more transparent, near = more opaque.
    private static final float MIN_ALPHA_FAR = 0.25f;

    // Visibility timing (ticks): fade in once, hold for 5s after last hit, then fade out
    private static final int VISIBLE_TICKS   = 20 * 5;   // 5 seconds
    private static final int FADE_IN_TICKS   = 8;        // ~0.4s
    private static final int FADE_OUT_TICKS  = 12;       // ~0.6s

    // Drain bar behavior
    private static final float DRAIN_RATE_PER_SEC = 0.6f; // how fast the delayed bar catches up (pct/second)

    private static final float Y_OFFSET = 0.3f;
    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;

    // --- NEW: ensure a sliver of HP is always visible (in texture pixels) ---
    private static final float MIN_SLIVER_TEXELS = 2f;                     // ~2px
    private static final float MIN_SLIVER_PCT    = MIN_SLIVER_TEXELS / BASE_W_PX;

    // Per-entity HUD state
    private static final Map<Integer, HudState> HUD = new ConcurrentHashMap<>();

    private EnemyHealthBarRenderer() {}

    // Per-entity state holder
    private static final class HudState {
        long firstShownTick;   // when the bar first appeared (for fade-in)
        long lastHitTick;      // last time player hit this entity (for hold/fade-out)
        long lastWorldTick;    // last tick we updated drain
        float delayedPct;      // trailing/drain bar percent (>= instant pct when taking damage)

        HudState(long nowTick, float pct) {
            this.firstShownTick = nowTick;
            this.lastHitTick = nowTick;
            this.lastWorldTick = nowTick;
            this.delayedPct = pct;
        }
    }

    public static void init() {
        // Record hits from the local player; refresh lastHitTick but don't reset firstShownTick
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient) return ActionResult.PASS;
            var mc = MinecraftClient.getInstance();
            if (mc == null || mc.player != player) return ActionResult.PASS;
            if (entity instanceof LivingEntity living) {
                long now = world.getTime();
                int id = entity.getId();

                // Use current observed health at hit time to seed if new
                float pct = clamp01((float)(living.getHealth() / Math.max(1e-6, living.getMaxHealth())));
                HUD.compute(id, (k, st) -> {
                    if (st == null) return new HudState(now, pct);
                    st.lastHitTick = now; // refresh visibility window (no re-fade)
                    // If damage landed, delayedPct should never fall below current for the damage bar
                    st.delayedPct = Math.max(st.delayedPct, pct);
                    return st;
                });
            }
            return ActionResult.PASS;
        });

        // Clear state on world change
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

            for (var entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (!living.isAlive()) { HUD.remove(entity.getId()); continue; }
                if (living instanceof PlayerEntity) continue;

                HudState st = HUD.get(entity.getId());
                if (st == null) continue; // only show after a hit

                long sinceFirst = nowTick - st.firstShownTick;
                long sinceHit   = nowTick - st.lastHitTick;

                // Fade curve: fade in once -> hold since last hit -> fade out -> remove
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

                // Current (instant) health percent
                float instantPct = clamp01((float)(living.getHealth() / Math.max(1e-6, living.getMaxHealth())));

                // Drain bar update using world ticks (20 tps)
                long dtTicks = Math.max(0, nowTick - st.lastWorldTick);
                if (dtTicks > 0) {
                    float dtSec = dtTicks / 20f;
                    if (st.delayedPct > instantPct) {
                        st.delayedPct = Math.max(instantPct, st.delayedPct - DRAIN_RATE_PER_SEC * dtSec);
                    } else if (st.delayedPct < instantPct) {
                        // Healing or overshoot: snap up to current
                        st.delayedPct = instantPct;
                    }
                    st.lastWorldTick = nowTick;
                }

                // Cull if both are effectively zero
                if (instantPct <= 0f && st.delayedPct <= 0f) continue;

                Box box = living.getBoundingBox();
                if (!isOnScreen(context, box)) continue;

                // Dynamic scale: +1 per full block above 2 blocks tall
                int extra = Math.max(0, (int) Math.floor(box.getLengthY() - 2.0));
                float dynScale = BASE_SCALE + extra;

                float worldW = BASE_WORLD_W * dynScale;
                float worldH = worldW * (BASE_H_PX / BASE_W_PX); // aspect-correct

                // Distance-based alpha: closer => more opaque, farther => more transparent
                float dist = (float) Math.sqrt(distSq);
                float maxDist = (float) Math.sqrt(MAX_DISTANCE_SQ);
                float t = clamp01(dist / maxDist);   // 0 near .. 1 far
                float nearFactor = 1f - t;           // 1 near .. 0 far
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

                drawBars(matrices, instantPct, st.delayedPct, finalAlpha);

                matrices.pop();
            }
        });
    }

    private static boolean isOnScreen(WorldRenderContext context, Box box) {
        var frustum = context.frustum();
        return frustum == null || frustum.isVisible(box);
    }

    private static void faceCamera(MatrixStack matrices, Camera camera) {
        // billboard (no upside-down)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
    }

    /**
     * Draw two-layer bar:
     * - Damage (delayed) region between delayedPct and instantPct (right-anchored), tinted.
     * - Instant bar at instantPct (right-anchored), full texture color with a "sliver" minimum if >0 HP.
     * - Border on top.
     */
    private static void drawBars(MatrixStack matrices, float instantPct, float delayedPct, float alpha) {
        RenderSystem.enableBlend();

        float halfW = 0.5f;
        float halfH = 0.5f; // unit square; aspect handled by matrix scale
        int light = LightmapTextureManager.pack(15, 15);

        Matrix4f mat = matrices.peek().getPositionMatrix();

        float leftX  = -halfW;
        float rightX =  halfW;
        float width  = rightX - leftX;

        float vTop = 1f, vBot = 0f; // flipped V already

        // --- Apply sliver only to what we DRAW (keeps internal math accurate) ---
        float instDrawPct   = instantPct > 0f ? clamp01(Math.max(instantPct, MIN_SLIVER_PCT)) : 0f;
        float delayedDrawPct = clamp01(Math.max(delayedPct, instDrawPct));

        // --- Damage region (only if delayed > instant) ---
        if (delayedDrawPct > instDrawPct) {
            float x0 = rightX - width * delayedDrawPct; // farther left edge
            float x1 = rightX - width * instDrawPct;    // nearer right edge
            float u0 = delayedDrawPct;                  // flipped U: remaining pct
            float u1 = instDrawPct;

            // Reddish damage tint
            drawTexturedQuad(TEX_BAR, mat, x0, -halfH, x1, halfH,
                    u0, vTop, u1, vBot, light,
                    1f, 0.35f, 0.35f, alpha);
        }

        // --- Instant bar (current health), right-anchored ---
        if (instDrawPct > 0f) {
            float x0 = rightX - width * instDrawPct;
            float x1 = rightX;
            float u0 = instDrawPct;
            float u1 = 0f;

            drawTexturedQuad(TEX_BAR, mat, x0, -halfH, x1, halfH,
                    u0, vTop, u1, vBot, light,
                    1f, 1f, 1f, alpha);
        }

        // --- Border (full) ---
        drawTexturedQuad(TEX_BORDER, mat, leftX, -halfH, rightX, halfH,
                1f, vTop, 0f, vBot, light,
                1f, 1f, 1f, alpha);
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

        buf.vertex(mat, x0, y1, 0f).color(r,g,b,a).texture(u0, v1).light(light);
        buf.vertex(mat, x1, y1, 0f).color(r,g,b,a).texture(u1, v1).light(light);
        buf.vertex(mat, x1, y0, 0f).color(r,g,b,a).texture(u1, v0).light(light);
        buf.vertex(mat, x0, y0, 0f).color(r,g,b,a).texture(u0, v0).light(light);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static double lerp(float t, double a, double b) {
        return a + (b - a) * t;
    }
}
