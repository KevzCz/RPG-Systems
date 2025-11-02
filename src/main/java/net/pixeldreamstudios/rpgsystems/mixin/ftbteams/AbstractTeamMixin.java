package net.pixeldreamstudios.rpgsystems.mixin.ftbteams;

import dev.ftb.mods.ftbteams.data.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsChatBridge;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = AbstractTeam.class, remap = false)
public abstract class AbstractTeamMixin {

    @Inject(
            method = "sendMessage(Ljava/util/UUID;Ljava/lang/String;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void rpgsystems$onSendMessage(UUID senderId, String message, CallbackInfo ci) {
        if (!FTBTeamsIntegration.isEnabled()) {
            return;
        }

        AbstractTeam team = (AbstractTeam) (Object) this;
        if (!team.isPartyTeam()) {
            return;
        }

        for (ServerPlayerEntity player : team.getOnlineMembers()) {
            FTBTeamsChatBridge.forwardToRPGSystems(
                    player.getServer(),
                    team,
                    senderId,
                    message
            );
            break;
        }
    }
}