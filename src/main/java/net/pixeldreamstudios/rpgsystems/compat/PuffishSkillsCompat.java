package net.pixeldreamstudios.rpgsystems.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

public final class PuffishSkillsCompat {
    private static final boolean IS_LOADED = FabricLoader.getInstance().isModLoaded("puffish_skills");

    private PuffishSkillsCompat() {}

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    public static boolean categoryUnlocked(ServerPlayerEntity player, Identifier category) {
        return IS_LOADED && SkillsAPI.getCategory(category).map(c -> c.isUnlocked(player)).orElse(false);
    }

    public static boolean skillUnlocked(ServerPlayerEntity player, Identifier category, String skill) {
        return IS_LOADED && SkillsAPI.getCategory(category)
                .flatMap(c -> c.getSkill(skill))
                .map(s -> s.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
    }
}
