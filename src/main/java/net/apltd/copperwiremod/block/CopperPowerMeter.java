package net.apltd.copperwiremod.block;

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
    public static final IntProperty POWER = RedstoneWireBlock.POWER;
    public static final IntProperty STEP = CopperWire.STEP;


    public CopperPowerMeter(AbstractBlock.Settings settings) {
        super(settings
                .luminance((BlockState blockState) -> {
                    int retval = 0;
                    if (blockState.isOf(ModBlocks.COPPER_POWERMETER)) {
                        retval = blockState.get(POWER);
                    }
                    return retval;
                })
        );

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(MODE, false)
                        .with(POWER, 0)
                        .with(STEP, 0)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MODE);
        builder.add(POWER);
        builder.add(STEP);
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
                retval = ActionResult.SUCCESS;
                state = state.with(MODE, mode);
                world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                world.updateNeighborsAlways(pos, this);
            }
        }
        return retval;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            if (canPlaceAt(state, world, pos)) {
                int max = 0;

                for (Direction dir : Direction.values()) {
                    BlockPos dPos = pos.offset(dir);
                    BlockState dState = world.getBlockState(dPos);

                    int power = dState.isOf(Blocks.REDSTONE_WIRE)
                            ? dState.get(POWER) << 4
                            : dState.isOf(ModBlocks.COPPER_WIRE)
                            ? dState.get(POWER) << 4 | dState.get(STEP)
                            : 0;

                    max = Math.max(max, power);
                }

                if (max != (state.get(POWER) << 4 | state.get(STEP))) {
                    state = state.with(POWER, max >> 4).with(STEP, max & 15);
                    world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                    world.updateNeighborsAlways(pos, this);
                }
            }
            else {
                CopperPowerMeter.dropStacks(state, world, pos);
                world.removeBlockEntity(pos);
                world.removeBlock(pos, false);
            }
        }
    }
}