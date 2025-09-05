package net.pixeldreamstudios.rpgsystems.title.power.powers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.pixeldreamstudios.rpgsystems.title.power.TitlePower;

import java.util.*;

public final class IlluminatePower implements TitlePower {
    public static final Identifier ID = Identifier.of("rpg-systems", "illuminate");
    private final Map<UUID, BlockPos> lastPos = new HashMap<>();
    private final Map<UUID, net.minecraft.registry.RegistryKey<World>> lastDim = new HashMap<>();
    @Override
    public Identifier id() {
        return ID;
    }
    @Override
    public void onDeactivate(ServerPlayerEntity player) {
        removeLight(player.getServer(), player.getUuid());
    }
    @Override
    public void onServerTick(MinecraftServer server, Set<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            tryUpdateLight(player);
        }
        if (!lastPos.isEmpty()) {
            HashSet<UUID> active = new HashSet<>();
            for (ServerPlayerEntity p : players) active.add(p.getUuid());

            ArrayList<UUID> toClean = new ArrayList<>();
            for (UUID u : lastPos.keySet()) {
                if (!active.contains(u)) toClean.add(u);
            }
            for (UUID u : toClean) {
                removeLight(server, u);
            }
        }
    }
    private void tryUpdateLight(ServerPlayerEntity player) {
        World world = player.getWorld();
        UUID id = player.getUuid();

        BlockPos previous = lastPos.get(id);
        net.minecraft.registry.RegistryKey<World> prevDim = lastDim.get(id);

        if (previous != null && prevDim != null && prevDim != world.getRegistryKey()) {
            removeLight(player, previous, prevDim, true);
            previous = null;
        }

        BlockPos target = findPlaceableAir(world, player.getBlockPos());
        if (target == null) {
            if (previous != null) {
                removeLight(player, previous, world.getRegistryKey(), true);
            }
            return;
        }

        if (previous != null && previous.equals(target)) {
            return;
        }

        if (previous != null) {
            removeLight(player, previous, world.getRegistryKey(), false);
        }

        BlockState state = Blocks.LIGHT.getDefaultState();
        world.setBlockState(target, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        lastPos.put(id, target);
        lastDim.put(id, world.getRegistryKey());
    }
    private void removeLight(MinecraftServer server, UUID id) {
        BlockPos prev = lastPos.remove(id);
        net.minecraft.registry.RegistryKey<World> dim = lastDim.remove(id);
        if (prev == null || dim == null) return;

        World world = server.getWorld(dim);
        if (world == null) return;

        BlockState cur = world.getBlockState(prev);
        if (cur.isOf(Blocks.LIGHT)) {
            world.removeBlock(prev, false);
        }
    }
    private void removeLight(ServerPlayerEntity player, BlockPos pos, net.minecraft.registry.RegistryKey<World> dim, boolean strictDim) {
        World world = player.getServer().getWorld(dim);
        if (world == null) return;
        BlockState cur = world.getBlockState(pos);
        if (cur.isOf(Blocks.LIGHT)) {
            world.removeBlock(pos, false);
        }
    }
    private BlockPos findPlaceableAir(World world, BlockPos base) {
        if (isAirOrLight(world, base)) return base;
        BlockPos above = base.up();
        if (isAirOrLight(world, above)) return above;
        BlockPos below = base.down();
        if (isAirOrLight(world, below)) return below;
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos p = base.offset(d);
            if (isAirOrLight(world, p)) return p;
        }
        return null;
    }
    private boolean isAirOrLight(World world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        return s.isAir() || s.isOf(Blocks.LIGHT);
    }
}
