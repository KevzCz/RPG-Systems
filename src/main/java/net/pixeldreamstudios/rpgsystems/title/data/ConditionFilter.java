package net.pixeldreamstudios.rpgsystems.title.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.Optional;

public record ConditionFilter(
        Optional<String> errand,
        Optional<String> kind,
        Optional<String> profession,
        Optional<Identifier> dialogue,
        Optional<String> standing,
        Optional<Identifier> category,
        Optional<String> skill
) {
    public static final ConditionFilter NONE = new ConditionFilter(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static final MapCodec<ConditionFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("errand").forGetter(ConditionFilter::errand),
            Codec.STRING.optionalFieldOf("kind").forGetter(ConditionFilter::kind),
            Codec.STRING.optionalFieldOf("profession").forGetter(ConditionFilter::profession),
            Identifier.CODEC.optionalFieldOf("dialogue").forGetter(ConditionFilter::dialogue),
            Codec.STRING.optionalFieldOf("standing").forGetter(ConditionFilter::standing),
            Identifier.CODEC.optionalFieldOf("category").forGetter(ConditionFilter::category),
            Codec.STRING.optionalFieldOf("skill").forGetter(ConditionFilter::skill)
    ).apply(i, ConditionFilter::new));
}
