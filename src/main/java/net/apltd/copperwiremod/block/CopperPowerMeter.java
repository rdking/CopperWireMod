package net.apltd.copperwiremod.block;

import static net.apltd.copperwiremod.util.CopperTools.*;

import net.apltd.copperwiremod.util.CopperTools;
import net.minecraft.block.*;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class CopperPowerMeter extends Block {
    public static final String BLOCK_NAME = "copper_pmeter";
    public static final BooleanProperty MODE = BooleanProperty.of("mode");
    public static final IntProperty CPOWER = CopperTools.CPOWER;

    public CopperPowerMeter(AbstractBlock.Settings settings) {
        super(settings
                .luminance((BlockState blockState) -> {
                    int retval = 0;
                    if (blockState.isOf(ModBlocks.COPPER_POWERMETER)) {
                        retval = blockState.get(CopperPowerMeter.MODE)
                                ? CPtoRP(blockState.get(CopperPowerMeter.CPOWER))
                                : blockState.get(CopperPowerMeter.CPOWER);

                    }
                    return retval;
                })
        );

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(MODE, false)
                        .with(CPOWER, 0)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MODE);
        builder.add(CPOWER);
        super.appendProperties(builder);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("block.copperwiremod.copper_pmeter.tooltip"));
        super.appendTooltip(stack, world, tooltip, options);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            neighborUpdate(state, world, pos, state.getBlock(), pos, false);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult retval = ActionResult.FAIL;
        if (!world.isClient) {
            if (!player.getAbilities().allowModifyWorld) {
                retval = ActionResult.PASS;
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                boolean mode = !state.get(MODE);
                int power = mode ? state.get(CPOWER) * 16 : CPtoRP(state.get(CPOWER));
                retval = ActionResult.SUCCESS;
                state = state.with(MODE, mode).with(CPOWER, power);
                world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                world.updateNeighborsAlways(pos, this);
            }
        }
        return retval;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            int max = 0;
            for (Direction dir: Direction.values()) {
                BlockPos dPos = pos.offset(dir);
                BlockState dState = world.getBlockState(dPos);
                Direction dDir = Direction.Type.VERTICAL.test(dir) ? Direction.NORTH : dir.getOpposite();
                if (state.get(MODE)) {
                    if ((dState.getBlock() instanceof CopperReadyDevice) &&
                            ((!dState.isOf(ModBlocks.COPPER_WIRE) ||
                                    (Direction.Type.VERTICAL.test(dir) ||
                                            (!dState.get(CopperWire.VERTICAL) &&
                                                    dState.get(propForDirection(dir)).isConnected()))))) {
                        max = Math.max(max, ((CopperReadyDevice)dState.getBlock())
                                .getCopperSignal(world, dPos, dDir, null));
                    }
                    else {
                        if (Direction.Type.VERTICAL.test(dir) && dState.isOf(Blocks.REDSTONE_WIRE)) {
                            if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH).isConnected()) {
                                dir = Direction.NORTH;
                            }
                            else if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST).isConnected()) {
                                dir = Direction.EAST;
                            }
                            else if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH).isConnected()) {
                                dir = Direction.SOUTH;
                            }
                            else if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST).isConnected()) {
                                dir = Direction.WEST;
                            }
                        }
                        max = Math.max(max, dState.getWeakRedstonePower(world, dPos, dir) * 16);
                    }
                }
                else {
                    if ((dState.getBlock() instanceof CopperReadyDevice) &&
                            ((!dState.isOf(ModBlocks.COPPER_WIRE) ||
                                    (Direction.Type.VERTICAL.test(dir) ||
                                            (!dState.get(CopperWire.VERTICAL) &&
                                                    dState.get(propForDirection(dir)).isConnected()))))) {
                        max = Math.max(max, CPtoRP(((CopperReadyDevice)dState.getBlock())
                                .getCopperSignal(world, dPos, dDir, null)));
                    }
                    else {
                        if (Direction.Type.VERTICAL.test(dir) && dState.isOf(Blocks.REDSTONE_WIRE)) {
                            if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH).isConnected()) {
                                dir = Direction.NORTH;
                            }
                            else if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST).isConnected()) {
                                dir = Direction.EAST;
                            }
                            else if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH).isConnected()) {
                                dir = Direction.SOUTH;
                            }
                            else if (dState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST).isConnected()) {
                                dir = Direction.WEST;
                            }
                        }
                        max = Math.max(max, dState.getWeakRedstonePower(world, dPos, dir));
                    }
                }
            }
            if (max != state.get(CPOWER)) {
                state = state.with(CPOWER, max);
                world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                world.updateNeighborsAlways(pos, this);
            }
        }
    }
}
