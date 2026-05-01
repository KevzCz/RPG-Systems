package net.pixeldreamstudios.rpgsystems.compat.showbuild;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatPayloads.OpenBuildData;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatPayloads.OpenBuildRequest;
import net.pixeldreamstudios.showmeyourbuild.network.BuildDataSerializer;
import net.pixeldreamstudios.showmeyourbuild.network.SkillTreeDataSerializer;

public final class ShowBuildCompatNetServer {
    private ShowBuildCompatNetServer() {}

    public static void initServer() {
        PayloadTypeRegistry.playC2S().register(
                ShowBuildCompatPayloads.OpenBuildRequest.ID,
                ShowBuildCompatPayloads.OpenBuildRequest.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                ShowBuildCompatPayloads.OpenBuildData.ID,
                ShowBuildCompatPayloads.OpenBuildData.CODEC
        );
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

                NbtCompound data = BuildDataSerializer.serialize(target);
                if (FabricLoader.getInstance().isModLoaded("puffish_skills")) {
                    NbtCompound skills = SkillTreeDataSerializer.serialize(target);
                    data.put("Skills", skills);
                }

                ServerPlayNetworking.send(requester, new OpenBuildData(target.getName().getString(), data));
            });
        });
    }
}
