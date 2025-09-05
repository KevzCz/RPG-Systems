package net.pixeldreamstudios.rpgsystems.client.title;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class TitleTextureResolver {
    private static final float HALF_PIXEL_INSET = 0.5f;
    public static final class FrameInfo {
        public final Identifier textureId;
        public final int frameWidth;
        public final int frameHeight;
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;

        public FrameInfo(Identifier textureId, int frameWidth, int frameHeight, float u0, float v0, float u1, float v1) {
            this.textureId = textureId;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }

    private static final class Meta {
        final Identifier textureId;
        final int imageWidth;
        final int imageHeight;
        final int frameWidth;
        final int frameHeight;

        Meta(Identifier textureId, int imageWidth, int imageHeight, int frameWidth, int frameHeight) {
            this.textureId = textureId;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
        }
    }
    private static final Map<Identifier, Meta> CACHE = new HashMap<>();
    private TitleTextureResolver() {}
    public static void clear() {
        CACHE.clear();
    }

    public static FrameInfo currentFrame(Identifier titleId) {
        Meta meta = CACHE.computeIfAbsent(titleId, TitleTextureResolver::loadMeta);
        if (meta == null) return null;

        int texW = meta.imageWidth;
        int texH = meta.imageHeight;

        float epsU = texW > 0 ? HALF_PIXEL_INSET / texW : 0f;
        float epsV = texH > 0 ? HALF_PIXEL_INSET / texH : 0f;

        boolean verticalStrip   = meta.frameWidth  == texW && meta.frameHeight < texH && texH % meta.frameHeight == 0;
        boolean horizontalStrip = meta.frameHeight == texH && meta.frameWidth  < texW && texW % meta.frameWidth  == 0;

        int frameW = meta.frameWidth;
        int frameH = meta.frameHeight;

        float u0, u1, v0, v1;
        if (verticalStrip) {
            int topPx = 0;
            int botPx = frameH;
            u0 = 0.0f + epsU;
            u1 = 1.0f - epsU;
            v1 = 1.0f - (topPx / (float) texH) - epsV;
            v0 = 1.0f - (botPx / (float) texH) + epsV;
        } else if (horizontalStrip) {
            int leftPx  = 0;
            int rightPx = frameW;
            u0 = (leftPx  / (float) texW) + epsU;
            u1 = (rightPx / (float) texW) - epsU;
            v0 = 0.0f + epsV;
            v1 = 1.0f - epsV;
        } else {
            u0 = 0.0f + epsU;
            u1 = 1.0f - epsU;
            v0 = 0.0f + epsV;
            v1 = 1.0f - epsV;
        }

        return new FrameInfo(meta.textureId, frameW, frameH, u0, v0, u1, v1);
    }

    private static Meta loadMeta(Identifier titleId) {
        try {
            String ns = titleId.getNamespace();
            String path = titleId.getPath();
            Identifier png    = Identifier.of(ns, "textures/title/" + path + ".png");
            Identifier mcmeta = Identifier.of(ns, "textures/title/" + path + ".png.mcmeta");

            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();

            Optional<Resource> resOpt = rm.getResource(png);
            if (resOpt.isEmpty()) return null;

            int imgW;
            int imgH;
            try (var is = resOpt.get().getInputStream(); var image = NativeImage.read(is)) {
                imgW = image.getWidth();
                imgH = image.getHeight();
            }

            Integer metaWidth  = null;
            Integer metaHeight = null;

            Optional<Resource> metaOpt = rm.getResource(mcmeta);
            if (metaOpt.isPresent()) {
                try (var reader = new InputStreamReader(metaOpt.get().getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("animation")) {
                        JsonObject anim = root.getAsJsonObject("animation");
                        if (anim.has("width"))  metaWidth  = Math.max(1, anim.get("width").getAsInt());
                        if (anim.has("height")) metaHeight = Math.max(1, anim.get("height").getAsInt());
                    }
                }
            }

            int frameW = metaWidth != null ? metaWidth : imgW;
            int frameH;

            if (metaHeight != null) {
                frameH = metaHeight;
            } else if (imgH > imgW && imgH % imgW == 0) {
                frameH = imgW;
                frameW = imgW;
            } else if (imgW > imgH && imgW % imgH == 0) {
                frameH = imgH;
                frameW = imgH;
            } else {
                frameH = imgH;
                frameW = imgW;
            }

            return new Meta(png, imgW, imgH, frameW, frameH);
        } catch (Exception e) {
            return null;
        }
    }
}
