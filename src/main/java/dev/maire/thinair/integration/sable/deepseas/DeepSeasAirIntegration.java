package dev.maire.thinair.integration.sable.deepseas;

import com.maxenonyme.createsubmarine.submarine.block.entity.OxygeneDiffuserBlockEntity;
import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentDetector;
import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker;
import dev.maire.thinair.api.AirQualityLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeepSeasAirIntegration {
    private static final long OXYGEN_CACHE_TTL = 20L;
    private static final Map<UUID, Map<BlockPos, OxygenCache>> OXYGEN_CACHES = new HashMap<>();

    private DeepSeasAirIntegration() {
    }

    @Nullable
    public static AirQualityLevel getAirQuality(SubLevel subLevel, Vector3d localPosition) {
        UUID subLevelId = subLevel.getUniqueId();
        BlockPos position = BlockPos.containing(localPosition.x, localPosition.y, localPosition.z);
        long gameTime = subLevel.getLevel().getGameTime();

        for (CompartmentDetector.Component compartment : CompartmentTracker.getCompartments(subLevelId)) {
            if (!compartment.sealed() || CompartmentTracker.isCompromised(subLevelId, compartment.anchor())) {
                continue;
            }
            if (compartment.internal().contains(position)) {
                return hasActiveOxygenSupply(subLevel, compartment, gameTime)
                        ? AirQualityLevel.GREEN
                        : AirQualityLevel.ORANGE;
            }
        }
        return null;
    }

    public static void clearCaches() {
        OXYGEN_CACHES.clear();
    }

    private static boolean hasActiveOxygenSupply(
            SubLevel subLevel,
            CompartmentDetector.Component compartment,
            long gameTime
    ) {
        BlockPos anchor = compartment.anchor();
        Map<BlockPos, OxygenCache> subLevelCache =
                OXYGEN_CACHES.computeIfAbsent(subLevel.getUniqueId(), unused -> new HashMap<>());
        OxygenCache cache = subLevelCache.get(anchor);
        if (cache != null && gameTime - cache.updatedAt < OXYGEN_CACHE_TTL) {
            return cache.hasSupply;
        }

        boolean hasSupply = hasPoweredOxygenDiffuser(subLevel, compartment.internal())
                || hasPoweredOxygenDiffuser(subLevel, compartment.hull());
        subLevelCache.put(anchor, new OxygenCache(gameTime, hasSupply));
        if (subLevelCache.size() > 32) {
            subLevelCache.entrySet().removeIf(entry -> gameTime - entry.getValue().updatedAt > 200L);
        }
        if (OXYGEN_CACHES.size() > 64) {
            OXYGEN_CACHES.entrySet().removeIf(entry -> entry.getValue().values().stream()
                    .allMatch(oxygenCache -> gameTime - oxygenCache.updatedAt > 200L));
        }
        return hasSupply;
    }

    private static boolean hasPoweredOxygenDiffuser(SubLevel subLevel, Iterable<BlockPos> positions) {
        for (BlockPos position : positions) {
            LevelChunk chunk = subLevel.getPlot().getChunk(new ChunkPos(position));
            if (chunk == null) {
                continue;
            }
            BlockEntity blockEntity = chunk.getBlockEntity(position);
            if (blockEntity instanceof OxygeneDiffuserBlockEntity diffuser
                    && diffuser.oxygenTank.getFluidAmount() > 0
                    && subLevel.getLevel().hasNeighborSignal(position)) {
                return true;
            }
        }
        return false;
    }

    private record OxygenCache(long updatedAt, boolean hasSupply) {
    }
}
