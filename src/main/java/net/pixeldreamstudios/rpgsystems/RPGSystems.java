package net.pixeldreamstudios.rpgsystems;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.pixeldreamstudios.rpgsystems.compat.CriticalStrikeCompat;
import net.pixeldreamstudios.rpgsystems.compat.KevsLibraryCritCompat;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatNetServer;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.*;
import net.pixeldreamstudios.rpgsystems.party.*;
import net.pixeldreamstudios.rpgsystems.pet.PetCommands;
import net.pixeldreamstudios.rpgsystems.title.TitleCommands;
import net.pixeldreamstudios.rpgsystems.title.TitleConditionEvents;
import net.pixeldreamstudios.rpgsystems.title.data.TitlesDataReloader;
import net.pixeldreamstudios.rpgsystems.title.power.PowerInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public class RPGSystems implements ModInitializer {
	public static final String MOD_ID = "rpg-systems";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@Override
	public void onInitialize() {
		RPGSystemsConfig.load();
		SystemNet.registerServer();

		if (FabricLoader.getInstance().isModLoaded("ftbteams")) {
			FTBTeamsLoader.tryInitialize();
		} else {
			LOGGER.info("FTB Teams not found, using native party system");
		}

		if (FabricLoader.getInstance().isModLoaded("partyaddon")) {
			PartyAddonLoader.tryInitialize();
		} else {
			LOGGER.info("PartyAddon not found, skipping integration");
		}

		if (RPGSystemsConfig.get().systems.party) {
			PartyNet.initCommon();
		}

		if (FabricLoader.getInstance().isModLoaded("showmeyourbuild")) {
			ShowBuildCompatNetServer.initServer();
		}
		if (FabricLoader.getInstance().isModLoaded("critical_strike")) {
			CriticalStrikeCompat.init();
		}
		EnemyNet.initCommon();

		if (RPGSystemsConfig.get().systems.title) {
			ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new TitlesDataReloader());
			TitleNet.registerServer();
			TitleConditionEvents.register();
			PowerInit.init();
			TitlePowerNet.registerServer();
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			if (RPGSystemsConfig.get().systems.party) {
				PartyCommands.register(dispatcher);
			}
			if (RPGSystemsConfig.get().systems.pet) {
				PetCommands.register(dispatcher);
			}
			if (RPGSystemsConfig.get().systems.title) {
				TitleCommands.register(dispatcher);
			}
			
			dispatcher.register(literal("rpgsystems")
					.then(literal("party")
							.then(literal("debug")
									.executes(ctx -> debugPartyCommand(ctx)))));
		});
		KevsLibraryCritCompat.init();
		LOGGER.info("RPG Systems loaded. Enabled systems -> party={}, pet={}, title={}",
				RPGSystemsConfig.get().systems.party,
				RPGSystemsConfig.get().systems.pet,
				RPGSystemsConfig.get().systems.title
		);
	}

	private static int debugPartyCommand(CommandContext<ServerCommandSource> ctx) {
		ServerPlayerEntity player = ctx.getSource().getPlayer();
		if (player == null) return 0;

		player.sendMessage(Text.literal("=== RPG Systems Party Debug ===").formatted(Formatting.GOLD));

		player.sendMessage(Text.literal("Config:").formatted(Formatting.YELLOW));
		player.sendMessage(Text.literal("  useFTBTeams: " + RPGSystemsConfig.get().party.useFTBTeams).formatted(Formatting.GRAY));
		player.sendMessage(Text.literal("  usePartyAddon: " + RPGSystemsConfig.get().party.usePartyAddon).formatted(Formatting.GRAY));

		player.sendMessage(Text.literal("Integration Status:").formatted(Formatting.YELLOW));
		player.sendMessage(Text.literal("  FTBTeams loaded: " + FabricLoader.getInstance().isModLoaded("ftbteams")).formatted(Formatting.GRAY));
		player.sendMessage(Text.literal("  FTBTeams enabled: " + FTBTeamsLoader.isEnabled()).formatted(Formatting.GRAY));
		player.sendMessage(Text.literal("  PartyAddon loaded: " + FabricLoader.getInstance().isModLoaded("partyaddon")).formatted(Formatting.GRAY));
		player.sendMessage(Text.literal("  PartyAddon enabled: " + PartyAddonLoader.isEnabled()).formatted(Formatting.GRAY));

		if (FTBTeamsLoader.isEnabled()) {
			player.sendMessage(Text.literal("FTB Teams Data:").formatted(Formatting.YELLOW));
			PartyDataProvider.PartyInfo ftbData = FTBTeamsIntegration.getPartyInfoForPlayer(player);
			if (ftbData != null) {
				player.sendMessage(Text.literal("  Party: " + ftbData.name).formatted(Formatting.GREEN));
				player.sendMessage(Text.literal("  Members: " + ftbData.members.size()).formatted(Formatting.GRAY));
			} else {
				player.sendMessage(Text.literal("  Not in FTB team").formatted(Formatting.RED));
			}
		}

		if (PartyAddonLoader.isEnabled()) {
			player.sendMessage(Text.literal("PartyAddon Data:").formatted(Formatting.YELLOW));
			for (Text line : PartyAddonIntegration.debugLines(player)) {
				player.sendMessage(line);
			}
		}

		player.sendMessage(Text.literal("Available Sources:").formatted(Formatting.YELLOW));
		List<PartyDataProvider.PartySource> sources = PartyDataProvider.getAvailableSourcesForPlayer(player);
		if (sources.isEmpty()) {
			player.sendMessage(Text.literal("  (none)").formatted(Formatting.GRAY));
		} else {
			for (PartyDataProvider.PartySource src : sources) {
				player.sendMessage(Text.literal("  - " + src.name()).formatted(Formatting.GREEN));
			}
		}

		PartyDataProvider.PartySource effective = PartyDataProvider.getEffectiveSourceForPlayer(player);
		player.sendMessage(Text.literal("Effective Source: " + effective.name()).formatted(Formatting.AQUA));

		return 1;
	}
}