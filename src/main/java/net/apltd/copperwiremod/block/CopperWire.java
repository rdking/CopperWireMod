package net.apltd.copperwiremod.block;

import static net.apltd.copperwiremod.util.CopperTools.*;
import net.apltd.copperwiremod.blockentity.CopperWireEntity;
import net.apltd.copperwiremod.util.HitSpot;
import net.apltd.copperwiremod.util.RelevantDirMode;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("deprecation")
public class CopperWire extends AbstractRedstoneGateBlock implements CopperReadyDevice, BlockEntityProvider, Waterloggable {
    public static final EnumProperty<WireConnection> NORTH = RedstoneWireBlock.WIRE_CONNECTION_NORTH;
    public static final EnumProperty<WireConnection> EAST = RedstoneWireBlock.WIRE_CONNECTION_EAST;
    public static final EnumProperty<WireConnection> SOUTH = RedstoneWireBlock.WIRE_CONNECTION_SOUTH;
    public static final EnumProperty<WireConnection> WEST = RedstoneWireBlock.WIRE_CONNECTION_WEST;
    public static final IntProperty POWER = RedstoneWireBlock.POWER;
    public static final IntProperty STEP = IntProperty.of("step", 0, 15);
    public static final BooleanProperty VERTICAL = BooleanProperty.of("vertical");
    public static final BooleanProperty HOP = BooleanProperty.of("hop");
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape COPPER_WIRE_SHAPE =
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public static final String BLOCK_NAME = "copperwire";
    private static final Logger LOGGER = LoggerFactory.getLogger(BLOCK_NAME);

