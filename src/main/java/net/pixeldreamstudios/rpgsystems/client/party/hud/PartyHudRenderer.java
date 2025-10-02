package net.pixeldreamstudios.rpgsystems.client.party.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.pixeldreamstudios.rpgsystems.client.party.config.PartyHudClientConfig;

public interface PartyHudRenderer {
    void render(DrawContext ctx, RenderTickCounter tickCounter, PartyHudClientConfig cfg);
}
