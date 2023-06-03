package net.apltd.copperwiremod.mixin;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRedstoneGateBlock.class)
public class AbstractRedstoneGateBlockMixin extends HorizontalFacingBlock {
    protected AbstractRedstoneGateBlockMixin(Settings settings) {
        super(settings);
        CopperWireMod.LOGGER.info("Registering AbstractRedstoneGateBlockMixin...");
    }

    @Inject(
            at = @At("HEAD"),
            method = "getPower(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)I",
            cancellable = true
    )
    private void getPower(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        Direction dir = world.getBlockState(pos).get(FACING);
        Direction oDir = dir.getOpposite();
        BlockPos tPos = pos.offset(dir);
        BlockState tState = world.getBlockState(tPos);

        if (tState.isAir()) {
            EnumProperty<WireConnection> prop = RedstoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(oDir);
            tPos = tPos.down();
            tState = world.getBlockState(tPos);

            if (tState.isOf(ModBlocks.COPPER_WIRE) && (tState.get(prop) == WireConnection.UP)) {
                int i = tState.getWeakRedstonePower(world, tPos, oDir);
                cir.setReturnValue(i);
            }
        }
    }
}
