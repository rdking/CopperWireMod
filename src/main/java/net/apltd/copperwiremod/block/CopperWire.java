package net.apltd.copperwiremod.block;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.blockentity.CopperWireEntity;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
public class CopperWire extends Block implements BlockEntityProvider, Waterloggable {
    public static final EnumProperty<WireConnection> NORTH = RedstoneWireBlock.WIRE_CONNECTION_NORTH;
    public static final EnumProperty<WireConnection> EAST = RedstoneWireBlock.WIRE_CONNECTION_EAST;
    public static final EnumProperty<WireConnection> SOUTH = RedstoneWireBlock.WIRE_CONNECTION_SOUTH;
    public static final EnumProperty<WireConnection> WEST = RedstoneWireBlock.WIRE_CONNECTION_WEST;
    public static final IntProperty POWER = RedstoneWireBlock.POWER;
    public static final BooleanProperty VERTICAL = BooleanProperty.of("vertical");
    public static final BooleanProperty HOP = BooleanProperty.of("hop");
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape COPPER_WIRE_SHAPE =
            Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    private enum HitSpot {
        None,
        North,
        East,
        South,
        West,
        Center
    }

    public static final String BLOCK_NAME = "copperwire";
    private static final Logger LOGGER = Logger.getAnonymousLogger();

    public CopperWire(Settings settings) {
        super(settings);

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(NORTH, WireConnection.SIDE)
                        .with(EAST, WireConnection.NONE)
                        .with(SOUTH, WireConnection.SIDE)
                        .with(WEST, WireConnection.NONE)
                        .with(POWER, 0)
                        .with(VERTICAL, false)
                        .with(HOP, false)
                        .with(WATERLOGGED, false)
        );

        CopperWireMod.COPPERWIRE = this;
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
        builder.add(VERTICAL);
        builder.add(HOP);
        builder.add(WATERLOGGED);
        super.appendProperties(builder);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = (BlockState) getDefaultState()
                .with(WATERLOGGED, world.getFluidState(pos).getFluid() == Fluids.WATER);

        Direction dir = ctx.getPlayerFacing();
        if ((dir == Direction.EAST) || (dir == Direction.WEST)) {
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
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public int getCopperSignal(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        int retval = 0;
        Direction opposite = direction.getOpposite();
        EnumProperty<WireConnection> prop = propForDirection(opposite);
        CopperWireEntity cwTileEntity = getEntity(world, pos);

        if ((prop != null) && (cwTileEntity != null)) {
            if ((state.get(prop) != WireConnection.NONE)) {
                retval = cwTileEntity.getPowerOut(direction, opposite);
            }
        }

        return retval;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CopperWireEntity(pos, state);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        //Place the wire down vertically from the player's perspective.
        if (!world.isClient) {
            updatePower(state, world, pos, true, true);
        }
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult retval = ActionResult.FAIL;

        if (!player.getAbilities().allowModifyWorld) {
            retval = ActionResult.PASS;
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            if (handleWireHit(world, state, pos, getHitSpot(hit))) {
                retval = ActionResult.SUCCESS;
                if (!world.isClient) {
                    BlockState newState = world.getBlockState(pos);
                    updateConnectedNeighbors(newState, world, pos);
                    if (newState.get(HOP) && !state.get(HOP)) {
                        CopperWireEntity blockEntity = this.getEntity(world, pos);
                        Direction powerDir = blockEntity.getPowerDir();
                        blockEntity.clearAll();
                        blockEntity.setHop(true);
                    }

                    updatePower(newState, world, pos, false, true);
                }
            } else {
                retval = ActionResult.CONSUME;
            }
        }

        return retval;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction dir) {
        int retval = 0;
        if (!isPhantomTesting(world, pos, dir)) {
            retval = state.getWeakRedstonePower(world, pos, dir);
        }
        return retval;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction dir) {
        int retval = 0;
        //if (!isPhantomTesting(world, pos, dir)) {
            Direction opposite = dir.getOpposite();
            EnumProperty<WireConnection> prop = propForDirection(opposite);
            if (prop != null) {
                WireConnection con = state.get(prop);
                BlockPos upPos = pos.offset(opposite).offset(Direction.UP);
                BlockState upState = world.getBlockState(upPos);
                if ((con == WireConnection.SIDE) || ((con == WireConnection.UP) &&
                        (upState.isOf(this) || upState.isOf(Blocks.REDSTONE_WIRE)))) {
                    retval = cpToRP(getCopperSignal(state, world, pos, dir));
                }
            }
        //}
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
        return blockState.isSideSolidFullSquare(world, blockPos, Direction.UP);
    }

    /* ---- Private Methods ---- */

    private boolean isPhantomTesting(BlockView world, BlockPos pos, Direction dir) {
        boolean retval = false;
        BlockPos srcPos = pos.offset(dir.getOpposite(), 2);
        BlockState srcState = world.getBlockState(srcPos);

        if (srcState.isOf(Blocks.REDSTONE_WIRE)) {
            RedstoneWireBlock src = (RedstoneWireBlock) srcState.getBlock();
            try {
                Field wgpf = RedstoneWireBlock.class.getDeclaredField("wiresGivePower");
                wgpf.setAccessible(true);
                retval = !wgpf.getBoolean(src);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //Do Nothing
            }
        }

        return retval;
    }

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
        if (!world.isClient()) {
            Direction dir = getForcedDirection(pos, changePos);
            EnumProperty<WireConnection> prop = propForDirection(dir);
            boolean isConnected = (prop != null) && state.get(prop).isConnected();

            if ((prop != null) && !isWallInDirection(world, dir, pos) &&
                    (state.get(prop) == WireConnection.UP)) {
                state = state.with(prop, WireConnection.SIDE);
                changed = true;
            }
            state = updateConnections(state, world, pos, changePos);
            updatePower(state, world, pos, changed, isConnected);
        }
    }

