package net.pixeldreamstudios.rpgsystems.title.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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
            TitleData data = TitleData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

            String path = fileId.getPath();
            int slash = path.lastIndexOf('/');
            String leaf = slash >= 0 ? path.substring(slash + 1) : path;
            Identifier id = Identifier.of(fileId.getNamespace(), leaf);

            Title.Builder b = Title.builder(id, Text.literal(data.name().orElse(leaf)));
            data.description().ifPresent(desc -> b.description(Text.literal(desc)));

            for (TitleData.Bonus jb : data.bonuses()) {
                RegistryKey<EntityAttribute> key = RegistryKey.of(RegistryKeys.ATTRIBUTE, jb.attribute());
                RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(key)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown attribute: " + jb.attribute()));
                b.add(entry, jb.amount(), jb.operation());
            }

            loaded.put(id, b.build());
        });

        TitleRegistry.replaceAll(loaded);
        TitleRegistry.bootstrapFallback();
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(RPGSystems.MOD_ID, "titles_data_reloader");
    }
}
