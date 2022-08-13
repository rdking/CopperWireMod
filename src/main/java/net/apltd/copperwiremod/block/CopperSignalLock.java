package net.apltd.copperwiremod.block;

import static net.apltd.copperwiremod.util.CopperTools.propForDirection;

import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CopperSignalLock extends AbstractRedstoneGateBlock {
    public static final String BLOCK_NAME = "copper_signallock";
    public static final IntProperty POWER = Properties.POWER;
    public static final BooleanProperty LOCKED = BooleanProperty.of("locked");
    public static final BooleanProperty LIT = BooleanProperty.of("lit");


    public CopperSignalLock(AbstractBlock.Settings settings) {
        super(settings);

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(POWER, 0)
                        .with(LOCKED, true)
                        .with(LIT, false)
                        .with(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER);
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

        if (locked != newState.get(LOCKED)) {
            newState = newState.with(LOCKED, locked);
        }
        if (!srcState.contains(prop) || srcState.get(prop).isConnected()) {
            power = srcState.getWeakRedstonePower(world, srcPos, srcDir);
        }
        newState = newState.with(LIT, power != 0);
        if (!locked && (power != newState.get(POWER))) {
            newState = newState.with(POWER, power);
        }
        return newState;
    }
}
