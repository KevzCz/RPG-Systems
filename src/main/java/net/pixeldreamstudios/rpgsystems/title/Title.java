package net.pixeldreamstudios.rpgsystems.title;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Title {
    public final Identifier id;
    public final Text displayName;
    public final Text description;
    public final List<Bonus> bonuses;

    private Title(Identifier id, Text displayName, Text description, List<Bonus> bonuses) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.bonuses = Collections.unmodifiableList(bonuses);
    }

    public static Builder builder(Identifier id, Text displayName) {
        return new Builder(id, displayName);
    }

    public static final class Bonus {
        public final RegistryEntry<EntityAttribute> attribute;
        public final double amount;
        public final EntityAttributeModifier.Operation operation;

        public Bonus(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
        }
    }

    public static final class Builder {
        private final Identifier id;
        private final Text displayName;
        private final List<Bonus> bonuses = new ArrayList<>();
        private Text description;

        private Builder(Identifier id, Text displayName) {
            this.id = id;
            this.displayName = displayName;
            this.description = Text.translatable("title." + id.getNamespace() + "." + id.getPath() + ".desc");
        }

        public Builder description(Text description) {
            this.description = description;
            return this;
        }

        public Builder add(RegistryEntry<EntityAttribute> attribute, double amount) {
            this.bonuses.add(new Bonus(attribute, amount, EntityAttributeModifier.Operation.ADD_VALUE));
            return this;
        }

        public Builder add(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation op) {
            this.bonuses.add(new Bonus(attribute, amount, op));
            return this;
        }

        public Title build() {
            return new Title(id, displayName, description, bonuses);
        }
    }
}
