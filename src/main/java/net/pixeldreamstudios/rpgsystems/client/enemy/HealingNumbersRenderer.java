package net.pixeldreamstudios.rpgsystems.client.enemy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class HealingNumbersRenderer {
    private static final int   LIFETIME_TICKS       = 26;
    private static final float BASE_RISE_PER_SEC    = 0.70f;
    private static final float DRIFT_PER_SEC        = 0.30f;
    private static final float HORIZONTAL_JITTER    = 0.16f;
    private static final float FORWARD_OFFSET       = 0.50f;
    private static final float ROTATE_AMPLITUDE_DEG = 5.0f;

    private static final int   POP_TICKS            = 6;
    private static final float POP_OVERSHOOT        = 1.15f;

    private static final float BASE_SCALE           = 0.0235f;
    private static final float DIST_SCALE_MULT      = 0.032f;

    private static final float Y_EXTRA              = 0.12f;
    private static final float ANCHOR_FRACTION      = 0.70f;

    private static final int   MAX_ACTIVE           = 256;

    private static final List<Floating> ACTIVE = new ArrayList<>();

    private HealingNumbersRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(HealingNumbersRenderer::onRender);
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> ACTIVE.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ACTIVE.clear());
    }

    public static void spawn(int entityId, float amount, boolean isSpell, UUID sourceUuid){
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;

        var cfg = HealingNumbersClientConfig.get();
        if (cfg.mode == HealingNumbersClientConfig.Mode.NONE) return;

        if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
        ACTIVE.add(new Floating(mc, entityId, amount, isSpell, sourceUuid, mc.world.getTime()));
    }

    private static void onRender(WorldRenderContext context) {
        if (ACTIVE.isEmpty()) return;

        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.options.hudHidden) return;

        var world = context.world();
        var camera = context.camera();
        var consumers = context.consumers();
        if (world == null || camera == null || consumers == null) return;

        long now = world.getTime();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d camPos = camera.getPos();

        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        int localPlayerEntityId = mc.player != null ? mc.player.getId() : Integer.MIN_VALUE;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Floating f = ACTIVE.get(i);

            int ageTicks = (int)(now - f.spawnTick);
            if (ageTicks >= LIFETIME_TICKS) { ACTIVE.remove(i); continue; }

            if (firstPerson && f.entityId == localPlayerEntityId) continue;

            Entity e = world.getEntityById(f.entityId);
            LivingEntity living = e instanceof LivingEntity ? (LivingEntity) e : null;

            if (living != null && f.needsBootstrap()) {
                f.bootstrapFromEntity(living);
            }

            if (f.needsBootstrap()) continue;

            float tLife = (ageTicks + tickDelta) / LIFETIME_TICKS;
            float rise  = easeOutCubic(tLife) * BASE_RISE_PER_SEC;
            float drift = (ageTicks + tickDelta) / 20f * DRIFT_PER_SEC;

            Vec3d toCam = new Vec3d(camPos.x - f.baseX, 0, camPos.z - f.baseZ);
            if (toCam.lengthSquared() < 1.0E-6) toCam = new Vec3d(0, 0, 1);
            Vec3d dir  = toCam.normalize();
            Vec3d perp = new Vec3d(-dir.z, 0, dir.x).normalize().multiply(f.jitter);

            Vec3d pos = new Vec3d(f.baseX, f.baseY + rise, f.baseZ)
                    .add(dir.multiply(FORWARD_OFFSET))
                    .add(perp.multiply(1.0 + drift));

            float alpha = computeAlpha(ageTicks, tickDelta);

            double dx = f.baseX - camPos.x;
            double dz = f.baseZ - camPos.z;
            float dist = (float) Math.sqrt(dx*dx + dz*dz);
            float distScale = BASE_SCALE * (1f + dist * DIST_SCALE_MULT);

            float popMul = 1f;
            float popT = clamp01((ageTicks + tickDelta) / POP_TICKS);
            if (popT < 1f) {
                float popEase = easeOutCubic(1f - popT);
                popMul = 1f + (POP_OVERSHOOT - 1f) * popEase;
            }

            float scale = distScale * popMul;

            float tilt = (float) Math.sin((f.seed + ageTicks + tickDelta) * 0.33f) * ROTATE_AMPLITUDE_DEG;

            int rgb = f.isSpell ? 0x72E0FF : 0x8AF58A;

            drawTextAt((int)(alpha * 255f), rgb, f.displayText, pos, camera, context.matrixStack(), context.consumers(), scale, tilt);
        }
    }

    private static boolean isFromMyParty(UUID sourceUuid) {
        if (sourceUuid == null) return false;
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;
        if (sourceUuid.equals(mc.player.getUuid())) return true;
        try {
            Class<?> c = Class.forName("net.pixeldreamstudios.rpgsystems.client.ClientPartyHudData");
            try {
                var f = c.getField("memberUuids");
                Object o = f.get(null);
                if (o instanceof Set<?> set) return set.contains(sourceUuid);
            } catch (NoSuchFieldException ignored) { }
            try {
                var m = c.getMethod("isInMyParty", UUID.class);
                Object r = m.invoke(null, sourceUuid);
                if (r instanceof Boolean b) return b;
            } catch (NoSuchMethodException ignored) { }
        } catch (Throwable ignored) { }
        return false;
    }

    private static void drawTextAt(int alpha255, int rgb, String text, Vec3d pos, Camera camera, MatrixStack ms, VertexConsumerProvider vcp, float scale, float tiltDeg) {
        var mc = MinecraftClient.getInstance();
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

        int a = alpha255 & 0xFF;
        int argb = (a << 24) | (rgb & 0xFFFFFF);

        int light = LightmapTextureManager.pack(15, 15);
        Matrix4f mat = ms.peek().getPositionMatrix();

        tr.draw(text, x, y, argb, true, mat, vcp,
                TextRenderer.TextLayerType.NORMAL, 0, light);

        ms.pop();
    }

    private static final class Floating {
        final int entityId;
        final String displayText;
        final boolean isSpell;
        final UUID sourceUuid;
        final long spawnTick;
        final float jitter;
        final float seed;
        double baseX = Double.NaN, baseY = Double.NaN, baseZ = Double.NaN;

        Floating(MinecraftClient mc, int entityId, float amount, boolean isSpell, UUID sourceUuid, long spawnTick) {
            this.entityId = entityId;
            this.displayText = formatAmount(amount);
            this.isSpell = isSpell;
            this.sourceUuid = sourceUuid;
            this.spawnTick = spawnTick;
            this.jitter = (float) ((Math.random() - 0.5) * 2.0 * HORIZONTAL_JITTER);
            this.seed = (float) Math.random() * 10_000f;

            if (mc.world != null) {
                Entity e = mc.world.getEntityById(entityId);
                if (e instanceof LivingEntity living) bootstrapFromEntity(living);
            }
        }

        static String formatAmount(float amount) {
            float abs = Math.abs(amount);
            long rounded = Math.round(abs);
            if (Math.abs(abs - rounded) < 0.005f) {
                return "+" + rounded;
            }
            String s = String.format(Locale.ROOT, "+%.2f", abs);
            int dot = s.indexOf('.');
            if (dot >= 0) {
                int end = s.length();
                while (end > dot + 1 && s.charAt(end - 1) == '0') end--;
                if (end == dot + 1) end = dot;
                s = s.substring(0, end);
            }
            return s;
        }

        boolean needsBootstrap() {
            return Double.isNaN(baseX) || Double.isNaN(baseY) || Double.isNaN(baseZ);
        }

        void bootstrapFromEntity(LivingEntity living) {
            Box box = living.getBoundingBox();
            double height  = box.getLengthY();
            double anchorY = box.minY + height * ANCHOR_FRACTION;
            baseX = living.getX();
            baseY = anchorY + Y_EXTRA;
            baseZ = living.getZ();
        }
    }

    private static float computeAlpha(int ageTicks, float tickDelta) {
        float t = (ageTicks + tickDelta) / LIFETIME_TICKS;
        float in  = clamp01((ageTicks + tickDelta) / 4f);
        float out = clamp01(1f - t);
        return clamp01(easeOutCubic(in) * easeOutCubic(out) * 1.05f);
    }
    private static float easeOutCubic(float x) {
        float inv = 1f - clamp01(x);
        return 1f - inv * inv * inv;
    }
    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
