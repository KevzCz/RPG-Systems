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
    public enum DamageOp { ADDED, MULTIPLIED }

    public final Identifier id;
    public final Text displayName;
    public final Text description;
    public final boolean hidden;
    public final List<Bonus> bonuses;
    public final List<Bonus> permaBonuses;
    public final List<Condition> conditions;

    private Title(Identifier id,
                  Text displayName,
                  Text description,
                  List<Bonus> bonuses,
                  List<Bonus> permaBonuses,
                  List<Condition> conditions,
                  boolean hidden) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.hidden = hidden;
        this.bonuses = Collections.unmodifiableList(new ArrayList<>(bonuses));
        this.permaBonuses = Collections.unmodifiableList(new ArrayList<>(permaBonuses));
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
    }

    public static Builder builder(Identifier id, Text displayName) {
        return new Builder(id, displayName);
    }

    public static final class Bonus {
        public final RegistryEntry<EntityAttribute> attribute;
        public final double amount;
        public final EntityAttributeModifier.Operation operation;
        public final Optional<Identifier> damageTarget;
        public final Optional<Identifier> damageTag;
        public final DamageOp damageOp;
        public final double damageAmount;
        public final Optional<Identifier> spellId;
        public final Optional<Identifier> powerId;

        private Bonus(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;

            this.spellId = Optional.empty();
            this.powerId = Optional.empty();

            this.damageTarget = Optional.empty();
            this.damageTag = Optional.empty();
            this.damageOp = null;
            this.damageAmount = 0.0;
        }

        private Bonus(Identifier spellOrPower, boolean isSpell) {
            this.attribute = null;
            this.amount = 0.0;
            this.operation = null;

            this.spellId = isSpell ? Optional.of(spellOrPower) : Optional.empty();
            this.powerId = isSpell ? Optional.empty() : Optional.of(spellOrPower);

            this.damageTarget = Optional.empty();
            this.damageTag = Optional.empty();
            this.damageOp = null;
            this.damageAmount = 0.0;
        }

        private Bonus(Optional<Identifier> target, Optional<Identifier> tag, double amount, DamageOp op) {
            this.attribute = null;
            this.amount = 0.0;
            this.operation = null;

            this.spellId = Optional.empty();
            this.powerId = Optional.empty();

            this.damageTarget = target;
            this.damageTag = tag;
            this.damageOp = op;
            this.damageAmount = amount;
        }

        public static Bonus forDamage(Identifier target, double amount, DamageOp op) {
            return new Bonus(Optional.of(target), Optional.empty(), amount, op);
        }

        public static Bonus forDamageTag(Identifier tag, double amount, DamageOp op) {
            return new Bonus(Optional.empty(), Optional.of(tag), amount, op);
        }

        public static Bonus forAttribute(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation) {
            return new Bonus(attribute, amount, operation);
        }

        public static Bonus forSpell(Identifier spellId) {
            return new Bonus(spellId, true);
        }

        public static Bonus forPower(Identifier powerId) {
            return new Bonus(powerId, false);
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
        public final Optional<Text> hint;
        public final boolean hidden;
        public final Optional<String> entitySpec;
        public final Optional<String> nbtQuery;
        public final int level;
        public final Optional<Identifier> block;
        public final Optional<Identifier> biome;
        public final Optional<Identifier> dimension;
        public final Optional<Identifier> structure;
        public final Optional<Identifier> attributeId;
        public final Optional<Identifier> entityTagId;
        public final double minValue;

        public Condition(Type type,
                         Optional<Identifier> item,
                         Optional<Identifier> entityType,
                         Optional<Identifier> advancement,
                         long distance,
                         int count,
                         Optional<Text> hint,
                         boolean hidden,
                         Optional<String> entitySpec,
                         Optional<String> nbtQuery,
                         int level,
                         Optional<Identifier> block,
                         Optional<Identifier> biome,
                         Optional<Identifier> dimension,
                         Optional<Identifier> structure,
                         Optional<Identifier> attributeId,
                         double minValue,
                         Optional<Identifier> entityTagId){
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
            this.entityTagId = entityTagId;
        }
    }

    public static final class Builder {
        private final Identifier id;
        private final Text displayName;
        private Text description = Text.empty();
        private final List<Bonus> bonuses = new ArrayList<>();
        private final List<Bonus> permaBonuses = new ArrayList<>();
        private final List<Condition> conditions = new ArrayList<>();
        private boolean hidden = false;

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

        public Builder addDamageBonus(Identifier target, double amount, DamageOp op) {
            this.bonuses.add(Bonus.forDamage(target, amount, op));
            return this;
        }

        public Builder addDamageBonusTag(Identifier tag, double amount, DamageOp op) {
            this.bonuses.add(Bonus.forDamageTag(tag, amount, op));
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

        public Builder addPerma(RegistryEntry<EntityAttribute> attribute, double amount, EntityAttributeModifier.Operation operation) {
            this.permaBonuses.add(Bonus.forAttribute(attribute, amount, operation));
            return this;
        }

        public Builder addPermaDamageBonus(Identifier target, double amount, DamageOp op) {
            this.permaBonuses.add(Bonus.forDamage(target, amount, op));
            return this;
        }

        public Builder addPermaDamageBonusTag(Identifier tag, double amount, DamageOp op) {
            this.permaBonuses.add(Bonus.forDamageTag(tag, amount, op));
            return this;
        }

        public Builder addPermaSpell(Identifier spellId) {
            this.permaBonuses.add(Bonus.forSpell(spellId));
            return this;
        }

        public Builder addPermaPower(Identifier powerId) {
            this.permaBonuses.add(Bonus.forPower(powerId));
            return this;
        }

        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder addCondition(Condition c) {
            this.conditions.add(c);
            return this;
        }

        public Title build() {
            return new Title(id, displayName, description, bonuses, permaBonuses, conditions, hidden);
        }
    }
}
