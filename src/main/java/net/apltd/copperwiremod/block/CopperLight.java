package net.apltd.copperwiremod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


@SuppressWarnings("deprecation")
public class CopperLight extends Block {
    public static final String BLOCK_NAME = "copper_light";
    public static final IntProperty COLOR = IntProperty.of("color", 0, 15);

    public CopperLight(Settings settings) {
        super(settings
                .luminance((BlockState state) -> state.get(COLOR) > 0 ? 15 : 0)
        );
        this.setDefaultState(this.getDefaultState().with(COLOR, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
        super.appendProperties(builder);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        ctx.getWorld().createAndScheduleBlockTick(ctx.getBlockPos(), this, 2);
        return super.getPlacementState(ctx);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            if (canPlaceAt(state, world, pos)) {
                int power = world.getReceivedRedstonePower(pos);
                if (power != state.get(COLOR)) {
                    world.createAndScheduleBlockTick(pos, this, 2);
                }
            }
            else {
                CopperLight.dropStacks(state, world, pos);
                world.removeBlockEntity(pos);
                world.removeBlock(pos, false);
            }
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int power = world.getReceivedRedstonePower(pos);
        world.setBlockState(pos, state.with(COLOR, power), Block.NOTIFY_ALL);
    }
}
