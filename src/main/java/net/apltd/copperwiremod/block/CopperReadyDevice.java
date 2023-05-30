package net.apltd.copperwiremod.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public interface CopperReadyDevice {
    int getCopperSignal(BlockView world, BlockPos pos, Direction dir);
    int getPowerStep(BlockView world, BlockPos pos, Direction dir);
}
