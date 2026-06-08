package dev.maire.thinair.client.renderer.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.client.LanternDisplayResolver;
import dev.maire.thinair.init.ModRegistry;
import dev.maire.thinair.mixin.client.ModelPartAccessor;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.ArrayList;

public class SafetyLanternCurioRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack matrices,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(slotContext.entity() instanceof Player player) || !(renderLayerParent.getModel() instanceof PlayerModel<?> playerModel)) {
            return;
        }

        boolean isWearingArmor = false;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.is(ItemTags.LEG_ARMOR) || armor.is(ItemTags.CHEST_ARMOR)) {
                isWearingArmor = true;
                break;
            }
        }

        float lanternTop = 11.0F / 16.0F;
        matrices.pushPose();

        float xOffset = 2.0F;
        float zOffset = -1.0F;
        Vec3 hipOffset = isWearingArmor
                ? new Vec3(xOffset + 0.05F, -1.25F, zOffset + 0.05F)
                : new Vec3(xOffset - 0.1F, -1.25F, zOffset - 0.1F);

        transformToModelPart(matrices, playerModel.body, hipOffset.x, hipOffset.y, hipOffset.z);
        matrices.translate(0.5F, lanternTop, 0.5F);

        float xRot = Math.min(0.0F, playerModel.rightLeg.xRot / 3.0F) - 0.1F;
        xRot -= playerModel.body.xRot;
        matrices.mulPose(Axis.ZP.rotation(xRot));
        matrices.translate(-0.5F, -lanternTop, -0.5F);

        AirQualityLevel airQualityLevel = LanternDisplayResolver.resolveAirQualityLevel(stack, player);
        BlockState blockState = ModRegistry.SAFETY_LANTERN_BLOCK.get()
                .defaultBlockState()
                .setValue(SafetyLanternBlock.AIR_QUALITY, airQualityLevel);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                blockState, matrices, multiBufferSource, light, OverlayTexture.NO_OVERLAY
        );
        matrices.popPose();
    }

    private static void transformToModelPart(PoseStack poseStack, ModelPart part, Number xPercent, Number yPercent, Number zPercent) {
        part.translateAndRotate(poseStack);
        Pair<Vec3, Vec3> aabb = getAabb(part);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        poseStack.translate(
                xPercent != null ? Mth.lerp((-xPercent.doubleValue() + 1.0D) / 2.0D, aabb.getFirst().x, aabb.getSecond().x) : 0.0D,
                yPercent != null ? Mth.lerp((-yPercent.doubleValue() + 1.0D) / 2.0D, aabb.getFirst().y, aabb.getSecond().y) : 0.0D,
                zPercent != null ? Mth.lerp((-zPercent.doubleValue() + 1.0D) / 2.0D, aabb.getFirst().z, aabb.getSecond().z) : 0.0D
        );
        poseStack.scale(8.0F, 8.0F, 8.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    private static Pair<Vec3, Vec3> getAabb(ModelPart part) {
        Vec3 min = new Vec3(0.0D, 0.0D, 0.0D);
        Vec3 max = new Vec3(0.0D, 0.0D, 0.0D);

        if (part.getClass().getSimpleName().contains("EMFModelPart")) {
            ArrayList<ModelPart> parts = new ArrayList<>();
            parts.add(part);
            parts.addAll(((ModelPartAccessor) (Object) part).getChildren().values());

            for (ModelPart modelPart : parts) {
                for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) modelPart).getCubes()) {
                    min = new Vec3(
                            Math.min(min.x, Math.min(cube.minX + modelPart.x, cube.maxX + modelPart.x)),
                            Math.min(min.y, Math.min(cube.minY + modelPart.y, cube.maxY + modelPart.y)),
                            Math.min(min.z, Math.min(cube.minZ + modelPart.z, cube.maxZ + modelPart.z))
                    );
                    max = new Vec3(
                            Math.max(max.x, Math.max(cube.minX + modelPart.x, cube.maxX + modelPart.x)),
                            Math.max(max.y, Math.max(cube.minY + modelPart.y, cube.maxY + modelPart.y)),
                            Math.max(max.z, Math.max(cube.minZ + modelPart.z, cube.maxZ + modelPart.z))
                    );
                }
            }
        } else {
            for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) part).getCubes()) {
                min = new Vec3(
                        Math.min(min.x, Math.min(cube.minX, cube.maxX)),
                        Math.min(min.y, Math.min(cube.minY, cube.maxY)),
                        Math.min(min.z, Math.min(cube.minZ, cube.maxZ))
                );
                max = new Vec3(
                        Math.max(max.x, Math.max(cube.minX, cube.maxX)),
                        Math.max(max.y, Math.max(cube.minY, cube.maxY)),
                        Math.max(max.z, Math.max(cube.minZ, cube.maxZ))
                );
            }
        }

        return Pair.of(min, max);
    }
}
