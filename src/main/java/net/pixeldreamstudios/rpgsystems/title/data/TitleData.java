package net.pixeldreamstudios.rpgsystems.title.data;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record TitleData(
        Optional<String> name,
        Optional<String> description,
        List<Condition> conditions,
        List<Bonus> bonuses
) {
    public static final Codec<TitleData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(TitleData::name),
            Codec.STRING.optionalFieldOf("description").forGetter(TitleData::description),
            Codec.list(Condition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(TitleData::conditions),
            Codec.list(Bonus.CODEC).optionalFieldOf("bonuses", List.of()).forGetter(TitleData::bonuses)
    ).apply(i, TitleData::new));

    public enum ConditionType {
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
        CHECK_ATTRIBUTE;

        public static final Codec<ConditionType> CODEC = Codec.STRING.xmap(
                s -> {
                    String k = s.toLowerCase(Locale.ROOT);
                    if (k.equals("reach_level")) return REACH_LEVEL;
                    if (k.equals("reach_level_xp")) return REACH_LEVEL_XP;
                    if (k.equals("reach_level_pufferfish")) return REACH_LEVEL_PUFFERFISH;
                    return ConditionType.valueOf(k.toUpperCase(Locale.ROOT));
                },
                t -> t.name().toLowerCase(Locale.ROOT)
        );
    }


    public static final class Codecs {
        private Codecs() {}

        public static <T> Codec<List<T>> oneOrMany(Codec<T> single) {
            return Codec.either(single, Codec.list(single)).xmap(
                    e -> e.map(List::of, l -> l),
                    l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
            );
        }
    }

    public record AttrBonus(
            Identifier id,
            double amount,
            EntityAttributeModifier.Operation operation
    ) {
        public static final Codec<EntityAttributeModifier.Operation> OP_CODEC = Codec.STRING.xmap(
                s -> {
                    String k = s.toUpperCase(Locale.ROOT);
                    if (k.equals("ADD_MULTIPLIED_BASE") || k.equals("MULTIPLY_BASE")) {
                        return EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    }
                    if (k.equals("ADD_MULTIPLIED_TOTAL") || k.equals("MULTIPLY_TOTAL")) {
                        return EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    }
                    return EntityAttributeModifier.Operation.ADD_VALUE;
                },
                op -> switch (op) {
                    case ADD_MULTIPLIED_BASE -> "multiply_base";
                    case ADD_MULTIPLIED_TOTAL -> "multiply_total";
                    case ADD_VALUE -> "add_value";
                }
        );

        public static final Codec<AttrBonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(AttrBonus::id),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttrBonus::amount),
                OP_CODEC.fieldOf("operation").forGetter(AttrBonus::operation)
        ).apply(i, AttrBonus::new));
    }

    public record Bonus(
            List<AttrBonus> attributes,
            List<Identifier> spells,
            List<Identifier> powers,
            List<DamageBonus> damageBonuses
    ) {
        public static final Codec<Bonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codecs.oneOrMany(AttrBonus.CODEC).optionalFieldOf("attribute", List.of()).forGetter(Bonus::attributes),
                Codecs.oneOrMany(Identifier.CODEC).optionalFieldOf("spell", List.of()).forGetter(Bonus::spells),
                Codecs.oneOrMany(Identifier.CODEC).optionalFieldOf("power", List.of()).forGetter(Bonus::powers),
                Codecs.oneOrMany(DamageBonus.CODEC).optionalFieldOf("damage_bonus", List.of()).forGetter(Bonus::damageBonuses)
        ).apply(i, Bonus::new));
    }

    public enum DamageOp {
        ADDED, MULTIPLIED;
        public static final Codec<DamageOp> CODEC = Codec.STRING.xmap(
                s -> s.equalsIgnoreCase("multiplied") ? MULTIPLIED : ADDED,
                op -> op == MULTIPLIED ? "multiplied" : "added"
        );
    }

    public record DamageBonus(
            Identifier id,
            double amount,
            DamageOp operation
    ) {
        public static final Codec<DamageBonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(DamageBonus::id),
                Codec.DOUBLE.fieldOf("amount").forGetter(DamageBonus::amount),
                DamageOp.CODEC.fieldOf("operation").forGetter(DamageBonus::operation)
        ).apply(i, DamageBonus::new));
    }
    public record Condition(
            ConditionType type,
            Optional<Identifier> item,
            Optional<Identifier> entityType,
            Optional<Identifier> advancement,
            Optional<Long> distance,
            Optional<Integer> count,
            Optional<String> hint,
            Optional<Boolean> hidden,
            Optional<String> entity,
            Optional<String> nbt,
            Optional<Integer> level,
            Optional<Identifier> block,
            Optional<Identifier> biome,
            Optional<Identifier> dimension,
            Optional<Identifier> structure,
            Optional<Identifier> attribute,
            Optional<Double> min
    ) {
        private static final MapCodec<Pair<Optional<String>, Optional<String>>> ENTITY_AND_NBT =
                RecordCodecBuilder.mapCodec(inst -> inst.group(
                        Codec.STRING.optionalFieldOf("entity").forGetter(Pair::getFirst),
                        Codec.STRING.optionalFieldOf("nbt").forGetter(Pair::getSecond)
                ).apply(inst, Pair::of));

        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group(
                ConditionType.CODEC.fieldOf("type").forGetter(Condition::type),
                Identifier.CODEC.optionalFieldOf("item").forGetter(Condition::item),
                Identifier.CODEC.optionalFieldOf("entity_type").forGetter(Condition::entityType),
                Identifier.CODEC.optionalFieldOf("advancement").forGetter(Condition::advancement),
                Codec.LONG.optionalFieldOf("distance").forGetter(Condition::distance),
                Codec.INT.optionalFieldOf("count").forGetter(Condition::count),
                Codec.STRING.optionalFieldOf("hint").forGetter(Condition::hint),
                Codec.BOOL.optionalFieldOf("hidden").forGetter(Condition::hidden),
                ENTITY_AND_NBT.forGetter(c -> Pair.of(c.entity(), c.nbt())),
                Codec.INT.optionalFieldOf("level").forGetter(Condition::level),
                Identifier.CODEC.optionalFieldOf("block").forGetter(Condition::block),
                Identifier.CODEC.optionalFieldOf("biome").forGetter(Condition::biome),
                Identifier.CODEC.optionalFieldOf("dimension").forGetter(Condition::dimension),
                Identifier.CODEC.optionalFieldOf("structure").forGetter(Condition::structure),
                Identifier.CODEC.optionalFieldOf("attribute").forGetter(Condition::attribute),
                Codec.DOUBLE.optionalFieldOf("min").forGetter(Condition::min)
        ).apply(i, (type, item, entityType, advancement, distance, count, hint, hidden, enNbt, level, block, biome, dimension, structure, attribute, min) ->
                new Condition(
                        type, item, entityType, advancement, distance, count, hint, hidden,
                        enNbt.getFirst(), enNbt.getSecond(), level, block, biome, dimension, structure, attribute, min
                )));
    }
}