    private int cpToRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

    private BlockState updateConnections(BlockState state, World world, BlockPos pos, BlockPos changePos) {
        BlockState srcState = world.getBlockState(pos.up());
        BlockState changeState = world.getBlockState(changePos);
        Direction changeDir = getForcedDirection(pos, changePos).getOpposite();
        BlockState newState = state;

        if ((!srcState.isAir() && srcState.isSideSolidFullSquare(world, pos.up(), Direction.DOWN)) ||
                !changeState.isSideSolidFullSquare(world, changePos, changeDir)) {
            for (Iterator<Direction> it = Direction.Type.HORIZONTAL.iterator(); it.hasNext(); ) {
                Direction dir = it.next();
                EnumProperty<WireConnection> prop = propForDirection(dir);

                if ((state.get(prop) == WireConnection.UP) &&
                        !isWallInDirection(world, dir, pos)) {
                    newState = newState.with(prop, WireConnection.SIDE);
                }
            }

            if (newState != state) {
                world.setBlockState(pos, newState);
            }
        }

        return newState;
    }

    private void updatePower(BlockState state, World world, BlockPos pos, boolean changed, boolean isConnected) {
        CopperWireEntity cwTileEntity = getEntity(world, pos);
        BlockState newState = state;
        LOGGER.fine("*** UpdatePower: Initial @ " + pos.toShortString() +
                ", Current: " + cwTileEntity.toShortString() + ", Power: " + newState.get(POWER));

        cwTileEntity.setChanging(true);

        if (isConnected) {
            for (Iterator<Direction> it = Direction.Type.HORIZONTAL.iterator(); it.hasNext(); ) {
                Direction dir = it.next();
                if (state.get(propForDirection(dir)).isConnected()) {
                    newState = updatePowerInDirection(state, world, pos, dir);
                } else {
                    cwTileEntity.reset(dir);
                }
            }
        }

        if (state.get(HOP)) {
            newState = newState.with(POWER, 0);
        } else {
            int power = cpToRP(cwTileEntity.getPowerOut());
            newState = newState.with(POWER, power);
        }

        if (changed || (newState != state)) {
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            changed = true;
        }

        LOGGER.fine("*** UpdatePower: Final @ " + pos.toShortString() +
                ", Current: " + cwTileEntity.toShortString() + ", Power: " + newState.get(POWER));
        if (cwTileEntity.isModified()) {
            cwTileEntity.setChanged();
            changed = true;
        }

        if (changed) {
            updateConnectedNeighbors(newState, world, pos);
        }

        cwTileEntity.setChanging(false);
    }

