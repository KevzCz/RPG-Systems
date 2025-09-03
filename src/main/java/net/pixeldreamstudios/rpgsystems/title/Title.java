package net.pixeldreamstudios.rpgsystems.title;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class Title {
    public final Identifier id;
    public final Text displayName;
    public final Text description;
    public final List<Bonus> bonuses;
    public final List<Condition> conditions;

    private Title(Identifier id, Text displayName, Text description, List<Bonus> bonuses, List<Condition> conditions) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.bonuses = Collections.unmodifiableList(bonuses);
        this.conditions = Collections.unmodifiableList(conditions);
    }

    public static Builder builder(Identifier id, Text displayName) {
        return new Builder(id, displayName);
    }

    public static final class Bonus {
        public final RegistryEntry<EntityAttribute> attribute;
        public final double amount;
        public final EntityAttributeModifier.Operation operation;
        public final Optional<Identifier> spellId;

        public Bonus(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
            this.spellId = Optional.empty();
        }

        public Bonus(Identifier spellId) {
            this.attribute = null;
            this.amount = 0.0;
            this.operation = EntityAttributeModifier.Operation.ADD_VALUE;
            this.spellId = Optional.of(spellId);
        }
    }


    public static final class Condition {
        public enum Type {
            OBTAIN_ITEM,
            KILL_MOBS,
            ADVANCEMENT,
            WALK_BLOCKS,
            REACH_LEVEL,
            CRAFT_ITEM,
            MINE_BLOCKS,
            VISIT_BIOME,
            ENTER_DIMENSION
        }

        public final Type type;
        public final Optional<Identifier> item;
        public final Optional<Identifier> entityType;
        public final Optional<Identifier> advancement;
        public final long distance;
        public final int count;
        public final Optional<String> hint;
        public final boolean hidden;

        public final Optional<String> entitySpec;
        public final Optional<String> nbtQuery;
        public final int level;

        public final Optional<Identifier> block;
        public final Optional<Identifier> biome;
        public final Optional<Identifier> dimension;

        public Condition(Type type,
                         Optional<Identifier> item,
                         Optional<Identifier> entityType,
                         Optional<Identifier> advancement,
                         long distance,
                         int count,
                         Optional<String> hint,
                         boolean hidden,
                         Optional<String> entitySpec,
                         Optional<String> nbtQuery,
                         int level,
                         Optional<Identifier> block,
                         Optional<Identifier> biome,
                         Optional<Identifier> dimension) {
            this.type = type;
            this.item = item;
            this.entityType = entityType;
            this.advancement = advancement;
            this.distance = distance;
            this.count = count;
            this.hint = hint;
            this.hidden = hidden;
            this.entitySpec = entitySpec;
            this.nbtQuery = nbtQuery;
            this.level = level;
            this.block = block;
            this.biome = biome;
            this.dimension = dimension;
        }
    }

    public static final class Builder {
        private final Identifier id;
        private final Text displayName;
        private final List<Bonus> bonuses = new ArrayList<>();
        private final List<Condition> conditions = new ArrayList<>();
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

        public Builder add(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                           double amount,
                           net.minecraft.entity.attribute.EntityAttributeModifier.Operation op) {
            this.bonuses.add(new Bonus(attribute, amount, op));
            return this;
        }

        public Builder add(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                           double amount) {
            return add(attribute, amount, net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE);
        }

        public Builder addSpell(Identifier spellId) {
            this.bonuses.add(new Bonus(spellId));
            return this;
        }

        public Builder addCondition(Condition c) {
            this.conditions.add(c);
            return this;
        }

        public Title build() {
            return new Title(id, displayName, description, bonuses, conditions);
        }
    }


}
