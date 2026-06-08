package dev.maire.thinair.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WallSignalTorchBlock extends WallTorchBlock {
    public static final MapCodec<WallSignalTorchBlock> CODEC = simpleCodec(WallSignalTorchBlock::new);

    public WallSignalTorchBlock(Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<WallTorchBlock> codec() {
        return (MapCodec<WallTorchBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var facing = state.getValue(FACING);
        var opposite = facing.getOpposite();
        double x = pos.getX() + 0.5D + 0.27D * opposite.getStepX();
        double y = pos.getY() + 0.92D;
        double z = pos.getZ() + 0.5D + 0.27D * opposite.getStepZ();
        double dx = (random.nextDouble() - 0.5) * 0.05 + facing.getStepX() * 0.01;
        double dy = random.nextDouble() * 0.1;
        double dz = (random.nextDouble() - 0.5) * 0.05 + facing.getStepZ() * 0.01;
        level.addParticle(ParticleTypes.FIREWORK, x, y, z, dx, dy, dz);
        level.addParticle(this.flameParticle, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
