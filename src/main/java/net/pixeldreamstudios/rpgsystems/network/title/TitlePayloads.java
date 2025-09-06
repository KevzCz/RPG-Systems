package net.pixeldreamstudios.rpgsystems.network.title;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TitlePayloads {
    private TitlePayloads() {}

    private static <V> PacketCodec<RegistryByteBuf, V> wrap(PacketCodec<io.netty.buffer.ByteBuf, V> base) {
        return new PacketCodec<>() {
            @Override
            public V decode(RegistryByteBuf buf) {
                return base.decode(buf);
            }

            @Override
            public void encode(RegistryByteBuf buf, V value) {
                base.encode(buf, value);
            }
        };
    }

    private static final PacketCodec<RegistryByteBuf, Integer> VAR_INT   = wrap(PacketCodecs.VAR_INT);
    private static final PacketCodec<RegistryByteBuf, Long>    VAR_LONG  = wrap(PacketCodecs.VAR_LONG);
    private static final PacketCodec<RegistryByteBuf, Double>  DOUBLE    = wrap(PacketCodecs.DOUBLE);
    private static final PacketCodec<RegistryByteBuf, Boolean> BOOL      = wrap(PacketCodecs.BOOL);
    private static final PacketCodec<RegistryByteBuf, String>  STRING    = wrap(PacketCodecs.STRING);
    private static final PacketCodec<RegistryByteBuf, Optional<String>> OPT_STRING =
            wrap(PacketCodecs.optional(PacketCodecs.STRING));
    private static final PacketCodec<RegistryByteBuf, Identifier> ID_CODEC =
            wrap(Identifier.PACKET_CODEC);
    private static final PacketCodec<RegistryByteBuf, Optional<Identifier>> OPT_ID =
            wrap(PacketCodecs.optional(Identifier.PACKET_CODEC));

    private static final PacketCodec<RegistryByteBuf, UUID> UUID_CODEC = new PacketCodec<RegistryByteBuf, UUID>() {
        @Override public UUID decode(RegistryByteBuf buf) { return Uuids.PACKET_CODEC.decode(buf); }
        @Override public void encode(RegistryByteBuf buf, UUID value) { Uuids.PACKET_CODEC.encode(buf, value); }
    };

    public record SyncSelf(List<Identifier> unlocked, Optional<Identifier> active) implements CustomPayload {
        public static final Id<SyncSelf> ID = new Id<>(Identifier.of("rpg-systems", "titles_self_sync"));
        public static final PacketCodec<RegistryByteBuf, SyncSelf> CODEC = PacketCodec.tuple(
                wrap(PacketCodecs.collection(ArrayList::new, Identifier.PACKET_CODEC)), SyncSelf::unlocked,
                wrap(PacketCodecs.optional(Identifier.PACKET_CODEC)), SyncSelf::active,
                SyncSelf::new
        );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SyncActive(UUID playerUuid, Optional<Identifier> active) implements CustomPayload {
        public static final Id<SyncActive> ID = new Id<>(Identifier.of("rpg-systems", "titles_active_sync"));
        public static final PacketCodec<RegistryByteBuf, SyncActive> CODEC = PacketCodec.tuple(
                UUID_CODEC, SyncActive::playerUuid,
                wrap(PacketCodecs.optional(Identifier.PACKET_CODEC)), SyncActive::active,
                SyncActive::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestSetActive(Optional<Identifier> active) implements CustomPayload {
        public static final Id<RequestSetActive> ID = new Id<>(Identifier.of("rpg-systems", "titles_set_active"));
        public static final PacketCodec<RegistryByteBuf, RequestSetActive> CODEC = PacketCodec.tuple(
                wrap(PacketCodecs.optional(Identifier.PACKET_CODEC)), RequestSetActive::active,
                RequestSetActive::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SyncDefinitions(List<Def> defs) implements CustomPayload {
        public static final Id<SyncDefinitions> ID = new Id<>(Identifier.of("rpg-systems", "titles_defs_sync"));
        public static final PacketCodec<RegistryByteBuf, SyncDefinitions> CODEC = PacketCodec.tuple(
                PacketCodecs.collection(ArrayList::new, Def.CODEC), SyncDefinitions::defs,
                SyncDefinitions::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public enum CondType {
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

        private static final PacketCodec<RegistryByteBuf, CondType> COND_TYPE_CODEC = new PacketCodec<RegistryByteBuf, CondType>() {
            @Override
            public CondType decode(RegistryByteBuf buf) {
                return CondType.values()[VAR_INT.decode(buf)];
            }

            @Override
            public void encode(RegistryByteBuf buf, CondType value) {
                VAR_INT.encode(buf, value.ordinal());
            }
        };

        public record ConditionDef(
                CondType type,
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
                Optional<Identifier> attribute,
                double min
        ) {
            public static final PacketCodec<RegistryByteBuf, ConditionDef> CODEC = new PacketCodec<RegistryByteBuf, ConditionDef>() {
                @Override
                public ConditionDef decode(RegistryByteBuf buf) {
                    CondType type = COND_TYPE_CODEC.decode(buf);
                    Optional<Identifier> item = OPT_ID.decode(buf);
                    Optional<Identifier> entityType = OPT_ID.decode(buf);
                    Optional<Identifier> advancement = OPT_ID.decode(buf);
                    long distance = VAR_LONG.decode(buf);
                    int count = VAR_INT.decode(buf);
                    Optional<String> hint = OPT_STRING.decode(buf);
                    boolean hidden = BOOL.decode(buf);
                    Optional<String> entitySpec = OPT_STRING.decode(buf);
                    Optional<String> nbtQuery = OPT_STRING.decode(buf);
                    int level = VAR_INT.decode(buf);
                    Optional<Identifier> block = OPT_ID.decode(buf);
                    Optional<Identifier> biome = OPT_ID.decode(buf);
                    Optional<Identifier> dimension = OPT_ID.decode(buf);
                    Optional<Identifier> structure = OPT_ID.decode(buf);
                    Optional<Identifier> attribute = OPT_ID.decode(buf);
                    double min = DOUBLE.decode(buf);
                    return new ConditionDef(type, item, entityType, advancement, distance, count, hint, hidden, entitySpec, nbtQuery, level, block, biome, dimension, structure, attribute, min);
                }

                @Override
                public void encode(RegistryByteBuf buf, ConditionDef v) {
                    COND_TYPE_CODEC.encode(buf, v.type);
                    OPT_ID.encode(buf, v.item);
                    OPT_ID.encode(buf, v.entityType);
                    OPT_ID.encode(buf, v.advancement);
                    VAR_LONG.encode(buf, v.distance);
                    VAR_INT.encode(buf, v.count);
                    OPT_STRING.encode(buf, v.hint);
                    BOOL.encode(buf, v.hidden);
                    OPT_STRING.encode(buf, v.entitySpec);
                    OPT_STRING.encode(buf, v.nbtQuery);
                    VAR_INT.encode(buf, v.level);
                    OPT_ID.encode(buf, v.block);
                    OPT_ID.encode(buf, v.biome);
                    OPT_ID.encode(buf, v.dimension);
                    OPT_ID.encode(buf, v.structure);
                    OPT_ID.encode(buf, v.attribute);
                    DOUBLE.encode(buf, v.min);
                }
            };
        }

        public record BonusDef(Identifier attribute, double amount, EntityAttributeModifier.Operation operation) {
            private static final PacketCodec<RegistryByteBuf, EntityAttributeModifier.Operation> OP_CODEC =
                    new PacketCodec<>() {
                        @Override
                        public EntityAttributeModifier.Operation decode(RegistryByteBuf buf) {
                            return EntityAttributeModifier.Operation.values()[VAR_INT.decode(buf)];
                        }

                        @Override
                        public void encode(RegistryByteBuf buf, EntityAttributeModifier.Operation value) {
                            VAR_INT.encode(buf, value.ordinal());
                        }
                    };
            public static final PacketCodec<RegistryByteBuf, BonusDef> CODEC = PacketCodec.tuple(
                    ID_CODEC, BonusDef::attribute,
                    DOUBLE, BonusDef::amount,
                    OP_CODEC, BonusDef::operation,
                    BonusDef::new
            );
        }
        public enum DmgOp { ADDED, MULTIPLIED }
        private static final PacketCodec<RegistryByteBuf, DmgOp> DMG_OP_CODEC = new PacketCodec<>() {
            @Override public DmgOp decode(RegistryByteBuf buf) { return DmgOp.values()[VAR_INT.decode(buf)]; }
            @Override public void encode(RegistryByteBuf buf, DmgOp v) { VAR_INT.encode(buf, v.ordinal()); }
        };

        public record DamageBonusDef(Identifier target, double amount, DmgOp op) {
            public static final PacketCodec<RegistryByteBuf, DamageBonusDef> CODEC = PacketCodec.tuple(
                    ID_CODEC, DamageBonusDef::target,
                    DOUBLE,  DamageBonusDef::amount,
                    DMG_OP_CODEC, DamageBonusDef::op,
                    DamageBonusDef::new
            );
        }
        public record Def(
                Identifier id,
                String name,
                Optional<String> description,
                List<BonusDef> bonuses,
                List<Identifier> spells,
                List<Identifier> powers,
                List<DamageBonusDef> damage,
                List<ConditionDef> conditions,
                boolean hidden
        ) {
            public static final PacketCodec<RegistryByteBuf, Def> CODEC = new PacketCodec<>() {
                @Override public Def decode(RegistryByteBuf buf) {
                    Identifier id = ID_CODEC.decode(buf);
                    String name = STRING.decode(buf);
                    Optional<String> description = OPT_STRING.decode(buf);
                    List<BonusDef> bonuses = PacketCodecs.collection(ArrayList::new, BonusDef.CODEC).decode(buf);
                    List<Identifier> spells = PacketCodecs.collection(ArrayList::new, ID_CODEC).decode(buf);
                    List<Identifier> powers = PacketCodecs.collection(ArrayList::new, ID_CODEC).decode(buf);
                    List<DamageBonusDef> damage = PacketCodecs.collection(ArrayList::new, DamageBonusDef.CODEC).decode(buf);
                    List<ConditionDef> conditions = PacketCodecs.collection(ArrayList::new, ConditionDef.CODEC).decode(buf);
                    boolean hidden = BOOL.decode(buf);
                    return new Def(id, name, description, bonuses, spells, powers, damage, conditions, hidden);
                }
                @Override public void encode(RegistryByteBuf buf, Def v) {
                    ID_CODEC.encode(buf, v.id);
                    STRING.encode(buf, v.name);
                    OPT_STRING.encode(buf, v.description);
                    PacketCodecs.collection(ArrayList::new, BonusDef.CODEC).encode(buf, new ArrayList<>(v.bonuses));
                    PacketCodecs.collection(ArrayList::new, ID_CODEC).encode(buf, new ArrayList<>(v.spells));
                    PacketCodecs.collection(ArrayList::new, ID_CODEC).encode(buf, new ArrayList<>(v.powers));
                    PacketCodecs.collection(ArrayList::new, DamageBonusDef.CODEC).encode(buf, new ArrayList<>(v.damage));
                    PacketCodecs.collection(ArrayList::new, ConditionDef.CODEC).encode(buf, new ArrayList<>(v.conditions));
                    BOOL.encode(buf, v.hidden);
                }
            };
        }


    }
    public record SyncProgress(List<TitleProgress> progresses) implements CustomPayload {
        public static final Id<SyncProgress> ID = new Id<>(Identifier.of("rpg-systems", "titles_progress_sync"));
        public static final PacketCodec<RegistryByteBuf, SyncProgress> CODEC = PacketCodec.tuple(
                PacketCodecs.collection(ArrayList::new, TitleProgress.CODEC), SyncProgress::progresses,
                SyncProgress::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public record CondProg(long current, boolean done) {
            public static final PacketCodec<RegistryByteBuf, CondProg> CODEC = PacketCodec.tuple(
                    VAR_LONG, CondProg::current,
                    BOOL,    CondProg::done,
                    CondProg::new
            );
        }

        public record TitleProgress(Identifier id, List<CondProg> conditions) {
            public static final PacketCodec<RegistryByteBuf, TitleProgress> CODEC = PacketCodec.tuple(
                    ID_CODEC, TitleProgress::id,
                    PacketCodecs.collection(ArrayList::new, CondProg.CODEC), TitleProgress::conditions,
                    TitleProgress::new
            );
        }
    }
}
