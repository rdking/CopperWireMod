package net.apltd.copperwiremod.block;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public class CopperDiode extends AbstractRedstoneGateBlock implements CopperReadyDevice {
    public static final String BLOCK_NAME = "copper_diode";
    public static final IntProperty POWER = RedstoneWireBlock.POWER;
    public static final IntProperty STEP = CopperWire.STEP;
    public static final BooleanProperty LEFT = BooleanProperty.of("left");
    public static final BooleanProperty FRONT = BooleanProperty.of("front");
    public static final BooleanProperty RIGHT = BooleanProperty.of("right");
    public CopperDiode(AbstractBlock.Settings settings) {
        super(settings
                .luminance((BlockState blockState) -> {
                    int retval = 0;
                    if (blockState.isOf(ModBlocks.COPPER_DIODE)) {
                        retval = blockState.get(POWER);
                    }
                    return retval;
                })
        );

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(POWER, 0)
                        .with(STEP, 0)
                        .with(FACING, Direction.NORTH)
                        .with(LEFT, false)
                        .with(FRONT, true)
                        .with(RIGHT, false)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER);
        builder.add(STEP);
        builder.add(FACING);
        builder.add(LEFT);
        builder.add(FRONT);
        builder.add(RIGHT);
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
    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir) {
        int retval = 0;
        BlockState state = world.getBlockState(pos);
        BooleanProperty prop = propForDirection(state, dir);
        if ((prop != null) && state.get(prop)) {
            retval = state.get(POWER) << 4 | state.get(STEP);
        }
        return retval;
    }

    @Override
    public int getPowerStep(BlockView world, BlockPos pos, Direction dir) {
        int retval = 0;
        BlockState state = world.getBlockState(pos);
        BooleanProperty prop = propForDirection(state, dir.getOpposite());
        if ((prop != null) && state.get(prop)) {
            retval = state.get(STEP);
        }
        return retval;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        int retval = 0;
        BooleanProperty prop = propForDirection(state, direction.getOpposite());
        if ((prop != null) && state.get(prop)) {
            retval = state.get(POWER);
        }
        return retval;
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult retval = ActionResult.FAIL;

        if (hand == Hand.MAIN_HAND) {
            if (!player.getAbilities().allowModifyWorld) {
                retval = ActionResult.PASS;
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                Direction dir = getHitDirection(hit);
                if (handleWireHit(world, state, pos, dir)) {
                    BlockState cState = world.getBlockState(pos);

                    if (!(cState.get(LEFT) || cState.get(FRONT) || cState.get(RIGHT))) {
                        world.setBlockState(pos, state);
                        retval = ActionResult.CONSUME;
                    } else {
                        retval = ActionResult.SUCCESS;
                    }
                } else {
                    retval = ActionResult.CONSUME;
                }
            }
        }

        return retval;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        Direction facing = state.get(FACING);
        if (!world.isClient && pos.offset(facing.getOpposite()).equals(sourcePos)) {
            BlockState newState = update(world, state, pos);
            if (state != newState) {
                world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
                world.updateNeighborsAlways(pos, this);
            }
        }
    }

    private BlockState update(World world, BlockState state, BlockPos pos) {
        Direction facing = state.get(FACING);
        Direction srcDir = facing.getOpposite();
        BlockPos srcPos = pos.offset(srcDir);
        BlockState srcState = world.getBlockState(srcPos);
        int power = srcState.getWeakRedstonePower(world, srcPos, srcDir);
        int step = power > 0 ? 15 : 0;

        return state.with(POWER, power).with(STEP, step);
    }

    private BooleanProperty propForDirection(BlockState state, Direction dir) {
        List<Direction> nDirs = Arrays.asList(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        BooleanProperty[] props = { null, LEFT, FRONT, RIGHT };
        Direction back = state.get(FACING).getOpposite();
        int dirIndex = (4 + nDirs.indexOf(dir) - nDirs.indexOf(back)) % 4;
        return props[dirIndex];
    }

    private Direction getHitDirection(BlockHitResult hit) {
        Direction retval = null;
        if (hit.getSide() == Direction.UP) {
            BlockPos pos = hit.getBlockPos();
            Vec3d loc = hit.getPos();
            int x = (int) ((loc.getX() - pos.getX()) * 16);
            int z = (int) ((loc.getZ() - pos.getZ()) * 16);

            if (!((x >= 6) && (x < 10) && (z >= 6) && (z < 10))) {
                if (z < 8) {
                    if (x < z) {
                        retval = Direction.WEST;
                    } else if (x > 15 - z) {
                        retval = Direction.EAST;
                    } else {
                        retval = Direction.NORTH;
                    }
                } else {
                    if (x < 15 - z) {
                        retval = Direction.WEST;
                    } else if (x > z) {
                        retval = Direction.EAST;
                    } else {
                        retval = Direction.SOUTH;
                    }
                }
            }
        }

        return retval;
    }

    private boolean handleWireHit(World world, BlockState oldState, BlockPos pos, Direction dir) {
        boolean retval;
        BlockState newState = oldState;
        BooleanProperty prop = propForDirection(oldState, dir);

        if (prop != null) {
            newState = oldState.with(prop, !oldState.get(prop));
        }

        retval = newState != oldState;

        if (retval) {
            world.setBlockState(pos, newState, 3);
        }
        return retval;
    }
}
