package net.pixeldreamstudios.rpgsystems.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.pixeldreamstudios.rpgsystems.client.config.MiscClientConfig;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.DamageNumbersClientConfig;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.EnemyHudClientConfig;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.HealingNumbersClientConfig;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyMemberInfoClientConfig;
import net.pixeldreamstudios.rpgsystems.client.title.config.TitlesClientConfig;

@Environment(EnvType.CLIENT)
public final class ClientConfigLoad {
    private ClientConfigLoad() {}

    public static void init() {
        DamageNumbersClientConfig.get(); DamageNumbersClientConfig.save();
        EnemyHudClientConfig.get();       EnemyHudClientConfig.save();
        HealingNumbersClientConfig.get(); HealingNumbersClientConfig.save();

        PartyHudClientConfig.get();
        PartyMemberInfoClientConfig.get();
        TitlesClientConfig.get();
        MiscClientConfig.get();
    }

}
