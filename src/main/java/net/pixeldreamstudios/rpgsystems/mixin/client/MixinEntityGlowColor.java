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
public abstract class MixinEntityGlowColor {
    @ModifyReturnValue(method = "getTeamColorValue", at = @At("RETURN"))
    private int rpgsystems$partyGlowColor(int original) {
        Entity self = (Entity)(Object)this;
        if (ClientPartyHighlighter.shouldGlow(self)) {
            return ClientPartyHighlighter.getColor(self);
        }
        return original;
    }
}
