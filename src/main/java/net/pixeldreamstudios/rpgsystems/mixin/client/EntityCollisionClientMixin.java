package net.pixeldreamstudios.rpgsystems.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.pixeldreamstudios.rpgsystems.client.party.ClientPartyHudData;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityCollisionClientMixin {

    private static boolean samePartyAndIgnoreClient(Entity a, Entity b) {
        if (a == null || b == null) return false;

        if (!ClientPartyHudData.ignorePartyCollision()) return false;
        UUID ownerA = PartyAllies.owningPlayerUuid(a);
        UUID ownerB = PartyAllies.owningPlayerUuid(b);

        if (ownerA == null || ownerB == null) return false;

        return ClientPartyHudData.isSameParty(ownerA, ownerB);
    }

    @Inject(
            method = "collidesWith(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rpg$noPartyCollision_collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self.getWorld().isClient && samePartyAndIgnoreClient(self, other)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rpg$noPartyCollision_pushAwayFrom(Entity other, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (self.getWorld().isClient && samePartyAndIgnoreClient(self, other)) {
            ci.cancel();
        }
    }
}