package net.pixeldreamstudios.rpgsystems.mixin.pet;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.rpgsystems.pet.PetOwnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityPetHookMixin {
    @Inject(method = "setOwnerUuid", at = @At("TAIL"))
    private void rpgsystems$onSetOwnerUuid(UUID ownerUuid, CallbackInfo ci) {
        PetOwnable p = (PetOwnable) this;
        p.rpgsystems$setOwnerUuid(ownerUuid);
        p.rpgsystems$setPet(ownerUuid != null);
    }

    @Inject(method = "setTame", at = @At("TAIL"))
    private void rpgsystems$onSetTame(boolean tame, CallbackInfo ci) {
        PetOwnable p = (PetOwnable) this;
        if (!tame) {
            p.rpgsystems$setOwnerUuid(null);
        }
        p.rpgsystems$setPet(tame);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void rpgsystems$adoptOnLoad(NbtCompound nbt, CallbackInfo ci) {
        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;
        PetOwnable p = (PetOwnable) this;
        p.rpgsystems$setOwnerUuid(self.getOwnerUuid());
        p.rpgsystems$setPet(self.isTame());
    }
}
