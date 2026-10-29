package dev.maire.thinair.world.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class RespiratorItem extends Item {

    public RespiratorItem(Properties properties) {
        super(properties);
    }

    /**
     * Respiration relies on vanilla's per-EquipmentSlot enchantment lookup, which Curios slots never
     * satisfy - so a Respiration-enchanted respirator worn in the Curios head slot has no actual effect
     * on air duration. Refuse the enchantment outright rather than let players enchant for nothing.
     */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.RESPIRATION) && super.supportsEnchantment(stack, enchantment);
    }
}
