package net.pixeldreamstudios.rpgsystems.client.enemy.configui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.pixeldreamstudios.rpgsystems.client.enemy.config.DamageTypeConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class DamageTypeConfigScreen extends Screen {
    private final Screen parent;
    private DamageTypeList list;
    private ButtonWidget saveButton;
    private ButtonWidget backButton;

    public DamageTypeConfigScreen(Screen parent) {
        super(Text.literal("Damage Types"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int top = 56;
        int bottomPad = 48;
        int listHeight = this.height - top - bottomPad;

        if (this.list == null) {
            this.list = new DamageTypeList(this.client, this.width, listHeight, top, 28);
            populateEntries();
        } else {
            this.list.position(this.width, listHeight, top);
        }

        this.addDrawableChild(this.list);

        int cx = this.width / 2;
        int y = this.height - 32;

        this.saveButton = ButtonWidget.builder(Text.literal("Save"), b -> {
            applyAndSave();
            this.client.setScreen(parent);
        }).dimensions(cx - 120, y, 100, 20).build();

        this.backButton = ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(parent))
                .dimensions(cx + 20, y, 100, 20).build();

        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(this.backButton);
    }

    private void populateEntries() {
        this.list.wipe();
        var cfg = DamageTypeConfig.get();

        var ids = new LinkedHashSet<String>();
        ids.addAll(cfg.showByDamageType.keySet());
        ids.addAll(cfg.colorByDamageType.keySet());

        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(Comparator.naturalOrder());

        for (String id : sorted) {
            boolean show = cfg.showByDamageType.getOrDefault(id, true);
            String color = cfg.colorByDamageType.getOrDefault(id, "");
            this.list.addRow(id, show, color);
        }
    }

    private void applyAndSave() {
        var cfg = DamageTypeConfig.get();
        cfg.showByDamageType.clear();
        cfg.colorByDamageType.clear();
        for (DamageTypeList.Entry e : this.list.children()) {
            cfg.showByDamageType.put(e.id, e.checkbox.isChecked());
            String raw = e.colorField.getText().trim();
            if (!raw.isEmpty()) {
                cfg.colorByDamageType.put(e.id, raw);
            }
        }
        DamageTypeConfig.save();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Damage Types", this.width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Toggle visibility and set optional color overrides", this.width / 2, 36, 0xAAAAAA);
    }

    private static final class DamageTypeList extends EntryListWidget<DamageTypeList.Entry> {
        private TextFieldWidget focusedField;

        DamageTypeList(MinecraftClient client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        public void wipe() {
            this.clearEntries();
            this.focusedField = null;
        }

        public void addRow(String id, boolean show, String color) {
            this.addEntry(new Entry(id, show, color));
        }

        @Override
        public int getRowWidth() {
            return Math.min(480, this.width - 24);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (focusedField != null && focusedField.keyPressed(keyCode, scanCode, modifiers)) return true;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (focusedField != null && focusedField.charTyped(chr, modifiers)) return true;
            return super.charTyped(chr, modifiers);
        }

        void setFocusedField(TextFieldWidget field) {
            if (this.focusedField != null && this.focusedField != field) this.focusedField.setFocused(false);
            this.focusedField = field;
            if (this.focusedField != null) this.focusedField.setFocused(true);
        }

        final class Entry extends EntryListWidget.Entry<Entry> {
            final String id;
            final CheckboxWidget checkbox;
            final TextFieldWidget colorField;

            Entry(String id, boolean show, String color) {
                this.id = id;
                var tr = MinecraftClient.getInstance().textRenderer;

                this.checkbox = CheckboxWidget.builder(Text.literal(""), tr)
                        .pos(0, 0).checked(show).build();

                this.colorField = new TextFieldWidget(tr, 0, 0, 120, 20, Text.literal(""));
                this.colorField.setText(color == null ? "" : color);
                this.colorField.setMaxLength(16);
                this.colorField.setPlaceholder(Text.literal("Hex (e.g. FF0000)"));
            }

            @Override
            public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int left = x + 6;
                int checkX = left;
                int labelX = checkX + 18;
                int fieldW = 120;
                int fieldX = x + entryWidth - fieldW - 12;
                int fieldY = y + 4;

                this.checkbox.setPosition(checkX, y + 5);
                this.checkbox.render(ctx, mouseX, mouseY, tickDelta);

                var tr = MinecraftClient.getInstance().textRenderer;
                ctx.drawTextWithShadow(tr, this.id, labelX, y + 8, 0xFFFFFF);

                this.colorField.setX(fieldX);
                this.colorField.setY(fieldY);
                this.colorField.setWidth(fieldW);
                this.colorField.render(ctx, mouseX, mouseY, tickDelta);

                Integer rgb = previewColor();
                if (rgb != null) {
                    int swatch = 12;
                    int sx1 = fieldX - 16;
                    int sy1 = y + 6;
                    ctx.fill(sx1, sy1, sx1 + swatch, sy1 + swatch, 0xFF000000 | (rgb & 0xFFFFFF));
                }
            }

            private Integer previewColor() {
                try {
                    String s = this.colorField.getText();
                    if (s == null) return null;
                    s = s.trim();
                    if (s.isEmpty()) return null;
                    if (s.startsWith("#")) s = s.substring(1);
                    if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
                    int rgb = (int) Long.parseLong(s, 16);
                    return rgb & 0xFFFFFF;
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.colorField.mouseClicked(mouseX, mouseY, button)) {
                    DamageTypeList.this.setFocusedField(this.colorField);
                    return true;
                }
                if (this.checkbox.mouseClicked(mouseX, mouseY, button)) {
                    DamageTypeList.this.setFocusedField(null);
                    return true;
                }
                if (button == 0 && this.isMouseOver(mouseX, mouseY)) {
                    this.checkbox.isChecked();
                    DamageTypeList.this.setFocusedField(null);
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (this.colorField.mouseReleased(mouseX, mouseY, button)) return true;
                return this.checkbox.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return this.colorField.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char chr, int modifiers) {
                return this.colorField.charTyped(chr, modifiers);
            }
        }
    }
}
