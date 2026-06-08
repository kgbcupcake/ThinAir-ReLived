package dev.maire.thinair.world.level.block;

import com.mojang.serialization.MapCodec;
import dev.maire.thinair.config.ThinAirConfig;
import dev.maire.thinair.init.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SignalTorchBlock extends TorchBlock {
    public static final MapCodec<SignalTorchBlock> CODEC = simpleCodec(SignalTorchBlock::new);

    public SignalTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }

    @Override
    public MapCodec<? extends TorchBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.7D;
        double z = pos.getZ() + 0.5D;
        double dx = (random.nextDouble() - 0.5) * 0.05;
        double dy = random.nextDouble() * 0.1;
        double dz = (random.nextDouble() - 0.5) * 0.05;
        level.addParticle(ParticleTypes.FIREWORK, x, y, z, dx, dy, dz);
        level.addParticle(this.flameParticle, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand interactionHand, BlockHitResult hitResult) {
        if (!ThinAirConfig.get().enableSignalTorches() || interactionHand != InteractionHand.MAIN_HAND || player.isDiscrete()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        BlockState bs = level.getBlockState(pos);

        Block nextBlock = null;
        float pitch = 1f;
        if (bs.is(Blocks.TORCH)) {
            nextBlock = ModRegistry.SIGNAL_TORCH_BLOCK.get();
        } else if (bs.is(Blocks.WALL_TORCH)) {
            nextBlock = ModRegistry.WALL_SIGNAL_TORCH_BLOCK.get();
        } else if (bs.is(ModRegistry.SIGNAL_TORCH_BLOCK.get())) {
            nextBlock = Blocks.TORCH;
            pitch = 0.8f;
        } else if (bs.is(ModRegistry.WALL_SIGNAL_TORCH_BLOCK.get())) {
            nextBlock = Blocks.WALL_TORCH;
            pitch = 0.8f;
        }
        if (nextBlock != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                ModRegistry.SIGNALIFY_TORCH_TRIGGER.get().trigger(serverPlayer, pos);
            }

            BlockState nextBs = nextBlock.defaultBlockState();
            if (bs.hasProperty(WallTorchBlock.FACING)) {
                nextBs = nextBs.setValue(WallTorchBlock.FACING, bs.getValue(WallTorchBlock.FACING));
            }
            level.setBlockAndUpdate(pos, nextBs);
            level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, pitch);
            player.swing(interactionHand);

            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
