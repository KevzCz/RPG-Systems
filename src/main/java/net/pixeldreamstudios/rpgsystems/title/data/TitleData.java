package net.pixeldreamstudios.rpgsystems.title.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public record TitleData(
        Optional<String> name,
        Optional<String> description,
        Optional<String> condition,
        List<Bonus> bonuses
) {
    public static final Codec<TitleData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(TitleData::name),
            Codec.STRING.optionalFieldOf("description").forGetter(TitleData::description),
            Codec.STRING.optionalFieldOf("condition").forGetter(TitleData::condition),
            Bonus.CODEC.listOf().fieldOf("bonuses").forGetter(TitleData::bonuses)
    ).apply(i, TitleData::new));

    public record Bonus(Identifier attribute, double amount, EntityAttributeModifier.Operation operation) {
        public static final Codec<Bonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("attribute").forGetter(Bonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Bonus::amount),
                EntityAttributeModifier.Operation.CODEC.optionalFieldOf("operation", EntityAttributeModifier.Operation.ADD_VALUE).forGetter(Bonus::operation)
        ).apply(i, Bonus::new));
    }
}
