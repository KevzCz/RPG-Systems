package net.pixeldreamstudios.rpgsystems.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class MixinEntityGlow {
    @ModifyReturnValue(method = "isGlowing", at = @At("RETURN"))
    private boolean rpgsystems$glowPartyMembers(boolean original) {
        return original || ClientPartyHighlighter.shouldGlow((Entity)(Object)this);
    }
}
