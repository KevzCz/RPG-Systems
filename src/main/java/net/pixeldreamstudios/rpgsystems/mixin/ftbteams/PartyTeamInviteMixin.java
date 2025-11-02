package net.pixeldreamstudios.rpgsystems.mixin.ftbteams;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.network.party.PartyInvitePayloads;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.UUID;

@Mixin(value = PartyTeam.class, remap = false)
public abstract class PartyTeamInviteMixin {

    @Shadow(remap = false)
    protected TeamManagerImpl manager;

    @Inject(
            method = "invite",
            at = @At("TAIL"),
            remap = false
    )
    private void rpgsystems$onInvite(
            ServerPlayerEntity inviter,
            Collection<GameProfile> profiles,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!FTBTeamsIntegration.isEnabled()) return;

        PartyTeam team = (PartyTeam) (Object) this;
        String partyName = team.getProperty(TeamProperties.DISPLAY_NAME);
        UUID partyId = team.getId();
        UUID leaderUuid = team.getOwner();
        String leaderName = inviter.getName().getString();

        for (GameProfile profile : profiles) {
            ServerPlayerEntity invitee = this.manager.getServer().getPlayerManager().getPlayer(profile.getId());
            if (invitee != null && !invitee.getUuid().equals(inviter.getUuid())) {
                ServerPlayNetworking.send(invitee,
                        new PartyInvitePayloads.InviteAdded(partyId, leaderUuid, leaderName, partyName));
            }
        }
    }
}