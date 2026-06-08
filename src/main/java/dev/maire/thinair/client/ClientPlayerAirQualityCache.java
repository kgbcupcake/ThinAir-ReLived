package dev.maire.thinair.client;

import dev.maire.thinair.api.AirQualityLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class ClientPlayerAirQualityCache {

    private static final Map<Integer, AirQualityLevel> LEVELS = new ConcurrentHashMap<>();

    private ClientPlayerAirQualityCache() {
    }

    public static void put(int entityId, AirQualityLevel airQualityLevel) {
        LEVELS.put(entityId, airQualityLevel);
    }

    @Nullable
    public static AirQualityLevel get(int entityId) {
        return LEVELS.get(entityId);
    }

    public static void clear() {
        LEVELS.clear();
    }
}
