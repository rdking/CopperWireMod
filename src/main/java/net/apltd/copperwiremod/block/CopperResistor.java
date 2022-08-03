package net.apltd.copperwiremod.block;

import static net.apltd.copperwiremod.util.CopperTools.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CopperResistor extends AbstractRedstoneGateBlock implements CopperReadyDevice {
    public static final String BLOCK_NAME = "copper_resistor";
    public static final IntProperty CPOWER = CopperPowerSource.CPOWER;
    public CopperResistor(AbstractBlock.Settings settings) {
        super(settings
                .luminance((BlockState blockState) -> {
                    int retval = 0;
                    if (blockState.isOf(ModBlocks.COPPER_RESISTOR)) {
                        retval = CPtoRP(blockState.get(CopperResistor.CPOWER));
                    }
                    return retval;
                })
        );

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(CPOWER, 0)
                        .with(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CPOWER);
        builder.add(FACING);
        super.appendProperties(builder);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = getDefaultState()
                .with(FACING, ctx.getPlayerFacing());
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        state = update(world, state, pos);
        return state;
    }

    @Override
    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir, Direction iDir) {
        BlockState state = world.getBlockState(pos);
        return dir == state.get(FACING) ? world.getBlockState(pos).get(CPOWER) : 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return direction.getOpposite() == state.get(FACING) ? CPtoRP(state.get(CPOWER)) : 0;
    }

    @Override
    protected int getUpdateDelayInternal(BlockState state) {
        return 0;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        /* Necessary to block side effects of AbstractRedstoneGateBlock. */
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        Direction facing = state.get(FACING);
        if (!world.isClient && pos.offset(facing.getOpposite()).equals(sourcePos)) {
            BlockState newState = update(world, state, pos);
            if (state != newState) {
                world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
                world.updateNeighbor(pos.offset(facing), this, pos);
            }
        }
    }

    private BlockState update(World world, BlockState state, BlockPos pos) {
        Direction facing = state.get(FACING);
        Direction srcDir = facing.getOpposite();
        BlockPos srcPos = pos.offset(srcDir);
        BlockState srcState = world.getBlockState(srcPos);
        BlockState newState = state;
        Property<WireConnection> prop = propForDirection(facing);
        int power = 0;
        if (!srcState.contains(prop) || srcState.get(prop).isConnected()) {
            power = Math.max(0, srcState.getWeakRedstonePower(world, srcPos, srcDir) - 1) * 16;
        }
        if (power != state.get(CPOWER)) {
            newState = state.with(CPOWER, power);
        }
        return newState;
    }

}
