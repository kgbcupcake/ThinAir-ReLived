package dev.maire.thinair.advancements.criterion;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.init.ModRegistry;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public class BreatheAirTrigger extends SimpleCriterionTrigger<BreatheAirTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, AirQualityLevel airQualityLevel) {
        super.trigger(player, instance -> instance.matches(airQualityLevel));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<List<AirQualityLevel>> allowedQualities) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                .forGetter(TriggerInstance::player),
                        AirQualityLevel.CODEC.listOf().optionalFieldOf("air_qualities")
                                .forGetter(TriggerInstance::allowedQualities)
                ).apply(instance, TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> breatheAir(AirQualityLevel... airQualityLevels) {
            return ModRegistry.BREATHE_AIR_TRIGGER.get().createCriterion(
                    new TriggerInstance(Optional.empty(), Optional.of(ImmutableList.copyOf(airQualityLevels)))
            );
        }

        public boolean matches(AirQualityLevel airQualityLevel) {
            return this.allowedQualities.isEmpty() || this.allowedQualities.get().contains(airQualityLevel);
        }
    }
}
