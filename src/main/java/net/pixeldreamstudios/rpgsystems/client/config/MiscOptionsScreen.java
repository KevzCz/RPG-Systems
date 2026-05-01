package net.pixeldreamstudios.rpgsystems.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

@Environment(EnvType.CLIENT)
public final class MiscOptionsScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget inviteX;
    private TextFieldWidget inviteY;
    private TextFieldWidget joinX;
    private TextFieldWidget joinY;
    private TextFieldWidget invPlateX;
    private TextFieldWidget invPlateY;
    private TextFieldWidget titleBtnDX;
    private TextFieldWidget titleBtnDY;
    private TextFieldWidget partyBtnDX;
    private TextFieldWidget partyBtnDY;

    private ScrollForm form;
    private double formScroll = 0.0;

    public MiscOptionsScreen(Screen parent) {
        super(Text.literal("Misc UI Positions"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearChildren();

        int centerX = this.width / 2;
        int colW = 280;
        int fieldW = 80;
        int rowH = 20;

        int top = 56;
        int bottomPad = 70;
        int formH = Math.max(60, this.height - top - bottomPad);

        var cfg = MiscClientConfig.get();

        inviteX    = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        inviteY    = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        joinX      = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        joinY      = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        invPlateX  = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        invPlateY  = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        titleBtnDX = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        titleBtnDY = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        partyBtnDX = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());
        partyBtnDY = new TextFieldWidget(this.textRenderer, 0, 0, fieldW, rowH, Text.empty());

        inviteX.setText(String.valueOf(cfg.inviteHudX));
        inviteY.setText(String.valueOf(cfg.inviteHudY));
        joinX.setText(String.valueOf(cfg.joinRequestHudX));
        joinY.setText(String.valueOf(cfg.joinRequestHudY));
        invPlateX.setText(String.valueOf(cfg.inviteInventoryX));
        invPlateY.setText(String.valueOf(cfg.inviteInventoryY));
        titleBtnDX.setText(String.valueOf(cfg.handledTitlesBtnOffsetX));
        titleBtnDY.setText(String.valueOf(cfg.handledTitlesBtnOffsetY));
        partyBtnDX.setText(String.valueOf(cfg.handledPartyBtnOffsetX));
        partyBtnDY.setText(String.valueOf(cfg.handledPartyBtnOffsetY));

        int formX = centerX - colW / 2;
        form = new ScrollForm(formX, top, colW, formH, this.textRenderer, () -> formScroll, s -> formScroll = s);

        form.addRow("Join Request HUD X (from left)", joinX, 0);
        form.addRow("Join Request HUD Y (from bottom)", joinY, 4);

        form.addRow("Invite Inventory Plate X (from left)", invPlateX, 0);
        form.addRow("Invite Inventory Plate Y (from bottom)", invPlateY, 4);

        form.addRow("Titles Button dX (relative to screen x)", titleBtnDX, 0);
        form.addRow("Titles Button dY (relative to screen y)", titleBtnDY, 4);

        form.addRow("Party Button dX (relative to screen x)", partyBtnDX, 0);
        form.addRow("Party Button dY (relative to screen y)", partyBtnDY, 0);

        form.addRow("Invite HUD X (from left)", inviteX, 0);
        form.addRow("Invite HUD Y (from bottom)", inviteY, 0);

        this.addDrawableChild(form);

        int btnY = top + formH + 16;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
            applyAndSave();
            this.client.setScreen(this.parent);
        }).size(100, rowH).position(centerX - 180, btnY).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset Defaults"), b -> {
            cfg.resetDefaults();
            MiscClientConfig.save();
            this.client.setScreen(new MiscOptionsScreen(this.parent));
        }).size(120, rowH).position(centerX - 50, btnY).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> this.client.setScreen(this.parent))
                .size(100, rowH).position(centerX + 100, btnY).build());
    }

    @Override
    public void resize(MinecraftClient mc, int w, int h) {
        String a = inviteX != null ? inviteX.getText() : "";
        String b = inviteY != null ? inviteY.getText() : "";
        String c = joinX   != null ? joinX.getText()   : "";
        String d = joinY   != null ? joinY.getText()   : "";
        String e = invPlateX != null ? invPlateX.getText() : "";
        String f = invPlateY != null ? invPlateY.getText() : "";
        String g = titleBtnDX != null ? titleBtnDX.getText() : "";
        String htx = titleBtnDY != null ? titleBtnDY.getText() : "";
        String i = partyBtnDX != null ? partyBtnDX.getText() : "";
        String j = partyBtnDY != null ? partyBtnDY.getText() : "";
        double prev = formScroll;

        super.resize(mc, w, h);
        init();

        inviteX.setText(a);
        inviteY.setText(b);
        joinX.setText(c);
        joinY.setText(d);
        invPlateX.setText(e);
        invPlateY.setText(f);
        titleBtnDX.setText(g);
        titleBtnDY.setText(htx);
        partyBtnDX.setText(i);
        partyBtnDY.setText(j);
        formScroll = prev;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (form != null && form.isMouseOver(mouseX, mouseY)) {
            return form.mouseScrolled(mouseX, mouseY, horiz, vert);
        }
        return super.mouseScrolled(mouseX, mouseY, horiz, vert);
    }

    private static int parseIntSafe(TextFieldWidget f, int fallback) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (Throwable t) {
            return fallback;
        }
    }

    private void applyAndSave() {
        var cfg = MiscClientConfig.get();
        cfg.inviteHudX = parseIntSafe(inviteX, cfg.inviteHudX);
        cfg.inviteHudY = parseIntSafe(inviteY, cfg.inviteHudY);
        cfg.joinRequestHudX = parseIntSafe(joinX, cfg.joinRequestHudX);
        cfg.joinRequestHudY = parseIntSafe(joinY, cfg.joinRequestHudY);
        cfg.inviteInventoryX = parseIntSafe(invPlateX, cfg.inviteInventoryX);
        cfg.inviteInventoryY = parseIntSafe(invPlateY, cfg.inviteInventoryY);
        cfg.handledTitlesBtnOffsetX = parseIntSafe(titleBtnDX, cfg.handledTitlesBtnOffsetX);
        cfg.handledTitlesBtnOffsetY = parseIntSafe(titleBtnDY, cfg.handledTitlesBtnOffsetY);
        cfg.handledPartyBtnOffsetX  = parseIntSafe(partyBtnDX, cfg.handledPartyBtnOffsetX);
        cfg.handledPartyBtnOffsetY  = parseIntSafe(partyBtnDY, cfg.handledPartyBtnOffsetY);
        MiscClientConfig.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    private static final class ScrollForm extends ClickableWidget {
        private static final int LABEL_COLOR = 0xAAAAAA;
        private static final int ROW_GAP = 24;
        private final List<Row> rows = new ArrayList<>();
        private final TextRenderer tr;
        private final DoubleSupplier getInitialScroll;
        private final DoubleConsumer onScrollChanged;

        private double scroll;
        private int contentHeight;

        private ClickableWidget focusedChild;

        ScrollForm(int x, int y, int w, int h,
                   TextRenderer tr,
                   DoubleSupplier initialScroll,
                   DoubleConsumer onScrollChanged) {
            super(x, y, w, h, Text.empty());
            this.tr = tr;
            this.getInitialScroll = initialScroll;
            this.onScrollChanged = onScrollChanged;
            this.scroll = clamp(initialScroll.getAsDouble());
        }

        void addRow(String label, TextFieldWidget field, int extraBottomSpace) {
            rows.add(new Row(label, field, extraBottomSpace));
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            this.scroll = clamp(this.scroll == 0 ? getInitialScroll.getAsDouble() : this.scroll);
            layoutChildren();
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            ctx.enableScissor(x, y, x + w, y + h);

            int drawY = y - (int) Math.round(scroll);
            for (Row r : rows) {
                ctx.drawText(tr, r.label, x, drawY + 4, LABEL_COLOR, false);
                r.field.render(ctx, mouseX, mouseY, delta);
                drawY += ROW_GAP + r.extraBottom;
            }

            ctx.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!this.isMouseOver(mx, my)) return false;
            for (Row r : rows) {
                if (r.field.mouseClicked(mx, my, button)) {
                    setFocusedChild(r.field);
                    return true;
                }
            }
            setFocusedChild(null);
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            boolean any = false;
            for (Row r : rows) any |= r.field.mouseReleased(mx, my, button);
            return any;
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

        @Override
        public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
            if (!this.isMouseOver(mx, my)) return false;
            this.scroll = clamp(this.scroll - vert * 18.0);
            onScrollChanged.accept(this.scroll);
            layoutChildren();
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (focusedChild != null && focusedChild.keyPressed(keyCode, scanCode, modifiers)) return true;
            for (Row r : rows) if (r.field.keyPressed(keyCode, scanCode, modifiers)) return true;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (focusedChild != null && focusedChild.charTyped(chr, modifiers)) return true;
            for (Row r : rows) if (r.field.charTyped(chr, modifiers)) return true;
            return super.charTyped(chr, modifiers);
        }

        private void setFocusedChild(ClickableWidget w) {
            if (this.focusedChild == w) return;
            if (this.focusedChild != null) this.focusedChild.setFocused(false);
            this.focusedChild = w;
            if (this.focusedChild != null) this.focusedChild.setFocused(true);
        }

        private void layoutChildren() {
            int x = this.getX();
            int y = this.getY() - (int) Math.round(scroll);
            int w = this.getWidth();
            int fieldW = 80;

            int drawY = y;
            for (Row r : rows) {
                int fieldX = x + w - fieldW;
                int fieldY = drawY;
                r.field.setX(fieldX);
                r.field.setY(fieldY);
                r.field.setWidth(fieldW);
                drawY += ROW_GAP + r.extraBottom;
            }
            contentHeight = Math.max(0, drawY - y);
        }

        private int maxScroll() { return Math.max(0, contentHeight - this.getHeight()); }
        private double clamp(double s) {
            int max = maxScroll();
            if (s < 0) return 0;
            if (s > max) return max;
            return s;
        }

        private static final class Row {
            final String label;
            final TextFieldWidget field;
            final int extraBottom;
            Row(String label, TextFieldWidget field, int extraBottom) {
                this.label = label;
                this.field = field;
                this.extraBottom = extraBottom;
            }
        }
    }
}
