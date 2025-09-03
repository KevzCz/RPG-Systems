package net.pixeldreamstudios.rpgsystems.title.data;

import com.mojang.serialization.Codec;
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
            Condition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(TitleData::conditions),
            Bonus.CODEC.listOf().fieldOf("bonuses").forGetter(TitleData::bonuses)
    ).apply(i, TitleData::new));

    public record Bonus(
            Optional<Identifier> attribute,
            Optional<Double> amount,
            Optional<EntityAttributeModifier.Operation> operation,
            Optional<Identifier> spell
    ) {
        public static final Codec<Bonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("attribute").forGetter(Bonus::attribute),
                Codec.DOUBLE.optionalFieldOf("amount").forGetter(Bonus::amount),
                EntityAttributeModifier.Operation.CODEC.optionalFieldOf("operation").forGetter(Bonus::operation),
                Identifier.CODEC.optionalFieldOf("spell").forGetter(Bonus::spell)
        ).apply(i, Bonus::new));
    }

    public record Condition(
            Type type,
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
            Optional<Identifier> dimension
    ) {
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

        public static final Codec<Type> TYPE_CODEC = Codec.STRING.xmap(
                s -> Type.valueOf(s.toUpperCase(Locale.ROOT)),
                t -> t.name().toLowerCase(Locale.ROOT)
        );

        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group(
                TYPE_CODEC.fieldOf("type").forGetter(Condition::type),
                Identifier.CODEC.optionalFieldOf("item").forGetter(Condition::item),
                Identifier.CODEC.optionalFieldOf("entity_type").forGetter(Condition::entityType),
                Identifier.CODEC.optionalFieldOf("advancement").forGetter(Condition::advancement),
                Codec.LONG.optionalFieldOf("distance").forGetter(Condition::distance),
                Codec.INT.optionalFieldOf("count").forGetter(Condition::count),
                Codec.STRING.optionalFieldOf("hint").forGetter(Condition::hint),
                Codec.BOOL.optionalFieldOf("hidden").forGetter(Condition::hidden),
                Codec.STRING.optionalFieldOf("entity").forGetter(Condition::entity),
                Codec.STRING.optionalFieldOf("nbt").forGetter(Condition::nbt),
                Codec.INT.optionalFieldOf("level").forGetter(Condition::level),
                Identifier.CODEC.optionalFieldOf("block").forGetter(Condition::block),
                Identifier.CODEC.optionalFieldOf("biome").forGetter(Condition::biome),
                Identifier.CODEC.optionalFieldOf("dimension").forGetter(Condition::dimension)
        ).apply(i, Condition::new));
    }
}
