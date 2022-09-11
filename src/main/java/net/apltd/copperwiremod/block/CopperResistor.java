package net.apltd.copperwiremod.block;

import static net.apltd.copperwiremod.util.CopperTools.*;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class CopperResistor extends AbstractRedstoneGateBlock implements CopperReadyDevice {
    public static final String BLOCK_NAME = "copper_resistor";
    public static final IntProperty CPOWER = CopperPowerSource.CPOWER;
    private static final VoxelShape COPPER_RESISTOR_SHAPE_NS =
            Block.createCuboidShape(5.0D, 0.0D, 0.0D, 11.0D, 3.0D, 16.0D);
    private static final VoxelShape COPPER_RESISTOR_SHAPE_EW =
            Block.createCuboidShape(0.0D, 0.0D, 5.0D, 16.0D, 3.0D, 11.0D);
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
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return ((state.get(FACING) == Direction.NORTH) || (state.get(FACING) == Direction.SOUTH))
                ? COPPER_RESISTOR_SHAPE_NS : COPPER_RESISTOR_SHAPE_EW;
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
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.offset(Direction.DOWN);
        BlockState blockState = world.getBlockState(blockPos);
        return blockState.isSideSolidFullSquare(world, blockPos, Direction.DOWN);
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
