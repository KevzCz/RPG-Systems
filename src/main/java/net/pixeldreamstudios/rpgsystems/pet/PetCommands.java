package net.pixeldreamstudios.rpgsystems.pet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class PetCommands {

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("pet")
                .then(CommandManager.literal("claim")
                        .executes(ctx -> claimLookingAt(ctx.getSource(), null))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> claimLookingAt(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("unclaim")
                        .executes(ctx -> unclaimLookingAt(ctx.getSource())))
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> renameLookingAt(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("tp")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            MobEntity mob = findOwnedPetNear(p, 16);
                            if (mob == null) { p.sendMessage(Text.literal("No nearby owned pet")); return 0; }

                            if (mob.getWorld() == p.getWorld()) {
                                mob.teleport(p.getX(), p.getY(), p.getZ(), true);
                                orientToPlayer(mob, p);
                                p.sendMessage(Text.literal("Pet teleported"));
                                return 1;
                            }

                            mob.teleport(p.getServerWorld(), p.getX(), p.getY(), p.getZ(),
                                    EnumSet.noneOf(PositionFlag.class), p.getYaw(), p.getPitch());
                            p.sendMessage(Text.literal("Pet teleported"));
                            return 1;
                        }))
        );
    }

    private static void orientToPlayer(MobEntity mob, ServerPlayerEntity p) {
        mob.setYaw(p.getYaw());
        mob.setPitch(p.getPitch());
        if (mob instanceof LivingEntity le) le.setHeadYaw(p.getYaw());
    }

    private static int claimLookingAt(ServerCommandSource src, String name) {
        ServerPlayerEntity p = src.getPlayer();
        MobEntity mob = findMobInFront(p, 5);
        if (mob == null) { p.sendMessage(Text.literal("Look at a mob within 5 blocks")); return 0; }
        PetOwnable own = (PetOwnable) (Object) mob;
        own.rpgsystems$setPet(true);
        own.rpgsystems$setOwnerUuid(p.getUuid());
        if (name != null) own.rpgsystems$setPetName(name);
        p.sendMessage(Text.literal("Claimed pet"));
        return 1;
    }

    private static int unclaimLookingAt(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        MobEntity mob = findMobInFront(p, 5);
        if (mob == null) { p.sendMessage(Text.literal("Look at a mob within 5 blocks")); return 0; }
        PetOwnable own = (PetOwnable) (Object) mob;
        UUID owner = own.rpgsystems$getOwnerUuid();
        if (owner == null || !owner.equals(p.getUuid())) { p.sendMessage(Text.literal("You are not the owner")); return 0; }
        own.rpgsystems$setPet(false);
        own.rpgsystems$setOwnerUuid(null);
        own.rpgsystems$setPetName("");
        p.sendMessage(Text.literal("Unclaimed pet"));
        return 1;
    }

    private static int renameLookingAt(ServerCommandSource src, String name) {
        ServerPlayerEntity p = src.getPlayer();
        MobEntity mob = findMobInFront(p, 5);
        if (mob == null) { p.sendMessage(Text.literal("Look at a mob within 5 blocks")); return 0; }
        PetOwnable own = (PetOwnable) (Object) mob;
        if (!p.getUuid().equals(own.rpgsystems$getOwnerUuid())) { p.sendMessage(Text.literal("You are not the owner")); return 0; }
        own.rpgsystems$setPetName(name);
        p.sendMessage(Text.literal("Renamed pet"));
        return 1;
    }

    private static MobEntity findMobInFront(ServerPlayerEntity p, double range) {
        Vec3d eyes = p.getCameraPosVec(1.0f);
        Vec3d look = p.getRotationVec(1.0f).normalize();
        Vec3d end = eyes.add(look.multiply(range));
        Box box = new Box(eyes, end).expand(1.0);
        List<MobEntity> mobs = p.getWorld().getEntitiesByClass(MobEntity.class, box, Entity::isAlive);
        if (mobs.isEmpty()) return null;
        return mobs.stream().min(Comparator.comparingDouble(m -> m.getPos().squaredDistanceTo(end))).orElse(null);
    }

    private static MobEntity findOwnedPetNear(ServerPlayerEntity p, double radius) {
        List<MobEntity> mobs = p.getWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(radius), Entity::isAlive);
        UUID me = p.getUuid();
        for (MobEntity mob : mobs) {
            PetOwnable own = (PetOwnable) (Object) mob;
            if (own.rpgsystems$isPet() && me.equals(own.rpgsystems$getOwnerUuid())) return mob;
        }
        return null;
    }
}
