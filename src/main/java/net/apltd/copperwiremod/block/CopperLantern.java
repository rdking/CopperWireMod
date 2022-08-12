package net.apltd.copperwiremod.block;

import static net.apltd.copperwiremod.util.CopperTools.CPtoRP;
import net.apltd.copperwiremod.util.CopperTools;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("deprecation")
public class CopperLantern extends Block implements CopperReadyDevice {
    public static final String BLOCK_NAME = "copper_lantern";

    public static final IntProperty CPOWER = CopperTools.CPOWER;
    public static final DirectionProperty FACING = DirectionProperty.of("facing",
            Arrays.stream(Direction.values()).filter((dir) -> dir != Direction.DOWN).toList());

    CopperLantern(AbstractBlock.Settings settings) {
        super(settings
                .luminance((BlockState state) -> CPtoRP(state.get(CPOWER)))
        );

        setDefaultState(getStateManager().getDefaultState()
                .with(CPOWER, 0)
                .with(FACING, Direction.UP)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CPOWER);
        builder.add(FACING);
        super.appendProperties(builder);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState retval = this.getDefaultState();
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        for (Direction dir : ctx.getPlacementDirections()) {
            Direction oDir = dir.getOpposite();
            if (dir != Direction.UP) {
                BlockState blockState = retval.with(FACING, oDir);
                if (blockState.canPlaceAt(world, pos)) {
                    retval = blockState;
                    break;
                }
            }
        }

        world.createAndScheduleBlockTick(pos, this, 2);

        return retval;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = state.get(FACING);
        double top = 15;
        double bottom = (facing == Direction.UP) ? 0 : 4;
        double north = (facing == Direction.SOUTH) ? 0 : (facing == Direction.NORTH) ? 5 : 4;
        double south = (facing == Direction.NORTH) ? 16 : (facing == Direction.SOUTH) ? 11 : 12;
        double east = (facing == Direction.WEST) ? 16 : (facing == Direction.EAST) ? 11 : 12;
        double west = (facing == Direction.EAST) ? 0 : (facing == Direction.WEST) ? 5 : 4;
        return Block.createCuboidShape(west, bottom, north, east, top, south);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction direction = state.get(FACING);
        BlockPos blockPos = pos.offset(direction.getOpposite());
        BlockState blockState = world.getBlockState(blockPos);
        return blockState.isSideSolidFullSquare(world, blockPos, direction);
    }

    @Override
    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir, Direction iDir) {
        BlockState state = world.getBlockState(pos);
        Direction facing = state.get(FACING);
        return (dir != Direction.DOWN) && ((facing == Direction.DOWN) || (dir != facing.getOpposite()))
                ? state.get(CPOWER)
                : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return (direction == Direction.DOWN)
                ? state.getWeakRedstonePower(world, pos, direction)
                : 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return (state.get(FACING) != direction)
                ? CPtoRP(state.get(CPOWER))
                : 0;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        BlockState retval = state;

        if (state.get(FACING) == Direction.UP) {
            if (direction == Direction.DOWN && !this.canPlaceAt(state, world, pos)) {
                retval = Blocks.AIR.getDefaultState();
            }
            else {
                retval = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
            }
        }
        else {
            if (direction.getOpposite() == state.get(FACING) && !state.canPlaceAt(world, pos)) {
                retval = Blocks.AIR.getDefaultState();
            }
        }

        return retval;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        Direction mount = state.get(FACING).getOpposite();
        int power = 240 - world.getEmittedRedstonePower(pos.offset(mount), mount) * 16;

        if (state.get(CPOWER) != power) {
            world.createAndScheduleBlockTick(pos, this, 2);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction mount = state.get(FACING).getOpposite();
        int power = 240 - world.getEmittedRedstonePower(pos.offset(mount), mount) * 16;

        if (state.get(CPOWER) != power) {
            BlockState newState = state.with(CPOWER, power);
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved) {
            for (Direction direction : Direction.values()) {
                world.updateNeighborsAlways(pos.offset(direction), this);
            }
        }
    }
}
