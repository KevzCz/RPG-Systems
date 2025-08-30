package net.pixeldreamstudios.rpgsystems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatNet;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.network.PartyNet;
import net.pixeldreamstudios.rpgsystems.network.TitleNet;
import net.pixeldreamstudios.rpgsystems.party.PartyCommands;
import net.pixeldreamstudios.rpgsystems.pet.PetCommands;
import net.pixeldreamstudios.rpgsystems.title.TitleCommands;
import net.pixeldreamstudios.rpgsystems.title.data.TitlesDataReloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPGSystems implements ModInitializer {
	public static final String MOD_ID = "rpg-systems";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		RPGSystemsConfig.load();

		if (RPGSystemsConfig.get().systems.party) {
			PartyNet.initCommon();
		}

		if (FabricLoader.getInstance().isModLoaded("showmeyourbuild")) {
			ShowBuildCompatNet.initCommon();
		}

		EnemyNet.initCommon();

		if (RPGSystemsConfig.get().systems.title) {
			ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new TitlesDataReloader());
			TitleNet.registerServer();
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
		});

		LOGGER.info("RPG Systems loaded. Enabled systems -> party={}, pet={}, title={}",
				RPGSystemsConfig.get().systems.party,
				RPGSystemsConfig.get().systems.pet,
				RPGSystemsConfig.get().systems.title
		);
	}
}
