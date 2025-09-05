package net.pixeldreamstudios.rpgsystems.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.DamageNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.EnemyHealthBarRenderer;
import net.pixeldreamstudios.rpgsystems.client.enemy.HealingNumbersRenderer;
import net.pixeldreamstudios.rpgsystems.client.party.*;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;
import net.pixeldreamstudios.rpgsystems.client.party.hud.PartyHud;
import net.pixeldreamstudios.rpgsystems.client.party.hud.PartyInviteHud;
import net.pixeldreamstudios.rpgsystems.client.party.hud.PartyInviteInventoryUi;
import net.pixeldreamstudios.rpgsystems.client.party.hud.PartyJoinRequestHud;
import net.pixeldreamstudios.rpgsystems.client.party.screen.PartyScreen;
import net.pixeldreamstudios.rpgsystems.client.title.PlayerTitleRenderer;
import net.pixeldreamstudios.rpgsystems.client.title.TitlePowersClient;
import net.pixeldreamstudios.rpgsystems.client.title.TitleTextureResolver;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatNet;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.network.TitleNet;
import net.pixeldreamstudios.rpgsystems.network.TitlePowerNet;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class RPGSystemsClient implements ClientModInitializer {
    private static KeyBinding openPartyScreen;
    private static KeyBinding highlightParty;
    private static KeyBinding togglePartyHud;

    @Override
    public void onInitializeClient() {
        PartyHudClientConfig.load();

        ClientPartyInvites.initClientReceivers();
        ClientPartyHudData.initClientReceivers();
        ClientPartyChat.initClientReceivers();
        ClientPartyJoinRequests.initClientReceivers();

        PartyHud.init();
        PartyInviteHud.init();
        PartyInviteInventoryUi.init();
        PartyJoinRequestHud.init();
        PartyHighlighter.init();

        ShowBuildCompatNet.initClient();

        EnemyNet.initClient();

        EnemyHealthBarRenderer.init();
        DamageNumbersRenderer.init();
        HealingNumbersRenderer.init();

        if (RPGSystemsConfig.get().systems.title) {
            TitleNet.registerClient();
            PlayerTitleRenderer.init();
            TitlePowerNet.registerClient();
            TitlePowersClient.init();
        }

        openPartyScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rpgsystems.open_party",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.categories.multiplayer"
        ));
        highlightParty = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rpgsystems.highlight_party",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.multiplayer"
        ));
        togglePartyHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rpgsystems.toggle_party_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.categories.multiplayer"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPartyScreen.wasPressed()) {
                if (ClientPartyHudData.partyId != null) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.currentScreen == null) mc.setScreen(new PartyScreen());
                }
            }
            while (highlightParty.wasPressed()) {
                PartyHighlighter.toggle();
            }
            while (togglePartyHud.wasPressed()) {
                var cfg = PartyHudClientConfig.get();
                cfg.hudEnabled = !cfg.hudEnabled;
                PartyHudClientConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Party HUD " + (cfg.hudEnabled ? "enabled" : "disabled")));
                }
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPartyHudData.clearAll();
            ClientPartyInvites.clearAll();
            ClientPartyJoinRequests.clearAll();
            ClientPartyChat.clearAll();
            ClientPartyStatusEffects.clearAll();
            PartyHighlighter.disable();
            if (RPGSystemsConfig.get().systems.title) {
                TitleTextureResolver.clear();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPartyHudData.clearAll();
            ClientPartyInvites.clearAll();
            ClientPartyJoinRequests.clearAll();
            ClientPartyChat.clearAll();
            ClientPartyStatusEffects.clearAll();
            PartyHighlighter.disable();
            if (RPGSystemsConfig.get().systems.title) {
                TitleTextureResolver.clear();
            }
        });
    }
}
