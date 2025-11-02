package net.pixeldreamstudios.rpgsystems.mixin.ftbteams;

import dev.ftb.mods.ftbteams.data.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
            method = "sendMessage(Ljava/util/UUID;Lnet/minecraft/text/Text;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void rpgsystems$onSendMessage(UUID from, Text text, CallbackInfo ci) {
        if (!FTBTeamsIntegration.isEnabled()) {
            return;
        }

        AbstractTeam team = (AbstractTeam) (Object) this;
        if (!team.isPartyTeam()) {
            return;
        }

        String message = text.getString();

        for (ServerPlayerEntity player : team.getOnlineMembers()) {
            FTBTeamsChatBridge.forwardToRPGSystems(
                    player.getServer(),
                    team,
                    from,
                    message
            );
            break;
        }
    }
}