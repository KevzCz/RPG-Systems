package net.pixeldreamstudios.rpgsystems.title;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.rpgsystems.RPGSystems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TitleRegistry {
    private static final Map<Identifier, Title> TITLES = new LinkedHashMap<>();
    private TitleRegistry() {}
    public static Title register(Title title) {
        Objects.requireNonNull(title, "title");
        if (TITLES.containsKey(title.id)) {
            throw new IllegalStateException("Duplicate title id " + title.id);
        }
        TITLES.put(title.id, title);
        return title;
    }
    public static void replaceAll(Map<Identifier, Title> newTitles) {
        TITLES.clear();
        TITLES.putAll(newTitles);
    }
    public static Title get(Identifier id) {
        return TITLES.get(id);
    }
    public static Map<Identifier, Title> all() {
        return java.util.Collections.unmodifiableMap(TITLES);
    }
    public static void bootstrapFallback() {
        if (!TITLES.isEmpty()) return;
        register(Title.builder(Identifier.of(RPGSystems.MOD_ID, "novice"), Text.translatable("title.rpgsystems.novice"))
                .description(Text.translatable("title.rpgsystems.novice.desc"))
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 2.0, EntityAttributeModifier.Operation.ADD_VALUE)
                .build());
    }
}
