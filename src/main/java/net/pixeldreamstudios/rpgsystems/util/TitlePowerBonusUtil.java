package net.pixeldreamstudios.rpgsystems.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.power.PowerRegistry;

import java.util.List;

public final class TitlePowerBonusUtil {

    private TitlePowerBonusUtil() {}

    public static void installTitlePowers(ServerPlayerEntity player, Identifier titleId, List<Identifier> powers) {
        for (Identifier pid : powers) {
            PowerRegistry.activate(pid, player);
        }
        }

    public static void uninstallTitlePowers(ServerPlayerEntity player, Title t) {
        for (Title.Bonus b : t.bonuses) {
            if (b.powerId != null && b.powerId.isPresent()) {
                PowerRegistry.deactivate(b.powerId.get(), player);
            }
        }
        }
}
