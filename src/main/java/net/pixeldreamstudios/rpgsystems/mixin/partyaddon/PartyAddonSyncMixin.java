package net.pixeldreamstudios.rpgsystems.mixin.partyaddon;

import net.minecraft.server.network.ServerPlayerEntity;
import net.partyaddon.group.GroupManager;
import net.partyaddon.network.PartyAddonServerPacket;
import net.pixeldreamstudios.rpgsystems.party.PartyAddonIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(value = PartyAddonServerPacket.class, remap = false)
public abstract class PartyAddonSyncMixin {

    @Inject(
            method = "writeS2CSyncGroupManagerPacket",
            at = @At("TAIL"),
            remap = false
    )
    private static void rpgsystems$onGroupSync(ServerPlayerEntity player, GroupManager groupManager, CallbackInfo ci) {
        if (!PartyAddonIntegration.isEnabled()) {
            return;
        }
        
        PartyAddonIntegration.onPartyAddonSync(player);
    }
}
