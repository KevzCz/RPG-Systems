// net/pixeldreamstudios/rpgsystems/client/party/configui/ClientConfigsScreen.java
package net.pixeldreamstudios.rpgsystems.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.configui.DamageNumbersScreen;
import net.pixeldreamstudios.rpgsystems.client.enemy.configui.HealingNumbersScreen;
import net.pixeldreamstudios.rpgsystems.client.party.configui.PartyHudOptionsScreen;
import net.pixeldreamstudios.rpgsystems.client.party.configui.PartyMemberInfoOptionsScreen;

@Environment(EnvType.CLIENT)
public final class ClientConfigsScreen extends Screen {
    private final Screen parent;

    // persist the vertical scroll even across rebuilds/resizes
    private double navScroll = 0.0;
    private VerticalColumn navColumn;

    public ClientConfigsScreen(Screen parent) {
        super(Text.literal("Client Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        build();
    }

    private void build() {
        this.clearChildren();

        final int buttonH = 20;
        final int top = 56; // leave room for the title/subtitle

        // Column size: centered, as tall as possible while leaving ~24px bottom breathing room
        int colW = 220;
        int colH = Math.max(buttonH + 8, this.height - top - 24);

        navColumn = new VerticalColumn(this.width / 2 - colW / 2, top, colW, colH,
                () -> navScroll,
                s -> navScroll = s
        );

        // Buttons in the column
        ButtonWidget partyHudBtn = ButtonWidget.builder(Text.literal("Party HUD"),
                        b -> this.client.setScreen(new PartyHudOptionsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget memberInfoBtn = ButtonWidget.builder(Text.literal("Member Info"),
                        b -> this.client.setScreen(new PartyMemberInfoOptionsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget damageBtn = ButtonWidget.builder(Text.literal("Damage Numbers"),
                        b -> this.client.setScreen(new DamageNumbersScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget healingBtn = ButtonWidget.builder(Text.literal("Healing Numbers"),
                        b -> this.client.setScreen(new HealingNumbersScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        navColumn.setButtons(partyHudBtn, memberInfoBtn, damageBtn, healingBtn);
        this.addDrawableChild(navColumn);
    }

    // keep column scroll on resize
    @Override
    public void resize(MinecraftClient mc, int w, int h) {
        if (navColumn != null) navScroll = navColumn.getScroll();
        super.resize(mc, w, h);
        build();
    }

    // let the column consume wheel when hovered
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (navColumn != null && navColumn.isMouseOver(mouseX, mouseY)) {
            return navColumn.mouseScrolled(mouseX, mouseY, horiz, vert);
        }
        return super.mouseScrolled(mouseX, mouseY, horiz, vert);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Open a section to configure"),
                this.width / 2, 36, 0xAAAAAA);
    }

    // ---- vertically scrollable column of buttons (persists scroll) ----
    private static final class VerticalColumn extends ClickableWidget {
        private final java.util.List<ButtonWidget> children = new java.util.ArrayList<>();
        private double scroll;              // 0 .. (contentHeight - height)
        private int contentHeight;
        private final java.util.function.DoubleSupplier getInitialScroll;
        private final java.util.function.DoubleConsumer onScrollChanged;

        VerticalColumn(int x, int y, int w, int h,
                       java.util.function.DoubleSupplier initialScroll,
                       java.util.function.DoubleConsumer onScrollChanged) {
            super(x, y, w, h, Text.empty());
            this.getInitialScroll = initialScroll;
            this.onScrollChanged = onScrollChanged;
            this.scroll = clamp(initialScroll.getAsDouble());
        }

        void setButtons(ButtonWidget... btns) {
            children.clear();
            java.util.Collections.addAll(children, btns);
            layout();
        }

        double getScroll() { return scroll; }
        void setScroll(double s) { this.scroll = clamp(s); layout(); }

        private void layout() {
            int y = this.getY() + 4 - (int)Math.round(scroll);
            int spacing = 8;
            contentHeight = 0;

            for (var b : children) {
                // center horizontally with 4px inset on each side
                b.setX(this.getX() + 4);
                b.setY(y);
                y += b.getHeight() + spacing;
                contentHeight += b.getHeight() + spacing;
            }
            if (!children.isEmpty()) contentHeight -= spacing;
        }

        private int maxScroll() { return Math.max(0, contentHeight - this.getHeight()); }
        private double clamp(double s) {
            int max = maxScroll();
            if (s < 0) return 0;
            if (s > max) return max;
            return s;
        }

        @Override protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder b) {}

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            // make sure initial scroll survives rebuilds
            this.scroll = clamp(this.scroll == 0 ? getInitialScroll.getAsDouble() : this.scroll);

            ctx.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            for (var b : children) b.render(ctx, mouseX, mouseY, delta);
            ctx.disableScissor();
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (!this.isMouseOver(mx, my)) return false;
            for (var b : children) if (b.mouseClicked(mx, my, button)) return true;
            return true; // consume
        }

        @Override public boolean mouseReleased(double mx, double my, int button) {
            boolean any = false;
            for (var b : children) any |= b.mouseReleased(mx, my, button);
            return any;
        }

        @Override public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
            if (!this.isMouseOver(mx, my)) return false;
            this.scroll = clamp(this.scroll - vert * 18.0);
            onScrollChanged.accept(this.scroll); // persist
            layout();
            return true;
        }
    }
}