    public int getCopperSignalFromPos(BlockState state, BlockView world, BlockPos pos, BlockPos otherPos) {
        int retval = 0;
        Direction direction = getForcedDirection(pos, otherPos);
        Direction opposite = direction.getOpposite();
        EnumProperty<WireConnection> prop = propForDirection(opposite);
        CopperWireEntity cwTileEntity = getEntity(world, pos);

        if ((prop != null) && (cwTileEntity != null)) {
            if ((state.get(prop) != WireConnection.NONE)) {
                retval = cwTileEntity.getPowerOut(direction);
            }
        }

        return retval;
    }

    private boolean isValueAdjacent(World world, BlockState state, BlockPos pos, BlockPos tgtPos) {
        boolean retval = false;
        BlockState tgtState = world.getBlockState(tgtPos);
        if (tgtState.contains(POWER)) {
            if (tgtState.isOf(state.getBlock())) {
                retval = getCopperSignalFromPos(state, world, pos, tgtPos) ==
                    getCopperSignalFromPos(tgtState, world, tgtPos, pos) - 1;
            }
            else {
                retval = state == tgtState;
            }
        }
        return retval;
    }

    private void updateConnectedNeighbors(BlockState state, World world, BlockPos pos) {
        for (Iterator<Direction> it = Direction.Type.HORIZONTAL.iterator(); it.hasNext(); ) {
            Direction dir = it.next();
            WireConnection side = state.get(propForDirection(dir));
            if (side.isConnected()) {
                BlockPos srcPos = pos.offset(dir);
                BlockState srcState = world.getBlockState(srcPos);

                if (side == WireConnection.UP) {
                    srcPos = srcPos.up();
                } else if (!srcState.emitsRedstonePower()) {
                    BlockPos downPos = srcPos.down();
                    BlockState downState = world.getBlockState(downPos);
                    if (downState.emitsRedstonePower()) {
                        srcPos = downPos;
                    }
                }

                if (!isValueAdjacent(world, state, pos, srcPos)) {
                    world.updateNeighbor(srcPos, this, pos);
                }
            }
        }
    }

    private BlockState updatePowerInDirection(BlockState state, World world, BlockPos pos, Direction dir) {
        CopperWireEntity cwTileEntity = getEntity(world, pos);

        if (cwTileEntity != null) {
            int oldVal = cwTileEntity.getPowerOut(dir);
            int newVal = Math.max(0, readPower(state, world, pos, dir) - 1);
            BlockPos srcPos = pos.offset(dir);
            WireConnection side = state.get(propForDirection(dir));
            BlockState srcState = world.getBlockState(srcPos);

            //Check to see if the connection is on the plane above or below.
            if (side == WireConnection.UP) {
                srcPos = srcPos.up();
                srcState = world.getBlockState(srcPos);
            } else if (!srcState.emitsRedstonePower()) {
                srcPos = srcPos.down();
                srcState = world.getBlockState(srcPos);
            }

            if ((oldVal != newVal)) {
                cwTileEntity.setPower(dir, newVal, srcState.isOf(Blocks.REDSTONE_WIRE));
                state = state.with(POWER, state.get(HOP) ? 0 : cpToRP(cwTileEntity.getPowerOut(dir)));
            }
        }

        return state;
    }

