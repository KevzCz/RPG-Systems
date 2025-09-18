package net.pixeldreamstudios.rpgsystems.mixin.pet;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.rpgsystems.pet.PetOwnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityPetMixin implements PetOwnable {
	@Unique private static final TrackedData<Boolean> RPGS_IS_PET =
			DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	@Unique private static final TrackedData<Optional<UUID>> RPGS_OWNER =
			DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	@Unique private static final TrackedData<String> RPGS_PET_NAME =
			DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.STRING);

	@Inject(method = "initDataTracker", at = @At("TAIL"))
	private void rpgsystems$initDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
		builder.add(RPGS_IS_PET, false);
		builder.add(RPGS_OWNER, Optional.empty());
		builder.add(RPGS_PET_NAME, "");
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void rpgsystems$read(NbtCompound nbt, CallbackInfo ci) {
		DataTracker dt = ((MobEntity) (Object) this).getDataTracker();

		if (nbt.contains("RPGS_Pet")) {
			dt.set(RPGS_IS_PET, nbt.getBoolean("RPGS_Pet"));
		}
		if (nbt.containsUuid("RPGS_Owner")) {
			dt.set(RPGS_OWNER, Optional.of(nbt.getUuid("RPGS_Owner")));
		} else {
			dt.set(RPGS_OWNER, Optional.empty());
		}
		if (nbt.contains("RPGS_Name", 8)) {
			dt.set(RPGS_PET_NAME, nbt.getString("RPGS_Name"));
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void rpgsystems$write(NbtCompound nbt, CallbackInfo ci) {
		DataTracker dt = ((MobEntity) (Object) this).getDataTracker();

		if (dt.get(RPGS_IS_PET)) {
			nbt.putBoolean("RPGS_Pet", true);
		}
		Optional<UUID> owner = dt.get(RPGS_OWNER);
		owner.ifPresent(uuid -> nbt.putUuid("RPGS_Owner", uuid));

		String name = dt.get(RPGS_PET_NAME);
		if (!name.isEmpty()) {
			nbt.putString("RPGS_Name", name);
		}
	}

	@Override
	public boolean rpgsystems$isPet() {
		return ((MobEntity) (Object) this).getDataTracker().get(RPGS_IS_PET);
	}

	@Override
	public void rpgsystems$setPet(boolean pet) {
		((MobEntity) (Object) this).getDataTracker().set(RPGS_IS_PET, pet);
	}

	@Override
	public UUID rpgsystems$getOwnerUuid() {
		return ((MobEntity) (Object) this).getDataTracker().get(RPGS_OWNER).orElse(null);
	}

	@Override
	public void rpgsystems$setOwnerUuid(UUID owner) {
		((MobEntity) (Object) this).getDataTracker().set(RPGS_OWNER, Optional.ofNullable(owner));
	}

	@Override
	public String rpgsystems$getPetName() {
		return ((MobEntity) (Object) this).getDataTracker().get(RPGS_PET_NAME);
	}

	@Override
	public void rpgsystems$setPetName(String name) {
		((MobEntity) (Object) this).getDataTracker().set(RPGS_PET_NAME, name == null ? "" : name);
	}
}
