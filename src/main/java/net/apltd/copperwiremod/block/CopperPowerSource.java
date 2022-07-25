package net.apltd.copperwiremod.block;

import net.apltd.copperwiremod.CopperWireMod;
import net.minecraft.block.*;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.item.TooltipContext;
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
    public static final IntProperty CPOWER = IntProperty.of("cpower", 0, 240);
    public static final BooleanProperty POWERED = LeverBlock.POWERED;

    public CopperPowerSource(AbstractBlock.Settings settings) {
        super(settings);

        setDefaultState(
                getStateManager().getDefaultState()
                        .with(CPOWER, 15)
                        .with(POWERED, false)
        );

        CopperWireMod.COPPERPOWERSOURCE = this;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CPOWER);
        builder.add(POWERED);
        super.appendProperties(builder);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("block.copperwiremod.copper_psrc.tooltip"));
        super.appendTooltip(stack, world, tooltip, options);
    }

    @Override
    public int getCopperSignal(BlockView world, BlockPos pos, Direction dir, Direction iDir) {
        BlockState state = world.getBlockState(pos);
        int power = state.get(CPOWER);
        return state.get(POWERED) ? power : power * 16;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) { return true; }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        int power = state.get(CPOWER);
        return state.get(POWERED) ? CPtoRP(power) : power;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult retval = ActionResult.FAIL;
        if (!world.isClient) {
            if (!player.getAbilities().allowModifyWorld) {
                retval = ActionResult.PASS;
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                retval = ActionResult.SUCCESS;
                int power = state.get(CPOWER) - 1;
                if (power < 0) {
                    power = state.get(POWERED) ? 240 : 15;
                }
                state = state.with(CPOWER, power);
                world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                world.updateNeighborsAlways(pos, this);
            }
        }
        return retval;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean powered = false;
            for (Direction dir: Direction.values()) {
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
                int power = state.get(CPOWER);
                state = state.with(CPOWER, powered ? power * 16 : CPtoRP(power)).with(POWERED, powered);
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
                world.updateNeighborsAlways(pos, this);
            }
        }
    }

    private int CPtoRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

}
