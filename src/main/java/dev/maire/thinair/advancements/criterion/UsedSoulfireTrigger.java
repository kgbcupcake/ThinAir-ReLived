package dev.maire.thinair.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.maire.thinair.init.ModRegistry;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;

public class UsedSoulfireTrigger extends SimpleCriterionTrigger<UsedSoulfireTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ItemStack itemStack) {
        super.trigger(player, instance -> instance.matches(itemStack));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
                ).apply(instance, TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> usedSoulfire(ItemLike item) {
            return ModRegistry.USED_SOULFIRE_TRIGGER.get().createCriterion(
                    new TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(item).build()))
            );
        }

        public boolean matches(ItemStack itemStack) {
            return this.item.isEmpty() || this.item.get().test(itemStack);
        }
    }
}
