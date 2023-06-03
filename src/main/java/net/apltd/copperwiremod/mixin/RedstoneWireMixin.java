package net.apltd.copperwiremod.mixin;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireMixin extends Block {
	public RedstoneWireMixin(Settings settings) {
		super(settings);
		CopperWireMod.LOGGER.info("Registering RedstoneWireMixin...");
	}

	//Make sure redstone dust visibly tries to connect to copper wire.
	@Inject(
			at = @At("HEAD"),
			method = "connectsTo(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z",
			cancellable = true
	)
	private static void connectsTo(BlockState state, @Nullable Direction dir, CallbackInfoReturnable<Boolean> cir) {
		if (state.isOf(ModBlocks.COPPER_WIRE)) {
			cir.setReturnValue(true);
		}
	}
}
