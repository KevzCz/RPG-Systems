package net.pixeldreamstudios.rpgsystems.client.enemy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.HealingNumbersClientConfig;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client-only floating healing numbers, e.g. "+2.85" in green.
 * Spawned via EnemyHudPayloads.HealingNumber S2C receiver in EnemyNet.
 */
@Environment(EnvType.CLIENT)
public final class HealingNumbersRenderer {

    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;

    // lifetime & motion
    private static final int   LIFETIME_TICKS      = 26;
    private static final float BASE_RISE_PER_SEC   = 0.70f;  // slightly gentler than damage
    private static final float DRIFT_PER_SEC       = 0.30f;
    private static final float HORIZONTAL_JITTER   = 0.16f;
    private static final float FORWARD_OFFSET      = 0.50f;
    private static final float ROTATE_AMPLITUDE_DEG= 5.0f;

    // spawn "pop"
    private static final int   POP_TICKS           = 6;
    private static final float POP_OVERSHOOT       = 1.15f;

    // text sizing
    private static final float BASE_SCALE          = 0.0235f;
    private static final float DIST_SCALE_MULT     = 0.032f;

    private static final float Y_EXTRA             = 0.12f;
    private static final float ANCHOR_FRACTION     = 0.70f; // slightly below the top of the mob

    private static final int   MAX_ACTIVE          = 256;

    private static final List<Floating> ACTIVE = new ArrayList<>();

    private HealingNumbersRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(HealingNumbersRenderer::onRender);
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> ACTIVE.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ACTIVE.clear());
    }

    /** Called by EnemyNet's S2C handler on the client thread. */
    public static void spawn(int entityId, float amount, boolean isSpell){
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);var cfg = HealingNumbersClientConfig.get();
        if (cfg.mode == HealingNumbersClientConfig.Mode.NONE) return;
