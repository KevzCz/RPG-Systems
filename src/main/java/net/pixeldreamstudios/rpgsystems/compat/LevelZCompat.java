package net.pixeldreamstudios.rpgsystems.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;


public final class LevelZCompat {
    private static final boolean IS_LOADED = FabricLoader.getInstance().isModLoaded("levelz");

    private LevelZCompat() {}

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    public static int getLevel(PlayerEntity player) {
        if (!IS_LOADED || player == null) return -1;

        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getOverallLevel();
                }
            }
        } catch (Throwable ignored) {

        }

        return -1;
    }


    public static int getLevel(ServerPlayerEntity player) {
        return getLevel((PlayerEntity) player);
    }
    public static int getSkillPoints(PlayerEntity player) {
        if (!IS_LOADED || player == null) return -1;

        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getSkillPoints();
                }
            }
        } catch (Throwable ignored) {
        }

        return -1;
    }

    public static float getLevelProgress(PlayerEntity player) {
        if (!IS_LOADED || player == null) return -1.0f;

        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getLevelProgress();
                }
            }
        } catch (Throwable ignored) {
        }

        return -1.0f;
    }

    @Nullable
    public static LevelManager getLevelManager(PlayerEntity player) {
        if (!IS_LOADED || player == null) return null;

        try {
            if (player instanceof LevelManagerAccess access) {
                return access.getLevelManager();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }


    public static boolean isMaxLevel(PlayerEntity player) {
        if (!IS_LOADED || player == null) return false;

        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.isMaxLevel();
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    public static int getNextLevelExperience(PlayerEntity player) {
        if (!IS_LOADED || player == null) return -1;

        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getNextLevelExperience();
                }
            }
        } catch (Throwable ignored) {
        }

        return -1;
    }
}