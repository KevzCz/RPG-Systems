// net/pixeldreamstudios/rpgsystems/client/enemy/DamageNumbersRenderer.java
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
import net.pixeldreamstudios.rpgsystems.client.enemy.config.DamageNumbersClientConfig;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class DamageNumbersRenderer {

    // motion/life
    private static final int   LIFETIME_TICKS     = 26;
    private static final float BASE_RISE_PER_SEC  = 0.80f;
    private static final float DRIFT_PER_SEC      = 0.35f;
    private static final float HORIZONTAL_JITTER  = 0.18f;
    private static final float FORWARD_OFFSET     = 0.55f;
    private static final float ROTATE_AMPLITUDE_DEG = 6.0f;

    private static final int   POP_TICKS          = 6;
    private static final float POP_OVERSHOOT      = 1.20f;

    private static final float BASE_SCALE         = 0.025f;
    private static final float DIST_SCALE_MULT    = 0.035f;
    private static final float CRIT_SCALE_MULT    = 1.15f;

    private static final float Y_EXTRA            = 0.15f;
    private static final float ANCHOR_FRACTION    = 0.72f;

    private static final int   MAX_ACTIVE         = 256;

    private static final List<Floating> ACTIVE = new ArrayList<>();

    private DamageNumbersRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(DamageNumbersRenderer::onRender);
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> ACTIVE.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ACTIVE.clear());
    }

    /** Back-compat: old packets without pet info. */
    public static void spawn(int entityId, float amount, boolean crit) {
        spawn(entityId, amount, crit, false);
    }

    /** Called on the client thread by S2C handler. */
    public static void spawn(int entityId, float amount, boolean crit, boolean isPet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var cfg = DamageNumbersClientConfig.get();
        if (!cfg.enabled) return;
        if (isPet && !cfg.showPetDamage) return;
        if (mc == null || mc.world == null) return;
        if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
        ACTIVE.add(new Floating(entityId, amount, crit, isPet, mc.world.getTime()));
    }

    private static void onRender(WorldRenderContext context) {
        if (ACTIVE.isEmpty()) return;
        var cfg = DamageNumbersClientConfig.get();
        if (!cfg.enabled) return;

        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.options.hudHidden) return;

        var world = context.world();
        var camera = context.camera();
        var consumers = context.consumers();
        if (world == null || camera == null || consumers == null) return;

        long now = world.getTime();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d camPos = camera.getPos();

        RenderSystem.enableBlend();

        double maxDistSq = cfg.viewDistanceSq();

        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Floating f = ACTIVE.get(i);

            Entity e = world.getEntityById(f.entityId);
            if (!(e instanceof LivingEntity living) || !living.isAlive()) { ACTIVE.remove(i); continue; }

            int ageTicks = (int) (now - f.spawnTick);
            if (ageTicks >= LIFETIME_TICKS) { ACTIVE.remove(i); continue; }

            if (e.squaredDistanceTo(camPos) > maxDistSq) continue;

            double ex = lerp(tickDelta, e.prevX, e.getX());
            double ey = lerp(tickDelta, e.prevY, e.getY());
            double ez = lerp(tickDelta, e.prevZ, e.getZ());

            float tLife = (ageTicks + tickDelta) / LIFETIME_TICKS;
            float riseEase = easeOutCubic(tLife);
            float rise = riseEase * BASE_RISE_PER_SEC;

            float drift = (ageTicks + tickDelta) / 20f * DRIFT_PER_SEC;

            Box box = living.getBoundingBox();
            double height = box.getLengthY();
            double anchorY = box.minY + height * ANCHOR_FRACTION;
            double baseY = anchorY + Y_EXTRA + rise;

            Vec3d mobCenter = new Vec3d(ex, baseY, ez);
            Vec3d toCam = new Vec3d(camPos.x - ex, 0, camPos.z - ez);
            if (toCam.lengthSquared() < 1.0E-6) toCam = new Vec3d(0, 0, 1);
            Vec3d dir = toCam.normalize();

            Vec3d perp = new Vec3d(-dir.z, 0, dir.x).normalize().multiply(f.jitter);

            Vec3d pos = mobCenter
                    .add(dir.multiply(FORWARD_OFFSET))
                    .add(perp.multiply(1.0 + drift));

            float alpha = computeAlpha(ageTicks, tickDelta);

            float dist = (float) Math.sqrt(e.squaredDistanceTo(camPos));
            float distScale = BASE_SCALE * (1f + dist * DIST_SCALE_MULT);

            float popMul = 1f;
            float popT = clamp01((ageTicks + tickDelta) / POP_TICKS);
            if (popT < 1f) {
                float popEase = easeOutCubic(1f - popT);
                popMul = 1f + (POP_OVERSHOOT - 1f) * popEase;
            }

            float critMul = f.crit ? CRIT_SCALE_MULT : 1f;
            float scale = distScale * popMul * critMul;

            float tilt = (float) Math.sin((f.seed + ageTicks + tickDelta) * 0.35f) * ROTATE_AMPLITUDE_DEG;

            int rgb = f.crit ? 0xFFE07A : 0xFFFFFF; // keep style; you could tint pets differently
            if (f.crit) alpha = clamp01(alpha * 1.05f);

            String text = f.displayText;
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

        ms.multiply(camera.getRotation());
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tiltDeg));
        ms.scale(-scale, -scale, scale);

        int widthPixels = tr.getWidth(text);
        float x = -widthPixels / 2f;
        float y = 0f;

        int a = (int) (alpha * 255f) & 0xFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);

        int light = LightmapTextureManager.pack(15, 15);
        Matrix4f mat = ms.peek().getPositionMatrix();

        tr.draw(text, x, y, argb, true, mat, vcp,
                net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0, light);

        ms.pop();
    }

    private static final class Floating {
        final int entityId;
        final String displayText;
        final boolean crit;
        final boolean pet;
        final long spawnTick;
        final float jitter;
        final float seed;

        Floating(int entityId, float amount, boolean crit, boolean pet, long spawnTick) {
            this.entityId = entityId;
            this.displayText = String.format(Locale.ROOT, "%.2f", Math.abs(amount));
            this.crit = crit;
            this.pet = pet;
            this.spawnTick = spawnTick;
            this.jitter = (float) ((Math.random() - 0.5) * 2.0 * HORIZONTAL_JITTER);
            this.seed = (float) Math.random() * 10_000f;
        }
    }

    private static float computeAlpha(int ageTicks, float tickDelta) {
        float t = (ageTicks + tickDelta) / LIFETIME_TICKS;
        float in = clamp01((ageTicks + tickDelta) / 4f);
        float out = clamp01(1f - t);
        return clamp01(easeOutCubic(in) * easeOutCubic(out) * 1.05f);
    }
    private static float easeOutCubic(float x) { float inv = 1f - clamp01(x); return 1f - inv * inv * inv; }
    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static double lerp(float t, double a, double b) { return a + (b - a) * t; }
}
