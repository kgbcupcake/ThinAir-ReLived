package dev.maire.thinair.client;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.client.renderer.entity.layers.RespiratorRenderer;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.integration.curios.CuriosClientIntegration;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(modid = ThinAir.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ThinAirClient {

    public static final ResourceLocation AIR_QUALITY_LEVEL_MODEL_PROPERTY = ThinAir.id("air_quality_level");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModRegistry.SAFETY_LANTERN_ITEM.get(),
                AIR_QUALITY_LEVEL_MODEL_PROPERTY,
                (ItemStack itemStack, ClientLevel level, LivingEntity entity, int seed) -> {
                    CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag compoundTag = customData.copyTag();
                        if (compoundTag.contains(SafetyLanternBlock.TAG_AIR_QUALITY_LEVEL, Tag.TAG_INT)) {
                            int airQualityLevel = compoundTag.getInt(SafetyLanternBlock.TAG_AIR_QUALITY_LEVEL);
                            return AirQualityLevel.values()[airQualityLevel].getItemModelProperty();
                        }
                    }
                    if (entity == null && itemStack.getEntityRepresentation() instanceof LivingEntity livingEntity) {
                        entity = livingEntity;
                    }
                    AirQualityLevel airQualityAtLocation;
                    if (entity != null) {
                        airQualityAtLocation = AirQualityHelper.INSTANCE.getAirQualityAtLocation(entity);
                    } else {
                        airQualityAtLocation = AirQualityLevel.YELLOW;
                    }
                    return airQualityAtLocation.getItemModelProperty();
                }
        ));

        if (ModList.get().isLoaded("curios")) {
            CuriosClientIntegration.registerCuriosRenderer();
        }
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                RespiratorRenderer.PLAYER_RESPIRATOR_LAYER,
                () -> LayerDefinition.create(
                        HumanoidModel.createMesh(new CubeDeformation(1.02F), 0.0F),
                        64,
                        32
                )
        );
    }

    @SubscribeEvent
    public static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
                RespiratorRenderer.bakeModel(Minecraft.getInstance().getEntityModels());
            }
        });
    }
}
