package net.pixeldreamstudios.rpgsystems.title.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.pixeldreamstudios.rpgsystems.RPGSystems;
import net.pixeldreamstudios.rpgsystems.title.Title;
import net.pixeldreamstudios.rpgsystems.title.TitleRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads data/<namespace>/title/*.json into server TitleRegistry.
 * Now with verbose debug for bonuses (spell vs attribute) so we can
 * verify that "spell" is actually present in parsed data.
 */
public final class TitlesDataReloader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public TitlesDataReloader() {
        super(GSON, "title");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        Map<Identifier, Title> loaded = new HashMap<>();
        prepared.forEach((fileId, json) -> {
            try {
                TitleData data = TitleData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                String path = fileId.getPath();
                int slash = path.lastIndexOf('/');
                String leaf = (slash >= 0) ? path.substring(slash + 1) : path;
                Identifier id = Identifier.of(fileId.getNamespace(), leaf);
                Title.Builder b = Title.builder(id, Text.literal(data.name().orElse(leaf)));
                data.description().ifPresent(desc -> b.description(Text.literal(desc)));

                int idx = 0;
                for (TitleData.Bonus jb : data.bonuses()) {
                    if (jb.spell().isPresent()) {
                        Identifier spellId = jb.spell().get();
                        b.addSpell(spellId);
                    } else {
                        Identifier attrId = jb.attribute()
                                .orElseThrow(() -> new IllegalArgumentException("Bonus missing 'attribute' or 'spell'"));
                        double amount = jb.amount().orElse(0.0);
                        EntityAttributeModifier.Operation op = jb.operation().orElse(EntityAttributeModifier.Operation.ADD_VALUE);

                        RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, attrId);
                        RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + attrId));

                        b.add(entry, amount, op);
                    }
                    idx++;
                }

                if (data.conditions() != null) {
                    int ci = 0;
                    for (TitleData.Condition jc : data.conditions()) {
                        Title.Condition.Type t = switch (jc.type()) {
                            case OBTAIN_ITEM     -> Title.Condition.Type.OBTAIN_ITEM;
                            case KILL_MOBS       -> Title.Condition.Type.KILL_MOBS;
                            case ADVANCEMENT     -> Title.Condition.Type.ADVANCEMENT;
                            case WALK_BLOCKS     -> Title.Condition.Type.WALK_BLOCKS;
                            case REACH_LEVEL     -> Title.Condition.Type.REACH_LEVEL;
                            case CRAFT_ITEM      -> Title.Condition.Type.CRAFT_ITEM;
                            case MINE_BLOCKS     -> Title.Condition.Type.MINE_BLOCKS;
                            case VISIT_BIOME     -> Title.Condition.Type.VISIT_BIOME;
                            case ENTER_DIMENSION -> Title.Condition.Type.ENTER_DIMENSION;
                        };
                        b.addCondition(new Title.Condition(
                                t,
                                jc.item(),
                                jc.entityType(),
                                jc.advancement(),
                                jc.distance().orElse(0L),
                                jc.count().orElse(0),
                                jc.hint(),
                                jc.hidden().orElse(false),
                                jc.entity(),
                                jc.nbt(),
                                jc.level().orElse(0),
                                jc.block(),
                                jc.biome(),
                                jc.dimension()
                        ));
                        ci++;
                    }

                }

                Title built = b.build();
                loaded.put(id, built);
            } catch (Exception ex) {}
        });

        TitleRegistry.replaceAll(loaded);
        TitleRegistry.bootstrapFallback();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(RPGSystems.MOD_ID, "titles_data_reloader");
    }
}