    public CopperWire(Settings settings) {
        super(settings
                .luminance((BlockState blockState) -> blockState.get(POWER))
        );

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(NORTH, WireConnection.SIDE)
                        .with(EAST, WireConnection.NONE)
                        .with(SOUTH, WireConnection.SIDE)
                        .with(WEST, WireConnection.NONE)
                        .with(POWER, 0)
                        .with(STEP, 0)
                        .with(VERTICAL, false)
                        .with(HOP, false)
                        .with(WATERLOGGED, false)
                        .with(FACING, Direction.NORTH)
        );
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("block.copperwiremod.copperwire.tooltip"));
        super.appendTooltip(stack, world, tooltip, options);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH);
        builder.add(EAST);
        builder.add(SOUTH);
        builder.add(WEST);
        builder.add(POWER);
        builder.add(STEP);
        builder.add(VERTICAL);
        builder.add(HOP);
        builder.add(WATERLOGGED);
        builder.add(FACING);
        super.appendProperties(builder);
    }

    @Override
    protected int getUpdateDelayInternal(BlockState state) {
        return 0;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = getDefaultState()
                .with(WATERLOGGED, world.getFluidState(pos).getFluid() == Fluids.WATER);

        Direction dir = ctx.getPlayerFacing();
        BlockPos downPos = pos.down();
        BlockState downState = world.getBlockState(downPos);
        if (downState.isOf(this)) {
            state = state.with(VERTICAL, true);
            for (Direction vdir : Direction.Type.HORIZONTAL) {
                EnumProperty<WireConnection> prop = propForDirection(vdir);
                if ((downState.get(prop) == WireConnection.UP) && isWallInDirection(world, vdir, pos)) {
                    state = state.with(prop, WireConnection.UP);
                } else {
                    state = state.with(prop, WireConnection.NONE);
                }
            }
        }
        else if ((dir == Direction.EAST) || (dir == Direction.WEST)) {
            state = state
                    .with(NORTH, WireConnection.NONE)
                    .with(SOUTH, WireConnection.NONE)
                    .with(EAST, WireConnection.SIDE)
                    .with(WEST, WireConnection.SIDE);
        }

        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.createAndScheduleBlockTick(pos, Blocks.WATER, Fluids.WATER.getTickRate(world));
            updatePower(state, (World)world, pos, true, true);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CopperWireEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!world.isClient) {
            updatePower(state, world, pos, true, true);
        }
        world.updateNeighbors(pos, this);
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
                if (!state.get(VERTICAL) && handleWireHit(world, state, pos, getHitSpot(hit))) {
                    retval = ActionResult.SUCCESS;
                    if (!world.isClient) {
                        BlockState newState = world.getBlockState(pos);
                        updateConnectedNeighbors(newState, world, pos);
                        if (newState.get(HOP) && !state.get(HOP)) {
                            CopperWireEntity blockEntity = this.getEntity(world, pos);
                            blockEntity.clearAll();
                        }

                        updatePower(newState, world, pos, false, true);
                    }
                } else {
                    retval = ActionResult.CONSUME;
                }
            }
        }

        return retval;
    }

    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir) {
        BlockState state = world.getBlockState(pos);
        CopperWireEntity cwTileEntity = getEntity(world, pos);
        return cwTileEntity.getPowerOut(state, dir);
    }

    @Override
    public int getPowerStep(BlockView world, BlockPos pos, Direction dir) {
        BlockState state = world.getBlockState(pos);
        Direction oDir = dir.getOpposite();
        int retval = 0;

        if (state.get(propForDirection(oDir)).isConnected()) {
            CopperWireEntity cwTileEntity = getEntity(world, pos);
            retval = cwTileEntity.getPowerStep(state, oDir);
        }
        return retval;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction dir) {
        return getWeakRedstonePower(state, world, pos, dir);
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction dir) {
        int retval = 0;
        CopperWireEntity cwTileEntity = getEntity(world, pos);
        Direction oDir = dir.getOpposite();
        BlockPos pos2 = pos.offset(oDir);

        if ((world.getBlockState(pos2).isSolidBlock(world, pos2)) && (oDir == Direction.DOWN)) {
            retval = (state.get(HOP) || state.get(VERTICAL)) ? 0 : cwTileEntity.getMaxPowerOut(state);
        }
        else if (state.get(propForDirection(oDir)).isConnected()) {
            if (!Direction.Type.VERTICAL.test(oDir)) {
                retval = cwTileEntity.getRedstonePower(state, oDir);
            }
        }
        return retval;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COPPER_WIRE_SHAPE;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient()) {
            if (state.canPlaceAt(world, pos)) {
                this.update(world, pos, state, sourcePos);
            } else {
                CopperWire.dropStacks(state, world, pos);
                world.removeBlockEntity(pos);
                world.removeBlock(pos, false);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        boolean retval = blockState.isSideSolidFullSquare(world, blockPos, Direction.UP);

        if (!retval && blockState.isOf(this)) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                EnumProperty<WireConnection> prop = propForDirection(dir);
                retval |= (blockState.get(prop) == WireConnection.UP) && isWallInDirection((World) world, dir, pos);
            }
        }
        return retval;
    }

    /* ---- Private Methods ---- */
    private Direction getForcedDirection(BlockPos pos, BlockPos otherPos) {
        Direction retval = Direction.fromVector(otherPos.subtract(pos));
        if (retval == null) {
            retval = Direction.fromVector(new BlockPos(otherPos.getX(), 0, otherPos.getZ())
                    .subtract(new BlockPos(pos.getX(), 0, pos.getZ())));
        }
        return retval;
    }

    private void update(World world, BlockPos pos, BlockState state, BlockPos changePos) {
        boolean changed = false;
        boolean isConnected = false;
        Direction dir = getForcedDirection(pos, changePos);

        if (Direction.Type.VERTICAL.test(dir)) {
            for (Direction vdir : Direction.Type.HORIZONTAL) {
                EnumProperty<WireConnection> prop = propForDirection(vdir);
                isConnected |= state.get(prop) == WireConnection.UP;
            }
        }
        else {
            EnumProperty<WireConnection> prop = propForDirection(dir);
            isConnected = (prop != null) && state.get(prop).isConnected();

            if ((prop != null) && !isWallInDirection(world, dir, pos) &&
                    (state.get(prop) == WireConnection.UP)) {
                state = state.with(prop, state.get(VERTICAL) ? WireConnection.NONE : WireConnection.SIDE);
                changed = true;
            }
        }
        BlockState newState = updateConnections(state, world, pos, changePos);
        changed |= (state != newState);
        isConnected |= (newState.get(NORTH) != WireConnection.NONE) || (newState.get(EAST) != WireConnection.NONE)
                || (newState.get(SOUTH) != WireConnection.NONE) || (newState.get(WEST) != WireConnection.NONE);
        updatePower(newState, world, pos, changed, isConnected);
    }

    private BlockState updateConnections(BlockState state, World world, BlockPos pos, BlockPos changePos) {
        BlockState upState = world.getBlockState(pos.up());
        BlockState changeState = world.getBlockState(changePos);
        Direction changeDir = getForcedDirection(pos, changePos).getOpposite();
        BlockState newState = state;
        boolean isPermeable = upState.isAir() || upState.isOf(Blocks.WATER);
        boolean isFullSquare = changeState.isSideSolidFullSquare(world, changePos, changeDir);

        if ((!isPermeable && upState.isSideSolidFullSquare(world, pos.up(), Direction.DOWN)) || !isFullSquare) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                EnumProperty<WireConnection> prop = propForDirection(dir);

                if ((state.get(prop) == WireConnection.UP) &&
                        !isWallInDirection(world, dir, pos)) {
                    newState = newState.with(prop, state.get(VERTICAL) ? WireConnection.NONE : WireConnection.SIDE);
                }
            }

            if (newState != state) {
                world.setBlockState(pos, newState);
            }
        } else if (isPermeable) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                EnumProperty<WireConnection> prop = propForDirection(dir);
                BlockState downState = world.getBlockState(pos.down());

                if ((state.get(prop) == WireConnection.NONE) && isWallInDirection(world, dir, pos) &&
                        downState.isOf(this) && downState.get(prop) == WireConnection.UP) {
                    newState = newState.with(prop, WireConnection.UP);
                }
            }
        }

        return newState;
    }

    private void updatePower(BlockState state, World world, BlockPos pos,
                             boolean changed, boolean isConnected) {
        CopperWireEntity cwTileEntity = getEntity(world, pos);
        BlockState newState = state;
        LOGGER.debug("*** UpdatePower: Initial @ " + pos.toShortString() +
                ", Current: " + cwTileEntity.toShortString());

        cwTileEntity.setChanging(true);

        if (isConnected) {
            readPower(state, world, pos);
            cwTileEntity.normalize(state);
        }

        int signal = cwTileEntity.getMaxPowerOut(newState);
        newState = newState.with(POWER, signal >> 4).with(STEP, signal & 15);

        if (changed || (newState != state)) {
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            changed = true;
        }

        LOGGER.debug("*** UpdatePower: Final @ " + pos.toShortString() +
                ", Current: " + cwTileEntity.toShortString());
        if (cwTileEntity.isModified()) {
            cwTileEntity.setChanged();
            changed = true;
        }

        if (changed) {
            updateConnectedNeighbors(newState, world, pos);
            world.updateNeighbors(pos, this);
        }

        cwTileEntity.setChanging(false);
    }

    private void updateConnectedNeighbors(BlockState state, World world, BlockPos pos) {
        if (state.get(VERTICAL)) {
            if (state.get(NORTH).isConnected() ||
                    state.get(EAST).isConnected() ||
                    state.get(SOUTH).isConnected() ||
                    state.get(WEST).isConnected()) {
                world.updateNeighbor(pos.down(), this, pos);
            }
        } else {
            world.updateNeighborsExcept(pos.down(), this, Direction.UP);
        }

        for (Direction dir : Direction.Type.HORIZONTAL) {
            EnumProperty<WireConnection> prop = propForDirection(dir);
            WireConnection side = state.get(prop);
            if (side.isConnected()) {
                BlockPos pPos = getRelevantPosition(world, pos, dir);
                if (world.getBlockState(pPos).isSolidBlock(world, pPos)) {
                    world.updateNeighborsExcept(pPos, this, dir.getOpposite());
                }
                else {
                    world.updateNeighbor(pPos, this, pos);
                }

                if (state.get(VERTICAL)) {
                    BlockPos backPos = pos.offset(dir);
                    world.updateNeighbor(pPos, this, pos);

                    if (world.getBlockState(backPos).isSolidBlock(world, backPos)) {
                        world.updateNeighborsExcept(backPos, this, dir.getOpposite());
                    }
                }
            }
        }
    }

    private void readPower(BlockState state, World world, BlockPos pos) {
        CopperWireEntity cwTileEntity = getEntity(world, pos);

        for (Direction dir: Direction.Type.HORIZONTAL) {
            int cpower = 0;

            if (state.get(propForDirection(dir)).isConnected()) {
                BlockPos tgtPos = getRelevantPosition(world, pos, dir);
                BlockState tgtState = world.getBlockState(tgtPos);
                Direction tgtDir = getRelevantDirection(world, pos, dir, tgtPos, RelevantDirMode.POWER);
                EnumProperty<WireConnection> tdProp = propForDirection(tgtDir);

                 if (state.get(VERTICAL)) {
                    BlockPos downPos = pos.down();
                    BlockState downState = world.getBlockState(downPos);
                    Block downBlock = downState.getBlock();
                    Direction dTgtDir = getRelevantDirection(world, pos, dir, downPos, RelevantDirMode.POWER);
                    int power = downState.getWeakRedstonePower(world, downPos, dTgtDir.getOpposite());
                    int step = ((CopperReadyDevice) downBlock).getPowerStep(world, downPos, dTgtDir.getOpposite());
                    cpower = Math.max(cpower, (power << 4 | step) - 1);
                }

                if (tgtState.emitsRedstonePower() &&
                        (!tgtState.contains(tdProp) || tgtState.get(tdProp).isConnected())) {
                    Block tgtBlock = tgtState.getBlock();
                    int power = tgtState.getWeakRedstonePower(world, tgtPos, tgtDir.getOpposite());
                    int step = tgtBlock instanceof CopperReadyDevice
                            ? ((CopperReadyDevice) tgtBlock).getPowerStep(world, tgtPos, tgtDir.getOpposite())
                            : tgtState.isOf(Blocks.REDSTONE_WIRE)
                                ? 0 : (power > 0)
                                    ? 15 : 0;
                    cpower = Math.max(cpower, (power << 4 | step) -
                            ((tgtState.isOf(Blocks.REDSTONE_WIRE) || tgtState.isOf(ModBlocks.COPPER_WIRE))
                                    ? 1 : 0));
                }
            }
            cwTileEntity.setPower(cpower >> 4, cpower & 15, dir);
        }
    }

    private HitSpot getHitSpot(BlockHitResult hit) {
        HitSpot retval = HitSpot.None;
        if (hit.getSide() == Direction.UP) {
            BlockPos pos = hit.getBlockPos();
            Vec3d loc = hit.getPos();
            int x = (int) ((loc.getX() - pos.getX()) * 16);
            int z = (int) ((loc.getZ() - pos.getZ()) * 16);

            if ((x >= 6) && (x < 10) && (z >= 6) && (z < 10)) {
                retval = HitSpot.Center;
            } else if (z < 8) {
                if (x < z) {
                    retval = HitSpot.West;
                } else if (x > 15 - z) {
                    retval = HitSpot.East;
                } else {
                    retval = HitSpot.North;
                }
            } else {
                if (x < 15 - z) {
                    retval = HitSpot.West;
                } else if (x > z) {
                    retval = HitSpot.East;
                } else {
                    retval = HitSpot.South;
                }
            }
        }

        return retval;
    }

    private boolean handleWireHit(World world, BlockState oldState, BlockPos pos, HitSpot spot) {
        boolean retval;
        boolean walled;
        BlockState newState = oldState;

        switch (spot) {
            case North -> {
                walled = isWallInDirection(world, Direction.NORTH, pos);
                newState = getStateChange(oldState, NORTH, walled);
            }
            case East -> {
                walled = isWallInDirection(world, Direction.EAST, pos);
                newState = getStateChange(oldState, EAST, walled);
            }
            case South -> {
                walled = isWallInDirection(world, Direction.SOUTH, pos);
                newState = getStateChange(oldState, SOUTH, walled);
            }
            case West -> {
                walled = isWallInDirection(world, Direction.WEST, pos);
                newState = getStateChange(oldState, WEST, walled);
            }
            case Center -> {
                if (!newState.get(VERTICAL) && (newState.get(NORTH) != WireConnection.NONE) &&
                        (newState.get(EAST) != WireConnection.NONE) &&
                        (newState.get(SOUTH) != WireConnection.NONE) &&
                        (newState.get(WEST) != WireConnection.NONE)) {

                    newState = oldState.with(HOP, !oldState.get(HOP));
                }
            }
        }

        retval = newState != oldState;

        if (retval) {
            world.setBlockState(pos, newState, 3);
        }
        return retval;
    }

    private int getSidesConnected(BlockState state) {
        int retval = 0;

        if (state.get(NORTH) != WireConnection.NONE) ++retval;
        if (state.get(EAST) != WireConnection.NONE) ++retval;
        if (state.get(SOUTH) != WireConnection.NONE) ++retval;
        if (state.get(WEST) != WireConnection.NONE) ++retval;

        return retval;
    }

    private int getSidesUp(BlockState state) {
        int retval = 0;

        if (state.get(NORTH) == WireConnection.UP) ++retval;
        if (state.get(EAST) == WireConnection.UP) ++retval;
        if (state.get(SOUTH) == WireConnection.UP) ++retval;
        if (state.get(WEST) == WireConnection.UP) ++retval;

        return retval;
    }

    private BlockState getStateChange(BlockState state, EnumProperty<WireConnection> prop, boolean walled) {
        BlockState retval = state.with(prop, getNextSide(state, prop, walled));

        if (retval != state) {
            int countConnected = getSidesConnected(retval);
            int countUp = getSidesUp(retval);

            if ((retval.get(VERTICAL) && (countUp == 0)) ||
                    (retval.get(HOP) && (countConnected != 4)) ||
                    (countConnected < 2)) {
                retval = state;
            }
        }

        return retval;
    }

    private WireConnection getNextSide(BlockState state, EnumProperty<WireConnection> prop, boolean allowUp) {
        WireConnection side = state.get(prop);
        boolean hop = state.get(HOP);
        int sides = getSidesConnected(state);
        boolean allowNone = !hop && (sides > 2);

        return switch (side) {
            case NONE -> WireConnection.SIDE;
            case SIDE -> allowUp ? WireConnection.UP : WireConnection.NONE;
            case UP -> allowNone ? WireConnection.NONE : WireConnection.SIDE;
        };
    }

    private boolean isWallInDirection(World world, Direction dir, BlockPos pos) {
        BlockPos upPos = pos.up();
        BlockState state = world.getBlockState(pos.offset(dir));
        BlockState upState = world.getBlockState(upPos);

        return state.isSideSolidFullSquare(world, pos.offset(dir), dir.getOpposite()) &&
                (upState.isAir() || !upState.isSideSolidFullSquare(world, upPos, Direction.DOWN));
    }

    private CopperWireEntity getEntity(BlockView world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        CopperWireEntity retval = null;

        if (entity instanceof CopperWireEntity) {
            retval = (CopperWireEntity) entity;
        }

        return retval;
    }

    private BlockPos getRelevantPosition(World world, BlockPos pos, Direction dir) {
        EnumProperty<WireConnection> prop = propForDirection(dir);
        WireConnection side = world.getBlockState(pos).get(prop);
        BlockPos dstPos = pos.up();
        BlockState dstState = world.getBlockState(dstPos);
        BlockPos srcPos;

        if ((side == WireConnection.UP) && (dstState.isOf(this) &&
                dstState.get(VERTICAL) && (dstState.get(prop) == WireConnection.UP))) {
            srcPos = dstPos;
        }
        else {
            srcPos = pos.offset(dir);
            BlockState srcState = world.getBlockState(srcPos);

            if (side == WireConnection.UP) {
                srcPos = srcPos.up();
            }
            else if (!srcState.emitsRedstonePower() ||
                    (srcState.isOf(this) && srcState.get(VERTICAL))) {
                if (!srcState.isSolidBlock(world, srcPos) &&
                        (!srcState.isOf(this) || (srcState.get(propForDirection(dir.getOpposite())) == WireConnection.NONE))) {
                    BlockPos downPos = srcPos.down();
                    BlockState downState = world.getBlockState(downPos);
                    if ((downState.isOf(this) || downState.isOf(Blocks.REDSTONE_WIRE)) &&
                            (downState.get(propForDirection(dir.getOpposite())) == WireConnection.UP)) {
                        srcPos = downPos;
                    }
                }
            }
        }
        return srcPos;
    }

    private Direction getRelevantDirection(World world, BlockPos pos, Direction dir,
                                           @Nullable BlockPos tgtPos, RelevantDirMode mode) {
        Direction retval = dir;
        BlockState state = world.getBlockState(pos);
        BlockState tgtState = tgtPos == null ? null : world.getBlockState(tgtPos);
        boolean isVertical = state.isOf(this) && state.get(VERTICAL);
        boolean tgtIsVertical = tgtState != null && tgtState.isOf(this) && tgtState.get(VERTICAL);
        boolean tgtIsUp = tgtPos != null && (pos.getY() - tgtPos.getY() == -1);
        boolean tgtIsDown = tgtPos != null && (pos.getY() - tgtPos.getY() == 1);

        Direction oDir = dir.getOpposite();

        switch (mode) {
            case POWER ->
                retval = (tgtIsUp || tgtIsDown)
                        ? tgtIsVertical
                            ? (isVertical || tgtIsUp) ? dir : oDir
                            : tgtIsDown ? dir : oDir
                        : oDir;

            case POSITION ->
                    retval = (Direction.Type.VERTICAL.test(dir) || !state.get(propForDirection(dir)).isConnected())
                            ? null
                            : (state.get(propForDirection(dir)) == WireConnection.UP) ? Direction.UP : dir;
        }

        return retval;
    }
}