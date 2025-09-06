package net.pixeldreamstudios.rpgsystems.client.title;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class TitleTextureResolver {
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

    private TitleTextureResolver() {}

    public static void clear() {}

    public static FrameInfo currentFrame(Identifier titleId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        Sprite sprite = client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(spriteIdFor(titleId));
        if (sprite == null) return null;

        Identifier missingId = MissingSprite.getMissingSpriteId();
        Identifier actualId = sprite.getContents().getId();
        if (actualId.equals(missingId)) return null;

        int frameW = sprite.getContents().getWidth();
        int frameH = sprite.getContents().getHeight();

        float u0 = sprite.getMinU();
        float v0 = sprite.getMinV();
        float u1 = sprite.getMaxU();
        float v1 = sprite.getMaxV();

        return new FrameInfo(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, frameW, frameH, u0, v0, u1, v1);
    }

    private static Identifier spriteIdFor(Identifier titleId) {
        return Identifier.of(titleId.getNamespace(), "block/title/" + titleId.getPath());
    }
}
