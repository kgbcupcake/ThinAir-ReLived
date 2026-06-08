package dev.maire.thinair.handler;

import dev.maire.thinair.init.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class ReinforcedBladderCraftHandler {

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!result.is(ModRegistry.REINFORCED_AIR_BLADDER_ITEM.get())) {
            return;
        }

        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack ingredient = event.getInventory().getItem(i);
            if (ingredient.is(ModRegistry.AIR_BLADDER_ITEM.get())) {
                int damage = ingredient.getDamageValue();
                if (damage > 0) {
                    result.setDamageValue(damage);
                }
                break;
            }
        }
    }
}
