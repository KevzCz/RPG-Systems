package net.pixeldreamstudios.rpgsystems.mixin;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.rpgsystems.pet.PetOwnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityPetMixin implements PetOwnable {

	@Unique
	private boolean rpgsystems$pet = false;
	@Unique private UUID rpgsystems$owner = null;
	@Unique private String rpgsystems$name = "";

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void rpgsystems$read(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("RPGS_Pet")) this.rpgsystems$pet = nbt.getBoolean("RPGS_Pet");
		if (nbt.containsUuid("RPGS_Owner")) this.rpgsystems$owner = nbt.getUuid("RPGS_Owner");
		if (nbt.contains("RPGS_Name")) this.rpgsystems$name = nbt.getString("RPGS_Name");
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void rpgsystems$write(NbtCompound nbt, CallbackInfo ci) {
		nbt.putBoolean("RPGS_Pet", this.rpgsystems$pet);
		if (this.rpgsystems$owner != null) nbt.putUuid("RPGS_Owner", this.rpgsystems$owner);
		if (!this.rpgsystems$name.isEmpty()) nbt.putString("RPGS_Name", this.rpgsystems$name);
	}

	@Override public boolean rpgsystems$isPet() { return rpgsystems$pet; }
	@Override public void rpgsystems$setPet(boolean pet) { this.rpgsystems$pet = pet; }
	@Override public UUID rpgsystems$getOwnerUuid() { return rpgsystems$owner; }
	@Override public void rpgsystems$setOwnerUuid(UUID owner) { this.rpgsystems$owner = owner; }
	@Override public String rpgsystems$getPetName() { return rpgsystems$name; }
	@Override public void rpgsystems$setPetName(String name) { this.rpgsystems$name = name == null ? "" : name; }
}