// Always queue; mode filtering happens at draw time (needs entity info or isSpell flag)
        ACTIVE.add(new Floating(entityId, amount, isSpell, mc.world.getTime()));
    }

    private static void onRender(WorldRenderContext context) {
        if (ACTIVE.isEmpty()) return;

        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.options.hudHidden) return;

        var world    = context.world();
        var camera   = context.camera();
        var consumers= context.consumers();
        if (world == null || camera == null || consumers == null) return;

        long now = world.getTime();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d camPos = camera.getPos();

        RenderSystem.enableBlend();

        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Floating f = ACTIVE.get(i);

            Entity e = world.getEntityById(f.entityId);
            if (!(e instanceof LivingEntity living) || !living.isAlive()) { ACTIVE.remove(i); continue; }

            int ageTicks = (int)(now - f.spawnTick);
            if (ageTicks >= LIFETIME_TICKS) { ACTIVE.remove(i); continue; }

            if (e.squaredDistanceTo(camPos) > MAX_DISTANCE_SQ) continue;

            double ex = lerp(tickDelta, e.prevX, e.getX());
            double ey = lerp(tickDelta, e.prevY, e.getY());
            double ez = lerp(tickDelta, e.prevZ, e.getZ());

            float tLife = (ageTicks + tickDelta) / LIFETIME_TICKS;
            float rise  = easeOutCubic(tLife) * BASE_RISE_PER_SEC;
            float drift = (ageTicks + tickDelta) / 20f * DRIFT_PER_SEC;

            Box box = living.getBoundingBox();
            double height  = box.getLengthY();
            double anchorY = box.minY + height * ANCHOR_FRACTION;
            double baseY   = anchorY + Y_EXTRA + rise;

            Vec3d toCam = new Vec3d(camPos.x - ex, 0, camPos.z - ez);
            if (toCam.lengthSquared() < 1.0E-6) toCam = new Vec3d(0, 0, 1);
            Vec3d dir  = toCam.normalize();
            Vec3d perp = new Vec3d(-dir.z, 0, dir.x).normalize().multiply(f.jitter);

            Vec3d pos = new Vec3d(ex, baseY, ez)
                    .add(dir.multiply(FORWARD_OFFSET))
                    .add(perp.multiply(1.0 + drift));
            var cfg = HealingNumbersClientConfig.get();

            switch (cfg.mode) {
                case NONE -> { continue; }
                case PLAYERS -> {
                    if (!(e instanceof net.minecraft.entity.player.PlayerEntity)) continue;
                }
                case SPELLS -> {
                    if (!f.isSpell) continue;
                }
                case ALL_ENTITIES -> { /* fallthrough */ }
            }

            double maxSq = (cfg.mode == HealingNumbersClientConfig.Mode.ALL_ENTITIES)
                    ? cfg.viewDistanceSq()
                    : MAX_DISTANCE_SQ;

            if (e.squaredDistanceTo(camPos) > maxSq) continue;

            float alpha     = computeAlpha(ageTicks, tickDelta);
            float dist      = (float)Math.sqrt(e.squaredDistanceTo(camPos));
            float distScale = BASE_SCALE * (1f + dist * DIST_SCALE_MULT);

            float popMul = 1f;
            float popT = clamp01((ageTicks + tickDelta) / POP_TICKS);
            if (popT < 1f) {
                float popEase = easeOutCubic(1f - popT);
                popMul = 1f + (POP_OVERSHOOT - 1f) * popEase;
            }

            float scale = distScale * popMul;
            float tilt  = (float)Math.sin((f.seed + ageTicks + tickDelta) * 0.33f) * ROTATE_AMPLITUDE_DEG;

            // Healing green
            int rgb = 0x77FF77; // soft green
            String text = formatAmount(f.amount, true); // "+2.85"

            drawNumber(context, pos, camera, text, scale, alpha, tilt, rgb);
        }
    }

    private static void drawNumber(WorldRenderContext ctx, Vec3d pos, Camera camera,
                                   String text, float scale, float alpha, float tiltDeg, int rgb) {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        var tr = mc.textRenderer;

        Vec3d cam = camera.getPos();
        ms.push();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

        // billboard and orient correctly
        ms.multiply(camera.getRotation());
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tiltDeg));
        ms.scale(-scale, -scale, scale);

        int widthPixels = tr.getWidth(text);
        float x = -widthPixels / 2f;
        float y = 0f;

        int a    = (int)(alpha * 255f) & 0xFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);

        int light = LightmapTextureManager.pack(15, 15);
        Matrix4f mat = ms.peek().getPositionMatrix();

        // shadowed for readability
        tr.draw(text, x, y, argb, true, mat, vcp,
                net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0, light);

        ms.pop();
    }

    // ----- data & utils -----

    private static final class Floating {
        final int entityId;
        final float amount;
        final long spawnTick;
        final float jitter;
        final float seed;
        final boolean isSpell;
        Floating(int entityId, float amount, boolean isSpell, long spawnTick) {
            this.entityId = entityId;
            this.amount   = amount;
            this.isSpell  = isSpell;
            this.spawnTick= spawnTick;
            this.jitter   = (float)((Math.random() - 0.5) * 2.0 * HORIZONTAL_JITTER);
            this.seed     = (float)Math.random() * 10_000f;
        }
    }

    private static String formatAmount(float amount, boolean plus) {
        // Always 2 decimals; clamp negative to positive for healing
        float v = Math.max(0f, amount);
        String s = String.format(Locale.ROOT, "%.2f", v);
        return plus ? ("+" + s) : s;
    }

    private static float computeAlpha(int ageTicks, float tickDelta) {
        float t = (ageTicks + tickDelta) / LIFETIME_TICKS; // 0..1
        float in  = clamp01((ageTicks + tickDelta) / 4f);
        float out = clamp01(1f - t);
        return clamp01(easeOutCubic(in) * easeOutCubic(out) * 1.05f);
    }
    private static float easeOutCubic(float x) {
        float inv = 1f - clamp01(x);
        return 1f - inv * inv * inv;
    }
    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static double lerp(float t, double a, double b) { return a + (b - a) * t; }
}
