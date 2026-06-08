package dev.maire.thinair.world.level.block;

import com.mojang.serialization.MapCodec;
import dev.maire.thinair.ThinAir;
import dev.maire.thinair.api.AirQualityHelper;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.init.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SafetyLanternBlock extends LanternBlock {
    public static final MapCodec<SafetyLanternBlock> CODEC = simpleCodec(SafetyLanternBlock::new);
    public static final EnumProperty<AirQualityLevel> AIR_QUALITY = EnumProperty.create("air_quality", AirQualityLevel.class);
    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;
    public static final String TAG_AIR_QUALITY_LEVEL = ThinAir.id("air_quality_level").toLanguageKey();

    public SafetyLanternBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(AIR_QUALITY, AirQualityLevel.GREEN).setValue(LOCKED, false));
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<LanternBlock> codec() {
        return (MapCodec<LanternBlock>) (MapCodec<?>) CODEC;
    }

    private static BlockState setAirQuality(Level level, BlockPos pos, BlockState blockState) {
        return blockState.setValue(
                AIR_QUALITY,
                AirQualityHelper.INSTANCE.getAirQualityAtLocation(level, Vec3.atCenterOf(pos), pos)
        );
    }

    public static ItemStack getDisplayItemStack(AirQualityLevel airQualityLevel) {
        ItemStack itemStack = new ItemStack(ModRegistry.SAFETY_LANTERN_ITEM.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_AIR_QUALITY_LEVEL, airQualityLevel.ordinal());
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return itemStack;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AIR_QUALITY, LOCKED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());

        BlockState blockState = this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        blockState = setAirQuality(context.getLevel(), context.getClickedPos(), blockState);

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() == Direction.Axis.Y) {
                BlockState out = blockState.setValue(HANGING, direction == Direction.UP);
                if (out.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return out;
                }
            }
        }

        return null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        updateAmbientAirQuality(level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        updateAmbientAirQuality(level, pos);
    }

    private static void updateAmbientAirQuality(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.hasProperty(AIR_QUALITY) || current.getValue(LOCKED)) {
            return;
        }
        AirQualityLevel ambient = AirQualityHelper.INSTANCE.getAirQualityAtLocation(
                level, Vec3.atCenterOf(pos), pos);
        if (current.getValue(AIR_QUALITY) != ambient) {
            level.setBlockAndUpdate(pos, current.setValue(AIR_QUALITY, ambient));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemUsed, BlockState blockState, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        AirQualityLevel lockedAirQuality = null;
        AirQualityLevel presentLockedAirQuality = null;
        boolean strippedDye = false;
        if (blockState.getValue(LOCKED)) {
            presentLockedAirQuality = blockState.getValue(AIR_QUALITY);
        }

        if (itemUsed.is(Items.GREEN_DYE) && presentLockedAirQuality != AirQualityLevel.GREEN) {
            lockedAirQuality = AirQualityLevel.GREEN;
        } else if (itemUsed.is(Items.BLUE_DYE) && presentLockedAirQuality != AirQualityLevel.BLUE) {
            lockedAirQuality = AirQualityLevel.BLUE;
        } else if (itemUsed.is(Items.YELLOW_DYE) && presentLockedAirQuality != AirQualityLevel.YELLOW) {
            lockedAirQuality = AirQualityLevel.YELLOW;
        } else if (itemUsed.is(Items.RED_DYE) && presentLockedAirQuality != AirQualityLevel.RED) {
            lockedAirQuality = AirQualityLevel.RED;
        } else if (itemUsed.getItem() instanceof AxeItem && blockState.getValue(LOCKED)) {
            strippedDye = true;
        }

        boolean didAnything = false;
        BlockState newBs = blockState;
        if (lockedAirQuality != null) {
            newBs = newBs.setValue(AIR_QUALITY, lockedAirQuality).setValue(LOCKED, true);
            level.levelEvent(player, 3003, pos, 0);
            if (!player.getAbilities().instabuild) {
                itemUsed.shrink(1);
            }

            didAnything = true;
        } else if (strippedDye) {
            level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3005, pos, 0);
            itemUsed.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            player.swing(hand);
            newBs = newBs.setValue(LOCKED, false);
            newBs = setAirQuality(level, pos, newBs);

            didAnything = true;
        }

        if (didAnything) {
            level.setBlockAndUpdate(pos, newBs);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return getDisplayItemStack(state.getValue(AIR_QUALITY));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        return blockState.getValue(AIR_QUALITY).getOutputSignal();
    }
}
