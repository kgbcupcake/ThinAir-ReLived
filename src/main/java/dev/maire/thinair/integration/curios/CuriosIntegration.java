package dev.maire.thinair.integration.curios;

import dev.maire.thinair.init.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CuriosIntegration {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CuriosIntegration::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                CuriosCapability.ITEM,
                (ItemStack stack, Void ctx) -> new ICurio() {
                    @Override
                    public ItemStack getStack() {
                        return stack;
                    }

                    @Override
                    public boolean canEquipFromUse(SlotContext slotContext) {
                        return true;
                    }
                },
                ModRegistry.RESPIRATOR_ITEM.get()
        );
    }
}
