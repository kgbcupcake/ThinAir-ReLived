package dev.maire.thinair.world.item;

import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class AirBladderItem extends Item {

    public AirBladderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemInHand = player.getItemInHand(interactionHand);
        if (!canStartUsing(player, itemInHand)) {
            return InteractionResultHolder.pass(itemInHand);
        }
        return ItemUtils.startUsingInstantly(level, player, interactionHand);
    }

    private static boolean canStartUsing(LivingEntity entity, ItemStack itemStack) {
        AirQualityLevel airQualityLevel = AirQualityHelper.INSTANCE.getAirQualityAtLocation(entity);
        if (airQualityLevel.canRefillAir) {
            return itemStack.isDamaged();
        }
        return itemStack.getDamageValue() < itemStack.getMaxDamage()
                && entity.getAirSupply() < entity.getMaxAirSupply();
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int remainingUseDuration) {
        if (level.isClientSide()) {
            return;
        }

        ItemStack usingStack = entity.getUseItem();
        if (usingStack.isEmpty()) {
            entity.releaseUsingItem();
            return;
        }

        boolean stopUsing = true;
        AirQualityLevel airQualityLevel = AirQualityHelper.INSTANCE.getAirQualityAtLocation(entity);
        if (airQualityLevel.canRefillAir) {
            if (usingStack.isDamaged()) {
                usingStack.setDamageValue(usingStack.getDamageValue() - 4);
                stopUsing = false;
            }
        } else if (usingStack.getDamageValue() < usingStack.getMaxDamage()) {
            int refillTicks = 4;
            EquipmentSlot slot = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            while (refillTicks-- > 0 && entity.getAirSupply() < entity.getMaxAirSupply()) {
                entity.setAirSupply(entity.getAirSupply() + 1);
                usingStack.hurtAndBreak(1, entity, slot);
                stopUsing = false;
            }
        }

        if (!stopUsing) {
            if (remainingUseDuration <= this.getUseDuration(usingStack, entity) - 7 && remainingUseDuration % 4 == 0) {
                entity.playSound(this.getDrinkingSound(), 0.5F, entity.level().random.nextFloat() * 0.1F + 0.9F);
            }
        } else {
            entity.releaseUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!level.isClientSide()
                && livingEntity instanceof Player player
                && player.getAirSupply() >= player.getMaxAirSupply()
                && !AirQualityHelper.INSTANCE.getAirQualityAtLocation(livingEntity).canRefillAir) {
            player.getCooldowns().addCooldown(this, 150);
        }
        super.releaseUsing(itemStack, level, livingEntity, timeCharged);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 72000;
    }
}
