package net.pixeldreamstudios.rpgsystems.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.config.MiscOptionsScreen;
import net.pixeldreamstudios.rpgsystems.client.enemy.configui.DamageNumbersScreen;
import net.pixeldreamstudios.rpgsystems.client.enemy.configui.EnemyHudsScreen;
import net.pixeldreamstudios.rpgsystems.client.enemy.configui.HealingNumbersScreen;
import net.pixeldreamstudios.rpgsystems.client.party.configui.PartyHudOptionsScreen;
import net.pixeldreamstudios.rpgsystems.client.party.configui.PartyMemberInfoOptionsScreen;
import net.pixeldreamstudios.rpgsystems.client.title.configui.TitlesOptionsScreen;

@Environment(EnvType.CLIENT)
public final class ClientConfigsScreen extends Screen {
    private final Screen parent;
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
        final int top = 56;

        int colW = 220;
        int colH = Math.max(buttonH + 8, this.height - top - 24);

        navColumn = new VerticalColumn(this.width / 2 - colW / 2, top, colW, colH,
                () -> navScroll,
                s -> navScroll = s
        );

        ButtonWidget partyHudBtn = ButtonWidget.builder(Text.literal("Party HUD"),
                        b -> this.client.setScreen(new PartyHudOptionsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget memberInfoBtn = ButtonWidget.builder(Text.literal("Member Info"),
                        b -> this.client.setScreen(new PartyMemberInfoOptionsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget enemyHudsBtn = ButtonWidget.builder(Text.literal("Enemy HUDs"),
                        b -> this.client.setScreen(new EnemyHudsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget damageBtn = ButtonWidget.builder(Text.literal("Damage Numbers"),
                        b -> this.client.setScreen(new DamageNumbersScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();

        ButtonWidget healingBtn = ButtonWidget.builder(Text.literal("Healing Numbers"),
                        b -> this.client.setScreen(new HealingNumbersScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();
        ButtonWidget titlesBtn = ButtonWidget.builder(Text.literal("Titles"),
                        b -> this.client.setScreen(new TitlesOptionsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();
        ButtonWidget miscBtn = ButtonWidget.builder(Text.literal("Misc UI Positions"),
                        b -> this.client.setScreen(new MiscOptionsScreen(this)))
                .size(colW - 8, buttonH).position(0, 0).build();
        navColumn.setButtons(partyHudBtn, memberInfoBtn, enemyHudsBtn, damageBtn, healingBtn, titlesBtn, miscBtn);
        this.addDrawableChild(navColumn);
    }

    @Override
    public void resize(MinecraftClient mc, int w, int h) {
        if (navColumn != null) navScroll = navColumn.getScroll();
        super.resize(mc, w, h);
        build();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (navColumn != null && navColumn.isMouseOver(mouseX, mouseY)) {
            return navColumn.mouseScrolled(mouseX, mouseY, horiz, vert);
        }
        return super.mouseScrolled(mouseX, mouseY, vert, horiz);
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

    private static final class VerticalColumn extends ClickableWidget {
        private final java.util.List<ButtonWidget> children = new java.util.ArrayList<>();
        private double scroll;
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
            int y = this.getY() + 4 - (int) Math.round(scroll);
            int spacing = 8;
            contentHeight = 0;

            for (var b : children) {
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
            this.scroll = clamp(this.scroll == 0 ? getInitialScroll.getAsDouble() : this.scroll);
            ctx.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            for (var b : children) b.render(ctx, mouseX, mouseY, delta);
            ctx.disableScissor();
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (!this.isMouseOver(mx, my)) return false;
            for (var b : children) if (b.mouseClicked(mx, my, button)) return true;
            return true;
        }

        @Override public boolean mouseReleased(double mx, double my, int button) {
            boolean any = false;
            for (var b : children) any |= b.mouseReleased(mx, my, button);
            return any;
        }

        @Override public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
            if (!this.isMouseOver(mx, my)) return false;
            this.scroll = clamp(this.scroll - vert * 18.0);
            onScrollChanged.accept(this.scroll);
            layout();
            return true;
        }
    }
}
