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
        this.bonuses = Collections.unmodifiableList(new ArrayList<>(bonuses));
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
    }
    public static Builder builder(Identifier id, Text displayName) {
        return new Builder(id, displayName);
    }

    public static final class Bonus {
        public final RegistryEntry<EntityAttribute> attribute;
        public final double amount;
        public final EntityAttributeModifier.Operation operation;
        public final Optional<Identifier> spellId;
        public final Optional<Identifier> powerId;

        private Bonus(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation, Optional<Identifier> spellId, Optional<Identifier> powerId) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
            this.spellId = spellId;
            this.powerId = powerId;
        }

        public static Bonus forAttribute(RegistryEntry<EntityAttribute> attr, double amount, EntityAttributeModifier.Operation op) {
            return new Bonus(attr, amount, op, Optional.empty(), Optional.empty());
        }

        public static Bonus forSpell(Identifier spellId) {
            return new Bonus(null, 0.0, EntityAttributeModifier.Operation.ADD_VALUE, Optional.of(spellId), Optional.empty());
        }

        public static Bonus forPower(Identifier powerId) {
            return new Bonus(null, 0.0, EntityAttributeModifier.Operation.ADD_VALUE, Optional.empty(), Optional.of(powerId));
        }
    }

    public static final class Condition {
        public enum Type {
            OBTAIN_ITEM,
            KILL_MOBS,
            ADVANCEMENT,
            WALK_BLOCKS,
            REACH_LEVEL,
            REACH_LEVEL_XP,
            REACH_LEVEL_PUFFERFISH,
            CRAFT_ITEM,
            MINE_BLOCKS,
            VISIT_BIOME,
            ENTER_DIMENSION,
            INTERACT_BLOCK,
            INTERACT_ENTITY,
            FIND_STRUCTURE,
            DEAL_DAMAGE_TOTAL,
            DEAL_DAMAGE_MAX,
            CHECK_ATTRIBUTE
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
        public final Optional<Identifier> structure;
        public final Optional<Identifier> attributeId;
        public final double minValue;

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
                         Optional<Identifier> dimension,
                         Optional<Identifier> structure,
                         Optional<Identifier> attributeId,
                         double minValue) {
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
            this.structure = structure;
            this.attributeId = attributeId;
            this.minValue = minValue;
        }
    }

    public static final class Builder {
        private final Identifier id;
        private final Text displayName;
        private Text description = Text.empty();
        private final List<Bonus> bonuses = new ArrayList<>();
        private final List<Condition> conditions = new ArrayList<>();

        private Builder(Identifier id, Text displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder description(Text description) {
            this.description = description;
            return this;
        }

        public Builder add(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation) {
            this.bonuses.add(Bonus.forAttribute(attribute, amount, operation));
            return this;
        }

        public Builder addSpell(Identifier spellId) {
            this.bonuses.add(Bonus.forSpell(spellId));
            return this;
        }

        public Builder addPower(Identifier powerId) {
            this.bonuses.add(Bonus.forPower(powerId));
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
