package net.pixeldreamstudios.rpgsystems.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
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
import net.pixeldreamstudios.rpgsystems.client.title.TitlePlayerRenderer;
import net.pixeldreamstudios.rpgsystems.client.title.TitlePowersClient;
import net.pixeldreamstudios.rpgsystems.client.title.TitleTextureResolver;
import net.pixeldreamstudios.rpgsystems.config.RPGSystemsConfig;
import net.pixeldreamstudios.rpgsystems.network.EnemyNet;
import net.pixeldreamstudios.rpgsystems.network.SystemNet;
import net.pixeldreamstudios.rpgsystems.network.TitleNet;
import net.pixeldreamstudios.rpgsystems.network.TitlePowerNet;
import net.pixeldreamstudios.rpgsystems.network.party.PartyJoinRequestPayloads;
import net.pixeldreamstudios.rpgsystems.party.FTBTeamsIntegration;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class RPGSystemsClient implements ClientModInitializer {
    private static KeyBinding openPartyScreen;
    private static KeyBinding highlightParty;
    private static KeyBinding togglePartyHud;

    @Override
    public void onInitializeClient() {
        ClientConfigLoad.init();

        SystemNet.registerClient();

        ClientPartyInvites.initClientReceivers();
        ClientPartyHudData.initClientReceivers();
        ClientPartyChat.initClientReceivers();
        ClientPartyJoinRequests.initClientReceivers();

        PartyHud.init();
        PartyInviteHud.init();
        PartyInviteInventoryUi.init();
        PartyJoinRequestHud.init();
        ClientPartyHighlighter.init();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("showmeyourbuild")) {
            net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatNetClient.initClient();
        }

        EnemyNet.initClient();

        EnemyHealthBarRenderer.init();
        DamageNumbersRenderer.init();
        HealingNumbersRenderer.init();

        if (RPGSystemsConfig.get().systems.title) {
            TitleNet.registerClient();
            TitlePlayerRenderer.init();
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
                boolean hasParty = ClientPartyHudData.partyId != null;

                MinecraftClient mc = MinecraftClient.getInstance();
                HitResult hit = mc.crosshairTarget;
                boolean acted = false;

                if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult ehr = (EntityHitResult) hit;
                    if (ehr.getEntity() instanceof PlayerEntity target && mc.player != null && !target.getUuid().equals(mc.player.getUuid())) {
                        if (mc.getNetworkHandler() != null) {
                            if (hasParty) {
                                // Send invite using appropriate system
                                FTBTeamsCommandHelper.sendInviteCommand(target.getName().getString());
                                acted = true;
                            } else {
                                // Send join request - works for both systems now!
                                // For FTB Teams: we'll send a custom join request packet
                                // For native: uses the native command
                                if (FTBTeamsIntegration.isEnabled()) {
                                    // Get the target's FTB Teams party and send join request packet
                                    sendFTBTeamsJoinRequest(target);
                                    acted = true;
                                } else {
                                    mc.getNetworkHandler().sendChatCommand("party request player " + target.getName().getString());
                                    acted = true;
                                }
                            }
                        }
                    }
                }

                if (!acted && hasParty) {
                    if (mc.currentScreen == null) {
                        mc.setScreen(new PartyScreen());
                    }
                }
            }

            while (highlightParty.wasPressed()) {
                ClientPartyHighlighter.toggle();
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
            ClientPartyHighlighter.disable();
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
            ClientPartyHighlighter.disable();
            if (RPGSystemsConfig.get().systems.title) {
                TitleTextureResolver.clear();
            }
        });

    }
    private static void sendFTBTeamsJoinRequest(PlayerEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            ClientPlayNetworking.send(
                    new PartyJoinRequestPayloads.FTBTeamsJoinRequest(target.getUuid())
            );
        }
    }
}
