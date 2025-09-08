package net.pixeldreamstudios.rpgsystems.compat.showbuild;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.pixeldreamstudios.rpgsystems.compat.showbuild.ShowBuildCompatPayloads.OpenBuildData;

@Environment(EnvType.CLIENT)
public final class ShowBuildCompatNetClient {
    private ShowBuildCompatNetClient() {}

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
