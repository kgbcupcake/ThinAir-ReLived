package dev.maire.thinair.handler;

import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.init.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

public class TickAirHandler {

    public static void onLivingBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (!AirQualityHelper.INSTANCE.isSensitiveToAirQuality(entity)) {
            return;
        }

        AirQualityLevel airQualityLevel = AirQualityHelper.INSTANCE.getAirQualityAtLocation(entity);

        if (entity instanceof ServerPlayer player) {
            ModRegistry.BREATHE_AIR_TRIGGER.get().trigger(player, airQualityLevel);
        }

        int airDelta = airQualityLevel.getAirAmountAfterProtection(entity);
        if (airDelta < 0) {
            event.setCanBreathe(false);
            event.setConsumeAirAmount(Math.abs(airDelta));
        } else if (airDelta > 0) {
            event.setCanBreathe(true);
            event.setRefillAirAmount(airDelta);
        } else {
            event.setCanBreathe(true);
            event.setRefillAirAmount(0);
        }
    }
}
