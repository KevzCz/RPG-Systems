package net.pixeldreamstudios.rpgsystems.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;
import net.pixeldreamstudios.rpgsystems.party.Party;
import net.pixeldreamstudios.rpgsystems.party.PartyAllies;
import net.pixeldreamstudios.rpgsystems.party.PartyPersistentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityCollisionServerMixin {

    private static boolean samePartyAndIgnore(Entity a, Entity b) {
        if (a == null || b == null) return false;

        var server = a.getServer();
        if (server == null) return false;

        UUID ownerA = PartyAllies.owningPlayerUuid(a);
        UUID ownerB = PartyAllies.owningPlayerUuid(b);

        if (ownerA == null || ownerB == null) return false;

        if (!PartyAllies.sameParty(server, ownerA, ownerB)) return false;

        if (FabricLoader.getInstance().isModLoaded("ftbteams") && FTBTeamsIntegration.isEnabled()) {

            if (a instanceof ServerPlayerEntity pa) {
                var ftbData = net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration.getPartyDataForPlayer(pa);
                if (ftbData != null) {
                    return ftbData.settings.ignorePartyCollision;
                }
            } else if (b instanceof ServerPlayerEntity pb) {
                var ftbData = net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration.getPartyDataForPlayer(pb);
                if (ftbData != null) {
                    return ftbData.settings.ignorePartyCollision;
                }
            }

            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerA);
            if (owner != null) {
                var ftbData = net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration.getPartyDataForPlayer(owner);
                if (ftbData != null) {
                    return ftbData.settings.ignorePartyCollision;
                }
            }

            return false;
        }

        PartyPersistentState state = PartyPersistentState.get(server);
        Party party = state.getPartyByMember(ownerA);
        if (party == null) return false;

        return party.settings.ignorePartyCollision;
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