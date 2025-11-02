package net.pixeldreamstudios.rpgsystems.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T rpgsystems$addDrawableChild(T child);

    @Invoker("addDrawable")
    <T extends Drawable> T rpgsystems$addDrawable(T drawable);

    @Invoker("remove")
    void rpgsystems$remove(Element child);
}
