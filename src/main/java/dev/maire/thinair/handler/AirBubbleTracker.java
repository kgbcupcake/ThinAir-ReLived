package dev.maire.thinair.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.maire.thinair.ThinAir;
import dev.maire.thinair.ThinAirMod;
import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.capability.AirBubblePositionsCapability;
import dev.maire.thinair.init.ModCapabilities;
import dev.maire.thinair.network.ClientboundChunkAirQualityPacket;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AirBubbleTracker {
    // Chunk positions repeat across dimensions, so all scanning state is kept per dimension.
    private static final Map<ResourceKey<Level>, LevelState> LEVEL_STATES = new ConcurrentHashMap<>();

    private static final class LevelState {
        final Set<ChunkPos> chunksToScan = Collections.synchronizedSet(Sets.newHashSet());
        final Set<ChunkPos> dirtyChunks = Collections.synchronizedSet(Sets.newHashSet());
        final List<Map.Entry<ChunkPos, BlockPos>> chunkScanningProgress =
                Collections.synchronizedList(Lists.newLinkedList());
    }

    private static LevelState state(ServerLevel level) {
        return LEVEL_STATES.computeIfAbsent(level.dimension(), key -> new LevelState());
    }

    public static void onBlockStateChange(ServerLevel level, BlockPos pos, BlockState oldBlockState, BlockState newBlockState) {
        if (!level.getServer().isSameThread()) return;
        ChunkPos chunkPos = new ChunkPos(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        AirQualityLevel newAirQuality = AirQualityLevel.getAirQualityFromBlock(newBlockState);
        AirBubblePositionsCapability capability = chunk != null ? ModCapabilities.get(chunk) : null;
        if (capability != null) {
            // Compare against what is actually tracked at this position rather than what the old block
            // state would provide now: the tracked entry can be out of date (block tags changed, or it
            // was saved by a version that tracked different blocks), and must not be left behind.
            Map<BlockPos, AirQualityLevel> positions = capability.getAirBubblePositions();
            AirQualityLevel tracked = positions.get(pos);
            if (tracked != newAirQuality) {
                if (tracked != null) {
                    positions.remove(pos);
                    ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                            chunk.getPos(), Map.of(pos, tracked), ClientboundChunkAirQualityPacket.Mode.REMOVE));
                }
                if (newAirQuality != null) {
                    positions.put(pos, newAirQuality);
                    ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                            chunk.getPos(), Map.of(pos, newAirQuality), ClientboundChunkAirQualityPacket.Mode.ADD));
                }
                chunk.setUnsaved(true);
            }
        }
        state(level).dirtyChunks.add(chunkPos);
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        LevelState state = state(level);
        if (state.chunksToScan.add(chunk.getPos())) {
            state.chunkScanningProgress.add(Map.entry(chunk.getPos(), getChunkStartingPosition(chunk)));
        }
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        state(level).chunksToScan.remove(chunk.getPos());
    }

    public static void onChunkWatch(ServerPlayer player, LevelChunk chunk, ServerLevel level) {
        AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
        if (capability == null) {
            return;
        }
        Map<BlockPos, AirQualityLevel> airBubblePositions = capability.getAirBubblePositionsView();
        ThinAirMod.sendToPlayer(player, new ClientboundChunkAirQualityPacket(
                chunk.getPos(), airBubblePositions, ClientboundChunkAirQualityPacket.Mode.REPLACE));
    }

    public static void onLevelUnload(MinecraftServer server, ServerLevel level) {
        LEVEL_STATES.remove(level.dimension());
    }

    public static void onEndLevelTick(MinecraftServer server, ServerLevel level) {
        LevelState state = state(level);
        final Set<ChunkPos> CHUNKS_TO_SCAN = state.chunksToScan;
        final Set<ChunkPos> DIRTY_CHUNKS = state.dirtyChunks;
        final List<Map.Entry<ChunkPos, BlockPos>> CHUNK_SCANNING_PROGRESS = state.chunkScanningProgress;
        try {
        if (!CHUNK_SCANNING_PROGRESS.isEmpty()) {
            if (!level.players().isEmpty() && server.getTickCount() % 200 == 0) {
                CHUNK_SCANNING_PROGRESS.sort(Comparator.comparingInt(entry -> {
                    BlockPos worldPosition = entry.getKey().getWorldPosition();
                    return level.players().stream()
                            .map(player -> player.chunkPosition().getWorldPosition())
                            .mapToInt(playerPos -> (int) playerPos.distSqr(worldPosition))
                            .min()
                            .orElse(0);
                }));
            }
            ListIterator<Map.Entry<ChunkPos, BlockPos>> iterator = CHUNK_SCANNING_PROGRESS.listIterator();
            Map.Entry<ChunkPos, BlockPos> entry = iterator.next();
            ChunkPos chunkPos = entry.getKey();
            if (CHUNKS_TO_SCAN.contains(chunkPos)) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                if (chunk != null) {
                    AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
                    if (capability == null) {
                        return;
                    }
                    if (capability.getSkipCountLeft() <= 0) {
                        capability.setSkipCountLeft(8);
                        HashMap<BlockPos, AirQualityLevel> airBubblePositions = Maps.newHashMap();
                        BlockPos blockPos = collectAirQualityPositions(chunk, entry.getValue(), airBubblePositions);
                        boolean markDirty = false;
                        if (entry.getValue().equals(getChunkStartingPosition(chunk))) {
                            capability.getAirBubblePositions().clear();
                            capability.getAirBubblePositions().putAll(airBubblePositions);
                            ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                                    chunkPos, airBubblePositions, ClientboundChunkAirQualityPacket.Mode.REPLACE));
                            markDirty = true;
                        } else if (!airBubblePositions.isEmpty()) {
                            capability.getAirBubblePositions().putAll(airBubblePositions);
                            ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                                    chunkPos, airBubblePositions, ClientboundChunkAirQualityPacket.Mode.ADD));
                            markDirty = true;
                        }
                        if (markDirty) {
                            chunk.setUnsaved(true);
                        }
                        if (blockPos != null) {
                            iterator.set(Map.entry(chunkPos, blockPos));
                            return;
                        }
                    } else {
                        capability.setSkipCountLeft(capability.getSkipCountLeft() - 1);
                        // No full rescan this time, but still drop tracked entries whose block no
                        // longer provides that air quality.
                        if (removeStaleAirBubbles(chunk, capability)) {
                            chunk.setUnsaved(true);
                            DIRTY_CHUNKS.add(chunkPos);
                        }
                    }
                } else {
                    return;
                }
            }
            iterator.remove();
            CHUNKS_TO_SCAN.remove(chunkPos);
        }
        } finally {
            if (!DIRTY_CHUNKS.isEmpty()) {
                Set<ChunkPos> dirtyChunks;
                synchronized (DIRTY_CHUNKS) {
                    dirtyChunks = Sets.newHashSet(DIRTY_CHUNKS);
                    DIRTY_CHUNKS.clear();
                }
                for (ChunkPos dirtyChunkPos : dirtyChunks) {
                    updateLanternsInChunk(level, dirtyChunkPos);
                }
            }
        }
    }

    private static boolean removeStaleAirBubbles(LevelChunk chunk, AirBubblePositionsCapability capability) {
        Map<BlockPos, AirQualityLevel> stale = Maps.newHashMap();
        Map<BlockPos, AirQualityLevel> corrected = Maps.newHashMap();
        for (Map.Entry<BlockPos, AirQualityLevel> entry : capability.getAirBubblePositions().entrySet()) {
            AirQualityLevel actual = AirQualityLevel.getAirQualityFromBlock(chunk.getBlockState(entry.getKey()));
            if (actual != entry.getValue()) {
                stale.put(entry.getKey(), entry.getValue());
                if (actual != null) {
                    corrected.put(entry.getKey(), actual);
                }
            }
        }
        if (stale.isEmpty()) {
            return false;
        }
        capability.getAirBubblePositions().keySet().removeAll(stale.keySet());
        capability.getAirBubblePositions().putAll(corrected);
        ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                chunk.getPos(), stale, ClientboundChunkAirQualityPacket.Mode.REMOVE));
        if (!corrected.isEmpty()) {
            ThinAirMod.sendToAllTracking(chunk, new ClientboundChunkAirQualityPacket(
                    chunk.getPos(), corrected, ClientboundChunkAirQualityPacket.Mode.ADD));
        }
        ThinAir.LOGGER.debug("Removed {} stale air bubbles in chunk {}", stale.size(), chunk.getPos());
        return true;
    }

    private static void updateLanternsInChunk(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            return;
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState blockState = chunk.getBlockState(blockPos);
                    if (!(blockState.getBlock() instanceof SafetyLanternBlock)) {
                        continue;
                    }
                    if (blockState.getValue(SafetyLanternBlock.LOCKED)) {
                        continue;
                    }
                    AirQualityLevel ambient = AirQualityHelper.INSTANCE.getAirQualityAtLocation(
                            level, Vec3.atCenterOf(blockPos), blockPos);
                    if (blockState.getValue(SafetyLanternBlock.AIR_QUALITY) != ambient) {
                        level.setBlockAndUpdate(blockPos, blockState.setValue(SafetyLanternBlock.AIR_QUALITY, ambient));
                    }
                }
            }
        }
    }

    private static BlockPos getChunkStartingPosition(LevelChunk chunk) {
        int posX = chunk.getPos().getMinBlockX();
        int posY = chunk.getMinBuildHeight();
        int posZ = chunk.getPos().getMinBlockZ();
        return new BlockPos(posX, posY, posZ);
    }

    @Nullable
    private static BlockPos collectAirQualityPositions(
            LevelChunk chunk, BlockPos startingPosition, Map<BlockPos, AirQualityLevel> airBubbleEntries) {
        int minX = chunk.getPos().getMinBlockX();
        int minY = chunk.getMinBuildHeight();
        int minZ = chunk.getPos().getMinBlockZ();
        int startX = startingPosition.getX() - minX;
        int startY = startingPosition.getY();
        int startZ = startingPosition.getZ() - minZ;
        int iterations = 0;
        for (int dx = startX; dx < 16; dx++, startX = 0) {
            for (int dz = startZ; dz < 16; dz++, startZ = 0) {
                int posX = minX + dx;
                int posZ = minZ + dz;
                int maxY = chunk.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, posX, posZ);
                for (int posY = startY; posY < maxY; posY++, startY = minY, iterations++) {
                    BlockPos blockPos = new BlockPos(posX, posY, posZ);
                    if (iterations >= 98304) {
                        return blockPos;
                    }
                    BlockState blockState = chunk.getBlockState(blockPos);
                    AirQualityLevel airQualityLevel = AirQualityLevel.getAirQualityFromBlock(blockState);
                    if (airQualityLevel != null) {
                        airBubbleEntries.put(blockPos, airQualityLevel);
                    }
                }
            }
        }
        return null;
    }
}
