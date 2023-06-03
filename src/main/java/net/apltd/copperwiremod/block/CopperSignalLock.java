package net.apltd.copperwiremod.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import static net.apltd.copperwiremod.util.CopperTools.propForDirection;

public class CopperSignalLock extends AbstractRedstoneGateBlock implements CopperReadyDevice {
    public static final String BLOCK_NAME = "copper_signallock";
    public static final IntProperty POWER = RedstoneWireBlock.POWER;
    public static final IntProperty STEP = IntProperty.of("step", 0, 15);
    public static final BooleanProperty LOCKED = BooleanProperty.of("locked");
    public static final BooleanProperty LIT = BooleanProperty.of("lit");


    public CopperSignalLock(AbstractBlock.Settings settings) {
        super(settings);

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(POWER, 0)
                        .with(STEP, 0)
                        .with(LOCKED, true)
                        .with(LIT, false)
                        .with(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER);
        builder.add(STEP);
        builder.add(LOCKED);
        builder.add(LIT);
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
    protected int getUpdateDelayInternal(BlockState state) {
        return 2;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction facing = state.get(FACING);
        BlockState newState = update(world, state, pos);
        world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
        world.updateNeighbor(pos.offset(facing), this, pos);
    }

    @Override
    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir) {
        BlockState state = world.getBlockState(pos);
        return (dir == state.get(FACING)) ? (state.get(POWER) << 4 | state.get(STEP)) : 0;
    }

    @Override
    public int getPowerStep(BlockView world, BlockPos pos, Direction dir) {
        BlockState state = world.getBlockState(pos);
        return (dir.getOpposite() == state.get(FACING)) ? state.get(STEP) : 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return (direction.getOpposite() == state.get(FACING)) ? state.get(POWER) : 0;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        Direction facing = state.get(FACING);
        if (!world.isClient && !pos.offset(facing).equals(sourcePos)) {
            BlockState newState = update(world, state, pos);
            if (state != newState) {
                world.createAndScheduleBlockTick(pos, this, 2);
            }
        }
    }

    private BlockState update(World world, BlockState state, BlockPos pos) {
        BlockState newState = state;
        boolean locked = getMaxInputLevelSides(world, pos, state) == 0;
        Direction facing = newState.get(FACING);
        Direction srcDir = facing.getOpposite();
        BlockPos srcPos = pos.offset(srcDir);
        BlockState srcState = world.getBlockState(srcPos);
        Property<WireConnection> prop = propForDirection(facing);
        int power = 0;
        int step = 0;
        boolean isDown = false;
        if (srcState.isAir()) {
            srcPos = srcPos.down();
            srcState = world.getBlockState(srcPos);
            isDown = true;
        }

        if (isDown && (srcState.isOf(ModBlocks.COPPER_WIRE) && (srcState.get(prop) == WireConnection.UP))) {
            Block block = srcState.getBlock();
            int cPower = ((CopperReadyDevice) block).getCopperSignal(world, srcPos, facing);
            power = cPower >> 4;
            step = cPower & 15;
        }
        else {
            if (locked != newState.get(LOCKED)) {
                newState = newState.with(LOCKED, locked);
            }
            if (!srcState.contains(prop) || srcState.get(prop).isConnected()) {
                Block block = srcState.getBlock();
                if (block instanceof CopperReadyDevice) {
                    int cPower = ((CopperReadyDevice) block).getCopperSignal(world, srcPos, facing);
                    power = cPower >> 4;
                    step = cPower & 15;
                } else if (srcState.isSolidBlock(world, srcPos)) {
                    power = world.getReceivedStrongRedstonePower(srcPos);
                } else {
                    power = srcState.getWeakRedstonePower(world, srcPos, srcDir);
                    step = power > 0 ? 15 : 0;
                }
            }
        }

        newState = newState.with(LIT, power != 0);
        if (!locked && ((power << 4 | step) != (newState.get(POWER) << 4 | newState.get(STEP)))) {
            newState = newState.with(POWER, power).with(STEP, step);
        }
        return newState;
    }
}
