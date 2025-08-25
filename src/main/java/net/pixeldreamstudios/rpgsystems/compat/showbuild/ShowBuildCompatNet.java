package net.pixeldreamstudios.rpgsystems.compat.showbuild;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatPayloads.*;

public final class ShowBuildCompatNet {
    private ShowBuildCompatNet() {}

    public static void initCommon() {
        PayloadTypeRegistry.playC2S().register(OpenBuildRequest.ID, OpenBuildRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenBuildData.ID, OpenBuildData.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenBuildRequest.ID, (payload, ctx) -> {
            ServerPlayerEntity requester = ctx.player();
            requester.server.execute(() -> {
                if (!FabricLoader.getInstance().isModLoaded("showmeyourbuild")) {
                    requester.sendMessage(Text.literal("Show Me Your Build is not installed on the server."), false);
                    return;
                }
                ServerPlayerEntity target = requester.server.getPlayerManager().getPlayer(payload.targetName());
                if (target == null) {
                    requester.sendMessage(Text.literal("Could not find player: " + payload.targetName()), false);
                    return;
                }

                NbtCompound data = net.pixeldreamstudios.showmeyourbuild.network.BuildDataSerializer.serialize(target);
                if (FabricLoader.getInstance().isModLoaded("puffish_skills")) {
                    NbtCompound skills = net.pixeldreamstudios.showmeyourbuild.network.SkillTreeDataSerializer.serialize(target);
                    data.put("Skills", skills);
                }

                ServerPlayNetworking.send(requester, new OpenBuildData(target.getName().getString(), data));
            });
        });
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenBuildData.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (!FabricLoader.getInstance().isModLoaded("showmeyourbuild")) return;
                MinecraftClient.getInstance().setScreen(
                        new net.pixeldreamstudios.showmeyourbuild.client.gui.BuildViewScreen(payload.data())
                );
            });
        });
    }
}
