package dev.maire.thinair.helper;

import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.capability.AirBubblePositionsCapability;
import dev.maire.thinair.config.ThinAirConfig;
import dev.maire.thinair.init.ModCapabilities;
import dev.maire.thinair.init.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class AirQualityHelperImpl implements AirQualityHelper {

    @Override
    public AirQualityLevel getAirQualityAtLocation(Level level, Vec3 location) {
        return getAirQualityAtLocation(level, location, null);
    }

    @Override
    public AirQualityLevel getAirQualityAtLocation(Level level, Vec3 location, @Nullable BlockPos excludedBlockPos) {
        BlockPos blockPos = BlockPos.containing(location);
        if (excludedBlockPos == null || !excludedBlockPos.equals(blockPos)) {
            BlockState blockAtEyes = level.getBlockState(blockPos);
            AirQualityLevel airQualityAtEyes = AirQualityLevel.getAirQualityAtEyes(blockAtEyes);
            if (airQualityAtEyes != null) {
                return airQualityAtEyes;
            }
        }

        // Let's throw the player a bone and say the best air quality wins
        AirQualityLevel bestAirBubbleQuality = null;
        ChunkPos chunkAtCenter = new ChunkPos(BlockPos.containing(location));
        // max radius for a campfire is 32, so that means we check two chunks on each side.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                ChunkPos posInChunk = new ChunkPos(chunkAtCenter.x + x, chunkAtCenter.z + z);
                LevelChunk chunk = level.getChunkSource().getChunkNow(posInChunk.x, posInChunk.z);
                if (chunk == null) {
                    continue;
                }
                AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
                if (capability == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, AirQualityLevel> entry : capability.getAirBubblePositionsView().entrySet()) {
                    BlockPos bubblePos = entry.getKey();
                    if (bubblePos.equals(excludedBlockPos)) {
                        continue;
                    }
                    AirQualityLevel airQualityLevel = entry.getValue();
                    Objects.requireNonNull(airQualityLevel, "air quality level is null");
                    if (bestAirBubbleQuality == null || airQualityLevel.isBetterThan(bestAirBubbleQuality)) {
                        double distanceSq = new Vec3(bubblePos.getX() + 0.5, bubblePos.getY() + 0.5, bubblePos.getZ() + 0.5)
                                .distanceToSqr(location);
                        if (distanceSq < Math.pow(airQualityLevel.getAirProviderRadius(), 2.0)) {
                            if (airQualityLevel == AirQualityLevel.GREEN) {
                                return AirQualityLevel.GREEN;
                            } else {
                                bestAirBubbleQuality = airQualityLevel;
                            }
                        }
                    }
                }
            }
        }

        if (bestAirBubbleQuality != null) {
            return bestAirBubbleQuality;
        } else {
            return ThinAirConfig.getAirQualityAtLevelByDimension(
                    level.dimension().location(), (int) Math.round(location.y));
        }
    }

    @Override
    public boolean isSensitiveToAirQuality(LivingEntity entity) {
        return entity.getType().is(ModRegistry.AIR_QUALITY_SENSITIVE_ENTITY_TYPE_TAG)
                && (!(entity instanceof Player player) || !player.getAbilities().invulnerable);
    }
}
