package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.party.Party;
import net.pixeldreamstudios.rpgsystems.party.PartyPersistentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionServerMixin {

    private static boolean samePartyAndIgnore(Entity a, Entity b) {
        if (!(a instanceof ServerPlayerEntity pa) || !(b instanceof ServerPlayerEntity pb)) return false;
        var server = pa.getServer();
        if (server == null) return false;

        PartyPersistentState state = PartyPersistentState.get(server);
        Party paParty = state.getPartyByMember(pa.getUuid());
        if (paParty == null) return false;
        if (!paParty.members.contains(pb.getUuid())) return false;

        return paParty.settings.ignorePartyCollision;
    }

    @Inject(
            method = "collidesWith(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rpg$noPartyCollision_collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (!self.getWorld().isClient && samePartyAndIgnore(self, other)) {
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
        if (!self.getWorld().isClient && samePartyAndIgnore(self, other)) {
            ci.cancel();
        }
    }
}