    private int readPower(BlockState state, World world, BlockPos pos, Direction dir) {
        int retval = 0;
        Direction oDir = dir.getOpposite();
        EnumProperty<WireConnection> prop = propForDirection(dir);
        EnumProperty<WireConnection> opposite = propForDirection(oDir);
        BlockPos srcPos = pos.offset(dir);
        WireConnection side = state.get(prop);
        BlockState srcState = world.getBlockState(srcPos);
        WireConnection checkSide = WireConnection.SIDE;

        if (side == WireConnection.UP) {
            srcPos = srcPos.up();
            srcState = world.getBlockState(srcPos);
        } else if (!srcState.emitsRedstonePower()) {
            srcPos = srcPos.down();
            srcState = world.getBlockState(srcPos);
            checkSide = WireConnection.UP;
        }

        if (srcState.contains(opposite) &&
                (srcState.get(opposite) == checkSide) &&
                srcState.isOf(this)) {
            BlockPos abovePos = pos.up();
            BlockState aboveState = world.getBlockState(abovePos);

            if (!aboveState.isSideSolidFullSquare(world, abovePos, Direction.DOWN) &&
                    !aboveState.isSideSolidFullSquare(world, abovePos, dir)) {
                retval = ((CopperWire) srcState.getBlock()).getCopperSignal(srcState, world, srcPos, dir);
            }
        }
        else {
            retval = srcState.getWeakRedstonePower(world, srcPos, dir) * 16;
        }

        return retval;
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
            case North:
                walled = isWallInDirection(world, Direction.NORTH, pos);
                newState = getStateChange(oldState, NORTH, walled);
                break;
            case East:
                walled = isWallInDirection(world, Direction.EAST, pos);
                newState = getStateChange(oldState, EAST, walled);
                break;
            case South:
                walled = isWallInDirection(world, Direction.SOUTH, pos);
                newState = getStateChange(oldState, SOUTH, walled);
                break;
            case West:
                walled = isWallInDirection(world, Direction.WEST, pos);
                newState = getStateChange(oldState, WEST, walled);
                break;
            case Center:
                if (!newState.get(VERTICAL) && (newState.get(NORTH) != WireConnection.NONE) &&
                        (newState.get(EAST) != WireConnection.NONE) &&
                        (newState.get(SOUTH) != WireConnection.NONE) &&
                        (newState.get(WEST) != WireConnection.NONE)) {

                    CopperWireEntity tileEntity = (CopperWireEntity) world.getBlockEntity(pos);
                    newState = oldState.with(HOP, !oldState.get(HOP));
                    tileEntity.setHop(newState.get(HOP));
                }
                break;
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
        WireConnection retval = WireConnection.NONE;
        WireConnection side = state.get(prop);
        boolean hop = state.get(HOP);
        int sides = getSidesConnected(state);
        boolean allowNone = !hop && (sides > 2);

        switch (side) {
            case NONE:
                retval = WireConnection.SIDE;
                break;
            case SIDE:
                retval = allowUp ? WireConnection.UP : WireConnection.NONE;
                break;
            case UP:
                retval = allowNone ? WireConnection.NONE : WireConnection.SIDE;
        }
        return retval;
    }

    private EnumProperty<WireConnection> propForDirection(Direction dir) {
        EnumProperty<WireConnection> retval = null;
        switch (dir) {
            case NORTH:
                retval = NORTH;
                break;
            case EAST:
                retval = EAST;
                break;
            case SOUTH:
                retval = SOUTH;
                break;
            case WEST:
                retval = WEST;
                break;
        }
        return retval;
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
//
//    private boolean isConnectedForDirection(BlockState state, World world, BlockPos pos, Direction dir) {
//        boolean retval = false;
//        Direction oDir = dir.getOpposite();
//        EnumProperty<WireConnection> prop = propForDirection(dir);
//        EnumProperty<WireConnection> oProp = propForDirection(oDir);
//        BlockPos srcPos = pos.offset(dir);
//
//        if (state.contains(prop)) {
//            WireConnection cSide = state.get(prop);
//            BlockState srcState = world.getBlockState(srcPos);
//
//            if (cSide == WireConnection.UP) {
//                srcPos = srcPos.up();
//                srcState = world.getBlockState(srcPos);
//            } else if (!srcState.emitsRedstonePower()) {
//                srcPos = srcPos.down();
//                srcState = world.getBlockState(srcPos);
//            }
//
//            retval = srcState.contains(oProp) && srcState.get(oProp).isConnected();
//        }
//
//        return retval;
//    }
}