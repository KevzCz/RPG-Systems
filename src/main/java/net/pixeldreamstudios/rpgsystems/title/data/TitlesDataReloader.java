package net.pixeldreamstudios.rpgsystems.title.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.attribute.EntityAttribute;
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
                JsonObject root = json != null && json.isJsonObject() ? json.getAsJsonObject() : null;

                boolean enabled = true;
                if (root != null && root.has("enabled")) {
                    enabled = root.get("enabled").getAsBoolean();
                }
                if (!enabled) {
                    RPGSystems.LOGGER.info("Skipping disabled title json {}", fileId);
                    return;
                }

                TitleData data = TitleData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

                String path = fileId.getPath();
                int slash = path.lastIndexOf('/');
                String leaf = (slash >= 0) ? path.substring(slash + 1) : path;
                if (leaf.endsWith(".json")) leaf = leaf.substring(0, leaf.length() - 5);
                Identifier id = Identifier.of(fileId.getNamespace(), leaf);

                Title.Builder b = Title.builder(id, Text.literal(data.name().orElse(leaf)));
                data.description().ifPresent(desc -> b.description(Text.literal(desc)));

                boolean hiddenTitle = root != null && root.has("hidden") && root.get("hidden").getAsBoolean();
                if (hiddenTitle) b.hidden(true);

                for (TitleData.Bonus group : data.bonuses()) {
                    for (TitleData.AttrBonus ab : group.attributes()) {
                        RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, ab.id());
                        RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + ab.id()));
                        b.add(entry, ab.amount(), ab.operation());
                    }
                    for (Identifier spell : group.spells()) b.addSpell(spell);
                    for (Identifier power : group.powers()) b.addPower(power);
                }

                for (TitleData.Condition jc : data.conditions()) {
                    Title.Condition.Type t = switch (jc.type()) {
                        case OBTAIN_ITEM -> Title.Condition.Type.OBTAIN_ITEM;
                        case KILL_MOBS -> Title.Condition.Type.KILL_MOBS;
                        case ADVANCEMENT -> Title.Condition.Type.ADVANCEMENT;
                        case WALK_BLOCKS -> Title.Condition.Type.WALK_BLOCKS;
                        case REACH_LEVEL -> Title.Condition.Type.REACH_LEVEL;
                        case REACH_LEVEL_XP -> Title.Condition.Type.REACH_LEVEL_XP;
                        case REACH_LEVEL_PUFFERFISH -> Title.Condition.Type.REACH_LEVEL_PUFFERFISH;
                        case CRAFT_ITEM -> Title.Condition.Type.CRAFT_ITEM;
                        case MINE_BLOCKS -> Title.Condition.Type.MINE_BLOCKS;
                        case VISIT_BIOME -> Title.Condition.Type.VISIT_BIOME;
                        case ENTER_DIMENSION -> Title.Condition.Type.ENTER_DIMENSION;
                        case INTERACT_BLOCK -> Title.Condition.Type.INTERACT_BLOCK;
                        case INTERACT_ENTITY -> Title.Condition.Type.INTERACT_ENTITY;
                        case FIND_STRUCTURE -> Title.Condition.Type.FIND_STRUCTURE;
                        case DEAL_DAMAGE_TOTAL -> Title.Condition.Type.DEAL_DAMAGE_TOTAL;
                        case DEAL_DAMAGE_MAX -> Title.Condition.Type.DEAL_DAMAGE_MAX;
                        case CHECK_ATTRIBUTE -> Title.Condition.Type.CHECK_ATTRIBUTE;
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
                            jc.dimension(),
                            jc.structure(),
                            jc.attribute(),
                            jc.min().orElse(0.0)
                    ));
                }

                loaded.put(id, b.build());
            } catch (Exception ex) {
                RPGSystems.LOGGER.error("Failed to parse title json {}: {}", fileId, ex.toString());
            }
        });

        TitleRegistry.replaceAll(loaded);
        TitleRegistry.bootstrapFallback();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(RPGSystems.MOD_ID, "titles_data_reloader");
    }
}
