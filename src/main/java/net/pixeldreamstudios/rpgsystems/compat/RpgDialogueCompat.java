package net.pixeldreamstudios.rpgsystems.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerDataContainer;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueManager;
import net.pixeldreamstudios.rpgerrands.errand.Errand;
import net.pixeldreamstudios.rpgerrands.errand.Errands;
import net.pixeldreamstudios.rpgerrands.errand.Task;
import net.pixeldreamstudios.rpgvillagers.village.Standing;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class RpgDialogueCompat {
    private static final boolean DIALOGUE_LOADED = FabricLoader.getInstance().isModLoaded("rpg_dialogue");
    private static final boolean ERRANDS_LOADED = DIALOGUE_LOADED && FabricLoader.getInstance().isModLoaded("rpg_errands");
    private static final boolean VILLAGERS_LOADED = DIALOGUE_LOADED && FabricLoader.getInstance().isModLoaded("rpg_villagers");

    public record ErrandInfo(Identifier errand, Optional<Identifier> kind, List<Identifier> professions) {
    }

    private RpgDialogueCompat() {}

    public static boolean isDialogueLoaded() {
        return DIALOGUE_LOADED;
    }

    public static boolean isErrandsLoaded() {
        return ERRANDS_LOADED;
    }

    public static boolean isVillagersLoaded() {
        return VILLAGERS_LOADED;
    }

    public static boolean dialogueCompleted(ServerPlayerEntity player, Identifier dialogue) {
        return DIALOGUE_LOADED && DialogueManager.hasCompleted(player, dialogue);
    }

    public static boolean standingAtLeast(ServerPlayerEntity player, String wanted) {
        if (!VILLAGERS_LOADED) return false;
        Standing target;
        try {
            target = Standing.valueOf(wanted.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
        Standing current = Standing.of(player, player);
        return current != null && current.ordinal() >= target.ordinal();
    }

    public static ErrandInfo errandInfo(ServerPlayerEntity player, UUID giver, Identifier errandId) {
        Errand errand = ERRANDS_LOADED ? Errands.get(errandId) : null;
        Optional<Identifier> kind = errand == null ? Optional.empty() : Optional.ofNullable(Task.IDS.get(errand.task().codec()));
        Entity giverEntity = player.getServerWorld().getEntity(giver);
        List<Identifier> professions = giverEntity instanceof VillagerDataContainer holder
                ? List.of(Registries.VILLAGER_PROFESSION.getId(holder.getVillagerData().getProfession()))
                : List.of();
        return new ErrandInfo(errandId, kind, professions);
    }
}
