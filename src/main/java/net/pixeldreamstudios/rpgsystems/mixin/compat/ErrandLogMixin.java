package net.pixeldreamstudios.rpgsystems.mixin.compat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgerrands.errand.ErrandLog;
import net.pixeldreamstudios.rpgsystems.compat.RpgDialogueCompat;
import net.pixeldreamstudios.rpgsystems.title.TitleConditionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(value = ErrandLog.class, remap = false)
public abstract class ErrandLogMixin {

    @Inject(method = "settle", at = @At("HEAD"), require = 0)
    private static void rpgsystems$errandSettled(ServerPlayerEntity player, UUID giver, Identifier errand, CallbackInfo ci) {
        TitleConditionEvents.onErrandSettled(player, RpgDialogueCompat.errandInfo(player, giver, errand));
    }
}
