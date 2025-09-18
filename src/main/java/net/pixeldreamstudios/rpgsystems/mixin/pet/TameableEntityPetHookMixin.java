package net.pixeldreamstudios.rpgsystems.mixin.pet;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.rpgsystems.pet.PetOwnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(TameableEntity.class)
public abstract class TameableEntityPetHookMixin {
    @Inject(method = "setOwnerUuid", at = @At("TAIL"))
    private void rpgsystems$onSetOwnerUuid(UUID ownerUuid, CallbackInfo ci) {
        PetOwnable p = (PetOwnable) this;
        p.rpgsystems$setOwnerUuid(ownerUuid);
        p.rpgsystems$setPet(ownerUuid != null);
    }

    @Inject(method = "setTamed(ZZ)V", at = @At("TAIL"))
    private void rpgsystems$onSetTamed(boolean tamed, boolean updateAttributes, CallbackInfo ci) {
        PetOwnable p = (PetOwnable) this;
        if (!tamed) {
            p.rpgsystems$setOwnerUuid(null);
        }
        p.rpgsystems$setPet(tamed);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void rpgsystems$adoptOnLoad(NbtCompound nbt, CallbackInfo ci) {
        TameableEntity self = (TameableEntity) (Object) this;
        PetOwnable p = (PetOwnable) this;
        p.rpgsystems$setOwnerUuid(self.getOwnerUuid());
        p.rpgsystems$setPet(self.isTamed());
    }
}
