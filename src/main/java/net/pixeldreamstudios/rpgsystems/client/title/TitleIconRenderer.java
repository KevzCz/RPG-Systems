package net.pixeldreamstudios.rpgsystems.client.title;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.pixeldreamstudios.rpgsystems.title.Title;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class TitleIconRenderer {
    private static final Map<Identifier, ItemStack> ITEM_CACHE = new HashMap<>();
    private static final Map<Identifier, LivingEntity> ENTITY_CACHE = new HashMap<>();

    private TitleIconRenderer() {}

    public static boolean hasIcon(Title.Condition c) {
        if (c.item.isPresent() || c.block.isPresent()) return true;
        Identifier mobId = resolveEntityId(c);
        return mobId != null;
    }

    public static boolean renderForCondition(DrawContext ctx, Title.Condition c, int x, int y, int sizePx) {
        if (sizePx <= 0) return false;

        if (c.item.isPresent()) {
            return drawItem(ctx, c.item.get(), x, y, sizePx);
        }

        if (c.block.isPresent()) {
            Identifier id = c.block.get();
            Item blockItem = Registries.BLOCK.get(id).asItem();
            if (blockItem != null) {
                Identifier itemId = Registries.ITEM.getId(blockItem);
                return drawItem(ctx, itemId, x, y, sizePx);
            }
        }

        Identifier mobId = resolveEntityId(c);
        if (mobId != null) {
            return drawEntityIcon(ctx, mobId, x, y, sizePx);
        }

        return false;
    }

    public static void drawMob(DrawContext context, int x, int y, int scale, int mouseX, int mouseY, LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 100.0);
        matrices.scale(scale, -scale, scale);

        float yOff = 0.5f + (entity.getHeight() * 0.5f);
        matrices.translate(0.0, -yOff, 0.0);

        try {
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrices, context.getVertexConsumers(), 0xF000F0);
        } catch (Throwable t) {
            TextRenderer renderer = client.textRenderer;
            int textWidth = renderer.getWidth("Can't render mob");
            context.drawTextWithShadow(renderer, Text.literal("Can't render mob"), x - textWidth / 2, y - 10, 0xFF5555);
        } finally {
            matrices.pop();
        }
    }


    private static boolean drawItem(DrawContext ctx, Identifier itemId, int x, int y, int sizePx) {
        if (!Registries.ITEM.containsId(itemId)) return false;

        ItemStack stack = ITEM_CACHE.computeIfAbsent(itemId, id -> new ItemStack(Registries.ITEM.get(id)));
        if (stack.isEmpty()) return false;

        float s = sizePx / 16.0f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(s, s, 1.0f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();
        return true;
    }

    private static boolean drawEntityIcon(DrawContext ctx, Identifier entityTypeId, int x, int y, int sizePx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return false;
        if (!Registries.ENTITY_TYPE.containsId(entityTypeId)) return false;

        LivingEntity entity = ENTITY_CACHE.get(entityTypeId);
        if (entity == null) {
            EntityType<?> et = Registries.ENTITY_TYPE.get(entityTypeId);
            var created = et.create(mc.world);
            if (!(created instanceof LivingEntity)) return false;
            entity = (LivingEntity) created;
            ENTITY_CACHE.put(entityTypeId, entity);
        }

        int cx = x + Math.round(sizePx * 0.5f);
        int cy = y + Math.round(sizePx * 0.95f);

        float scaleDivisor = 4.0f;
        float scaleBoost = 1.0f;
        int scale = Math.max(1, Math.round((sizePx / scaleDivisor) * scaleBoost));

        drawMob(ctx, cx, cy, scale, 0, 0, entity);
        return true;
    }

    public static boolean renderEntityPreview(DrawContext ctx, Identifier entityTypeId, int x, int y, int sizePx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return false;

        LivingEntity entity = getPreviewEntity(entityTypeId);
        if (entity == null) return false;

        int cx = x + Math.round(sizePx * 0.5f);
        int cy = y + Math.round(sizePx * 0.5f);
        int scale = Math.max(1, Math.round(sizePx / 4.0f));

        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        float t = mc.world.getTime() + tickDelta;
        float angle = (t * 2.5f) % 360.0f;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 120.0);
        m.scale(scale, -scale, scale);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

        float yOff = 0.5f + (entity.getHeight() * 0.5f);
        m.translate(0.0, -yOff, 0.0);

        try {
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, m, ctx.getVertexConsumers(), 0xF000F0);
        } catch (Throwable ignored) {
            TextRenderer r = mc.textRenderer;
            int w = r.getWidth("Can't render mob");
            ctx.drawTextWithShadow(r, Text.literal("Can't render mob"), cx - w / 2, cy - scale * 12, 0xFF5555);
        } finally {
            m.pop();
        }
        return true;
    }
    public static LivingEntity getPreviewEntity(Identifier entityTypeId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return null;
        if (!Registries.ENTITY_TYPE.containsId(entityTypeId)) return null;

        LivingEntity entity = ENTITY_CACHE.get(entityTypeId);
        if (entity == null) {
            EntityType<?> et = Registries.ENTITY_TYPE.get(entityTypeId);
            var created = et.create(mc.world);
            if (!(created instanceof LivingEntity)) return null;
            entity = (LivingEntity) created;
            ENTITY_CACHE.put(entityTypeId, entity);
        }
        return entity;
    }


    private static Identifier resolveEntityId(Title.Condition c) {
        if (c.entityType.isPresent()) return c.entityType.get();
        if (c.entitySpec.isPresent()) {
            Identifier maybe = Identifier.tryParse(c.entitySpec.get());
            if (maybe != null && Registries.ENTITY_TYPE.containsId(maybe)) return maybe;
        }
        return null;
    }
}
