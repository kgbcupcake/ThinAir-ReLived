package dev.maire.thinair.client;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.client.renderer.entity.layers.RespiratorRenderer;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.integration.curios.CuriosClientIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModRegistry.SAFETY_LANTERN_ITEM.get(),
                    AIR_QUALITY_LEVEL_MODEL_PROPERTY,
                    (ItemStack itemStack, ClientLevel level, LivingEntity entity, int seed) ->
                            LanternDisplayResolver.resolveModelProperty(itemStack, entity)
            );
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.SIGNAL_TORCH_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.WALL_SIGNAL_TORCH_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModRegistry.SAFETY_LANTERN_BLOCK.get(), RenderType.cutout());
        });

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
