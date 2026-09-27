package dev.maire.thinair.integration.sable;

import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.integration.sable.deepseas.DeepSeasAirIntegration;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SableAirIntegration {
    private static final int MAX_PROVIDER_RADIUS = 32;
    private static final long PROVIDER_CACHE_TTL = 20L;
    private static final Map<UUID, ProviderCache> PROVIDER_CACHES = new HashMap<>();

    private SableAirIntegration() {
    }

    @Nullable
    public static AirQualityLevel getAirQualityAtLocation(
            Level level,
            Vec3 location,
            @Nullable BlockPos excludedBlockPos,
            boolean useDeepSeas
    ) {
        if (level.isClientSide) {
            return null;
        }

        SubLevelLocation subLevelLocation = findSubLevel(level, location);
        if (subLevelLocation == null) {
            return null;
        }

        AirQualityLevel best = null;
        if (useDeepSeas) {
            best = DeepSeasAirIntegration.getAirQuality(subLevelLocation.subLevel, subLevelLocation.localPosition);
        }

        BlockPos localBlockPos = BlockPos.containing(
                subLevelLocation.localPosition.x,
                subLevelLocation.localPosition.y,
                subLevelLocation.localPosition.z
        );
        BlockPos localExcludedBlockPos = toLocalBlockPos(subLevelLocation.subLevel, excludedBlockPos);
        LevelPlot plot = subLevelLocation.subLevel.getPlot();
        LevelChunk eyeChunk = plot.getChunk(new ChunkPos(localBlockPos));
        if (eyeChunk != null && !localBlockPos.equals(localExcludedBlockPos)) {
            BlockState blockAtEyes = eyeChunk.getBlockState(localBlockPos);
            AirQualityLevel qualityAtEyes = AirQualityLevel.getAirQualityAtEyes(blockAtEyes);
            if (qualityAtEyes != null && (best == null || qualityAtEyes.isBetterThan(best))) {
                best = qualityAtEyes;
            }
        }

        for (AirProvider provider : getAirProviders(subLevelLocation.subLevel)) {
            if (provider.position.equals(localExcludedBlockPos)) {
                continue;
            }
            double distanceSq = new Vec3(
                    provider.position.getX() + 0.5,
                    provider.position.getY() + 0.5,
                    provider.position.getZ() + 0.5
            ).distanceToSqr(
                    subLevelLocation.localPosition.x,
                    subLevelLocation.localPosition.y,
                    subLevelLocation.localPosition.z
            );
            if (distanceSq < Math.pow(provider.quality.getAirProviderRadius(), 2.0)
                    && (best == null || provider.quality.isBetterThan(best))) {
                best = provider.quality;
            }
        }

        return best;
    }

    @Nullable
    private static BlockPos toLocalBlockPos(SubLevel subLevel, @Nullable BlockPos worldBlockPos) {
        if (worldBlockPos == null) {
            return null;
        }
        Vector3d localPosition = new Vector3d(
                worldBlockPos.getX() + 0.5,
                worldBlockPos.getY() + 0.5,
                worldBlockPos.getZ() + 0.5
        );
        subLevel.logicalPose().transformPositionInverse(localPosition);
        return BlockPos.containing(localPosition.x, localPosition.y, localPosition.z);
    }

    public static void clearCaches() {
        PROVIDER_CACHES.clear();
        if (ModList.get().isLoaded("create_submarine")) {
            DeepSeasAirIntegration.clearCaches();
        }
    }

    @Nullable
    private static SubLevelLocation findSubLevel(Level level, Vec3 location) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.getLevel() != level || subLevel.getPlot() == null) {
                continue;
            }

            Vector3d localPosition = new Vector3d(location.x, location.y, location.z);
            subLevel.logicalPose().transformPositionInverse(localPosition);

            BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
            if (localPosition.x >= bounds.minX() - MAX_PROVIDER_RADIUS
                    && localPosition.x <= bounds.maxX() + 1.0 + MAX_PROVIDER_RADIUS
                    && localPosition.y >= bounds.minY() - MAX_PROVIDER_RADIUS
                    && localPosition.y <= bounds.maxY() + 1.0 + MAX_PROVIDER_RADIUS
                    && localPosition.z >= bounds.minZ() - MAX_PROVIDER_RADIUS
                    && localPosition.z <= bounds.maxZ() + 1.0 + MAX_PROVIDER_RADIUS) {
                return new SubLevelLocation(subLevel, localPosition);
            }
        }
        return null;
    }

    private static List<AirProvider> getAirProviders(SubLevel subLevel) {
        long gameTime = subLevel.getLevel().getGameTime();
        ProviderCache cache = PROVIDER_CACHES.get(subLevel.getUniqueId());
        if (cache == null || gameTime - cache.updatedAt >= PROVIDER_CACHE_TTL) {
            cache = new ProviderCache(gameTime, scanAirProviders(subLevel.getPlot()));
            PROVIDER_CACHES.put(subLevel.getUniqueId(), cache);
            pruneCaches(gameTime);
        }
        return cache.providers;
    }

    private static List<AirProvider> scanAirProviders(LevelPlot plot) {
        List<AirProvider> providers = new ArrayList<>();
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            ChunkPos chunkPos = holder.getPos();
            int minX = chunkPos.getMinBlockX();
            int minZ = chunkPos.getMinBlockZ();
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    int maxY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = chunk.getMinBuildHeight(); y < maxY; y++) {
                        BlockPos position = new BlockPos(x, y, z);
                        AirQualityLevel quality = AirQualityLevel.getAirQualityFromBlock(chunk.getBlockState(position));
                        if (quality != null) {
                            providers.add(new AirProvider(position, quality));
                        }
                    }
                }
            }
        }
        return List.copyOf(providers);
    }

    private static void pruneCaches(long gameTime) {
        if (PROVIDER_CACHES.size() > 64) {
            PROVIDER_CACHES.entrySet().removeIf(entry -> gameTime - entry.getValue().updatedAt > 200L);
        }
    }

    private record SubLevelLocation(SubLevel subLevel, Vector3d localPosition) {
    }

    private record AirProvider(BlockPos position, AirQualityLevel quality) {
    }

    private record ProviderCache(long updatedAt, List<AirProvider> providers) {
    }
}
