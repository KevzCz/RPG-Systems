package net.pixeldreamstudios.rpgsystems.client.title;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class PlayerTitleRenderer {
    private static final float BASE_WORLD_W = 0.6f;
    private static final float NAME_GAP = 0.20f;
    private static final float Y_OFFSET = 0.55f;
    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;
    private static final float MIN_ALPHA_FAR = 0.35f;

    private PlayerTitleRenderer() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> {});
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {});
        WorldRenderEvents.LAST.register(PlayerTitleRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        var world = context.world();
        var matrices = context.matrixStack();
        var camera = context.camera();
        if (world == null) return;

        var client = MinecraftClient.getInstance();
        if (client == null || client.options.hudHidden) return;

        boolean isFirstPerson = client.options.getPerspective().isFirstPerson();
        var camPos = camera.getPos();
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        for (var entity : world.getPlayers()) {
            PlayerEntity player = entity;
            if (!player.isAlive()) continue;

            Identifier titleId = player.getUuid().equals(client.player != null ? client.player.getUuid() : null)
                    ? TitleClientData.getSelfActive()
                    : TitleClientData.getActive(player.getUuid());
            if (titleId == null) continue;

            double distSq = player.squaredDistanceTo(camPos.x, camPos.y, camPos.z);
            if (distSq > MAX_DISTANCE_SQ) continue;

            Box box = player.getBoundingBox();
            if (!isOnScreen(context, box)) continue;

            float dist = (float) Math.sqrt(distSq);
            float maxDist = (float) Math.sqrt(MAX_DISTANCE_SQ);
            float t = clamp01(dist / maxDist);
            float nearFactor = 1f - t;
            float distanceAlpha = MIN_ALPHA_FAR + (1f - MIN_ALPHA_FAR) * nearFactor;
            float finalAlpha = clamp01(distanceAlpha);
            if (finalAlpha <= 0.01f) continue;

            double ex = lerp(tickDelta, player.prevX, player.getX());
            double ey = lerp(tickDelta, player.prevY, player.getY());
            double ez = lerp(tickDelta, player.prevZ, player.getZ());
            double head = box.getLengthY() + Y_OFFSET;

            float hpOffset = net.pixeldreamstudios.rpgsystems.client.enemy.EnemyHealthBarRenderer.getNameYOffset(player, tickDelta) * 0.6f;

            boolean isSelf = client.player != null && player.getId() == client.player.getId();
            var titleCfg = net.pixeldreamstudios.rpgsystems.client.title.config.TitlesClientConfig.get();
            if (isSelf && !titleCfg.showOwnTitle) continue;
            if (!isSelf && !titleCfg.showOthersTitles) continue;
            if (isSelf && isFirstPerson) continue;

            float gapAboveName = isSelf ? 0f : NAME_GAP;

            matrices.push();
            matrices.translate(ex - camPos.x, ey - camPos.y + head + hpOffset + gapAboveName, ez - camPos.z);
            faceCamera(matrices, camera);

            TitleTextureResolver.FrameInfo fi = TitleTextureResolver.currentFrame(titleId);
            if (fi != null) {
                float worldW = BASE_WORLD_W;
                float worldH = worldW * ((float) fi.frameHeight / (float) fi.frameWidth);

                matrices.scale(worldW, worldH, 1f);

                RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);
                RenderSystem.setShaderTexture(0, fi.textureId);

                drawQuadLitUV(matrices.peek().getPositionMatrix(), fi.u0, fi.v0, fi.u1, fi.v1, finalAlpha);
                matrices.pop();
                continue;
            }

            var tTitle = net.pixeldreamstudios.rpgsystems.title.TitleRegistry.get(titleId);
            Text label = tTitle != null ? tTitle.displayName : Text.literal(titleId.getPath());
            drawStyledTitle3D(context, matrices, label, finalAlpha);
            matrices.pop();
        }

        RenderSystem.depthMask(true);
    }

    private static void drawQuadLitUV(Matrix4f mat, float u0, float v0, float u1, float v1, float alpha) {
        int light = LightmapTextureManager.pack(15, 15);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);

        buf.vertex(mat, -0.5f,  0.5f, 0f).color(1f,1f,1f,alpha).texture(u1, v0).light(light);
        buf.vertex(mat,  0.5f,  0.5f, 0f).color(1f,1f,1f,alpha).texture(u0, v0).light(light);
        buf.vertex(mat,  0.5f, -0.5f, 0f).color(1f,1f,1f,alpha).texture(u0, v1).light(light);
        buf.vertex(mat, -0.5f, -0.5f, 0f).color(1f,1f,1f,alpha).texture(u1, v1).light(light);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void drawStyledTitle3D(WorldRenderContext context, MatrixStack matrices, Text text, float alpha) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String raw = text.getString();
        java.util.List<net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.Span> spans =
                net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.parseSpans(raw);

        int light = LightmapTextureManager.pack(15, 15);
        float scale = 0.025f;

        matrices.push();
        matrices.scale(-scale, -scale, scale);

        float totalW = 0f;
        for (var sp : spans) totalW += tr.getWidth(sp.text());
        float baseX = -totalW / 2f;

        var consumers = context.consumers();
        if (consumers == null) {
            consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        }

        long nowMs = Util.getMeasuringTimeMs();
        float advanceX = 0f;
        int globalIndex = 0;

        for (var span : spans) {
            String t = span.text();
            int localLen = t.codePointCount(0, t.length());
            int localIndex = 0;

            for (int i = 0; i < t.length(); ) {
                int cp = t.codePointAt(i);
                String ch = new String(Character.toChars(cp));
                int cw = tr.getWidth(ch);

                int rgb = span.gradient() != null
                        ? net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.gradientRgb(localIndex, localLen, span.gradient())
                        : (span.rainbow()
                        ? net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.rainbowRgb(nowMs, globalIndex, span.rainbowSpeed() != null ? span.rainbowSpeed() : 0.18f)
                        : net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.resolveOrWhite(span.baseRgb()));

                if (span.pulseSpeed() != null) {
                    rgb = net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.pulseRgb(nowMs, rgb, span.pulseSpeed());
                }

                float yOff = 0f;
                if (span.wiggle()) {
                    float amp = span.wiggleAmp() != null ? span.wiggleAmp() : 2.0f;
                    yOff += net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.wiggleYOffsetPx(nowMs, globalIndex, amp);
                }
                if (span.bounceAmp() != null || span.bounceSpeed() != null) {
                    float amp = span.bounceAmp() != null ? span.bounceAmp() : 0f;
                    float spd = span.bounceSpeed() != null ? span.bounceSpeed() : 3.0f;
                    yOff += net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.bounceYOffsetPx(nowMs, globalIndex, amp, spd);
                }

                float xOff = 0f;
                if (span.waveAmp() != null || span.waveSpeed() != null) {
                    float amp = span.waveAmp() != null ? span.waveAmp() : 0f;
                    float spd = span.waveSpeed() != null ? span.waveSpeed() : 2.5f;
                    xOff += net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.waveXOffsetPx(nowMs, globalIndex, amp, spd);
                }
                if (span.shakeAmp() != null) {
                    xOff += net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.shakeXOffsetPx(nowMs, globalIndex, span.shakeAmp());
                }

                if (span.glitchIntensity() != null && net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.glitchActive(nowMs, globalIndex, span.glitchIntensity())) {
                    xOff += net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.glitchJitterX(nowMs, globalIndex, span.glitchIntensity());
                    yOff += net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.glitchJitterY(nowMs, globalIndex, span.glitchIntensity());
                    rgb = net.pixeldreamstudios.rpgsystems.client.title.TitleStyleUtil.glitchTintRgb(nowMs, globalIndex, rgb, span.glitchIntensity());
                }

                int argb = ((Math.round(alpha * 255f) & 0xFF) << 24) | (rgb & 0xFFFFFF);

                tr.draw(ch,
                        baseX + advanceX + xOff,
                        yOff,
                        argb,
                        false, matrices.peek().getPositionMatrix(),
                        consumers, TextRenderer.TextLayerType.NORMAL, 0, light);

                advanceX += cw;
                globalIndex++;
                localIndex++;
                i += Character.charCount(cp);
            }
        }

        matrices.pop();
    }

    private static boolean isOnScreen(WorldRenderContext context, Box box) {
        var frustum = context.frustum();
        return frustum == null || frustum.isVisible(box);
    }

    private static void faceCamera(MatrixStack matrices, Camera camera) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static double lerp(float t, double a, double b) { return a + (b - a) * t; }
}
