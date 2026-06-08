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
        AirQualityLevel airQualityLevel = AirQualityHelper.INSTANCE.getAirQualityAtLocation(player);
        ItemStack itemInHand = player.getItemInHand(interactionHand);
        if (airQualityLevel.canRefillAir && itemInHand.isDamaged() || !airQualityLevel.canRefillAir && itemInHand.getDamageValue() < itemInHand.getMaxDamage() && player.getAirSupply() < player.getMaxAirSupply()) {
            return ItemUtils.startUsingInstantly(level, player, interactionHand);
        } else {
            return super.use(level, player, interactionHand);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int remainingUseDuration) {
        boolean stopUsing = true;
        AirQualityLevel airQualityLevel = AirQualityHelper.INSTANCE.getAirQualityAtLocation(entity);
        if (airQualityLevel.canRefillAir) {
            if (itemStack.isDamaged()) {
                itemStack.setDamageValue(itemStack.getDamageValue() - 4);
                stopUsing = false;
            }
        } else if (itemStack.getDamageValue() < itemStack.getMaxDamage()) {
            int i = 4;
            EquipmentSlot slot = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            while (i-- > 0 && entity.getAirSupply() < entity.getMaxAirSupply()) {
                entity.setAirSupply(entity.getAirSupply() + 1);
                itemStack.hurtAndBreak(1, entity, slot);
                stopUsing = false;
            }
        }
        if (!stopUsing) {
            if (remainingUseDuration <= this.getUseDuration(itemStack, entity) - 7 && remainingUseDuration % 4 == 0) {
                entity.playSound(this.getDrinkingSound(), 0.5F, entity.level().random.nextFloat() * 0.1F + 0.9F);
            }
        } else {
            entity.releaseUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player && player.getAirSupply() >= player.getMaxAirSupply()) {
            if (!AirQualityHelper.INSTANCE.getAirQualityAtLocation(livingEntity).canRefillAir) {
                player.getCooldowns().addCooldown(this, 150);
            }
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
