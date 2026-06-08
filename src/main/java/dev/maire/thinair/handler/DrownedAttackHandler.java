package dev.maire.thinair.handler;

import dev.maire.thinair.config.ThinAirConfig;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class DrownedAttackHandler {

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getSource().getEntity() instanceof Drowned
                && event.getSource().is(DamageTypes.MOB_ATTACK)) {
            entity.setAirSupply(entity.getAirSupply() - ThinAirConfig.drownedChoking.get());
        }
    }
}
