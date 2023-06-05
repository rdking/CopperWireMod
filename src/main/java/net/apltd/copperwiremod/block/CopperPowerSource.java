package net.apltd.copperwiremod.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.WallMountLocation;
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
public class CopperPowerSource extends Block implements CopperReadyDevice{
    public static final String BLOCK_NAME = "copper_psrc";
    public static final IntProperty POWER = RedstoneWireBlock.POWER;
    public static final IntProperty STEP = IntProperty.of("step", 0, 15);
    public static final BooleanProperty POWERED = LeverBlock.POWERED;

    public CopperPowerSource(AbstractBlock.Settings settings) {
        super(settings
                .luminance((BlockState blockState) -> {
                    int retval = 0;
                    if (blockState.isOf(ModBlocks.COPPER_POWERSOURCE)) {
                        CopperPowerSource block = (CopperPowerSource) blockState.getBlock();
                        retval = block.getWeakRedstonePower(blockState, null, null, null);
                    }
                    return retval;
                })
        );

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(POWER, 15)
                        .with(STEP, 15)
                        .with(POWERED, false)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER);
        builder.add(STEP);
        builder.add(POWERED);
        super.appendProperties(builder);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            neighborUpdate(state, world, pos, state.getBlock(), pos, false);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("block.copperwiremod.copper_psrc.tooltip"));
        super.appendTooltip(stack, world, tooltip, options);
    }

    @Override
    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir) {
        BlockState state = world.getBlockState(pos);
        return state.get(POWER) << 4 | state.get(STEP);
    }

    @Override
    public int getPowerStep(BlockView world, BlockPos pos, Direction dir) {
        BlockState state = world.getBlockState(pos);
        return state.get(STEP);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) { return true; }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWER);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult retval = ActionResult.FAIL;
        if (!world.isClient && (hand == Hand.MAIN_HAND)) {
            if (!player.getAbilities().allowModifyWorld) {
                retval = ActionResult.PASS;
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                retval = ActionResult.SUCCESS;
                boolean powered = state.get(POWERED);
                int power = state.get(POWER) - (powered ? 0 : 1);
                int step = state.get(STEP) - (powered ? 1: 0);

                if (step < 0) {
                    --power;
                    step = 15;
                }

                if (power < 1) {
                    if (power < 0) {
                        power = 15;
                        step = 15;
                    }
                    else {
                        step = 0;
                    }
                }

                if (!powered && (power > 0)) {
                    step = 15;
                }

                state = state.with(POWER, power).with(STEP, step);
                world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                world. updateNeighborsAlways(pos, this);
            }
        }
        return retval;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            if (canPlaceAt(state, world, pos)) {
                boolean powered = false;
                for (Direction dir : Direction.values()) {
                    BlockState dState = world.getBlockState(pos.offset(dir));
                    if (dState.isOf(Blocks.REDSTONE_BLOCK) ||
                            (dState.isOf(Blocks.LEVER) && (
                                    ((dState.get(LeverBlock.FACE) == WallMountLocation.WALL) && (dState.get(LeverBlock.FACING) == dir)) ||
                                            ((dir == Direction.UP) && (dState.get(LeverBlock.FACE) == WallMountLocation.FLOOR)) ||
                                            ((dir == Direction.DOWN) && (dState.get(LeverBlock.FACE) == WallMountLocation.CEILING))
                            ))
                    ) {
                        powered |= dState.isOf(Blocks.REDSTONE_BLOCK) || dState.get(POWERED);
                    }
                }

                if (state.get(POWERED) != powered) {
                    state = state.with(POWERED, powered);
                    world.setBlockState(pos, state, Block.NOTIFY_ALL);
                    world.updateNeighborsAlways(pos, this);
                }
            }
            else {
                CopperPowerSource.dropStacks(state, world, pos);
                world.removeBlockEntity(pos);
                world.removeBlock(pos, false);
            }
        }
    }
}
