package net.apltd.copperwiremod.block;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.blockentity.CopperWireEntity;
import net.apltd.copperwiremod.item.ModItems;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;

import java.util.function.ToIntFunction;

public class ModBlocks {
    public static final Block COPPER_WIRE = registerBlock(CopperWire.BLOCK_NAME,
            new CopperWire(FabricBlockSettings.of(Material.BAMBOO)
                    .collidable(false)
                    .strength(0, 1)
                    .nonOpaque()
                    .luminance((BlockState blockState) -> blockState.get(CopperWire.POWER))
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> false)
            ), ItemGroup.REDSTONE);

    public static final Block COPPER_POWER_SOURCE = registerBlock(CopperPowerSource.BLOCK_NAME,
            new CopperPowerSource(FabricBlockSettings.copy(Blocks.REDSTONE_BLOCK)
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> false)
                    .luminance((BlockState blockState) -> {
                        int retval = 0;
                        if (blockState.isOf(CopperWireMod.COPPERPOWERSOURCE)) {
                            CopperPowerSource block = (CopperPowerSource) blockState.getBlock();
                            retval = block.getWeakRedstonePower(blockState, null, null, null);
                        }
                        return retval;
                    })
            ), ItemGroup.REDSTONE);
    public static final Block COPPER_POWER_METER = registerBlock(CopperPowerMeter.BLOCK_NAME,
            new CopperPowerMeter(FabricBlockSettings.copy(Blocks.RED_STAINED_GLASS)
                    .nonOpaque()
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> true)
                    .luminance((BlockState blockState) -> {
                        int retval = 0;
                        if (blockState.isOf(CopperWireMod.COPPERPOWERMETER)) {
                            retval = blockState.get(CopperPowerMeter.MODE)
                                    ? CPtoRP(blockState.get(CopperPowerMeter.CPOWER))
                                    : blockState.get(CopperPowerMeter.CPOWER);

                        }
                        return retval;
                    })
            ), ItemGroup.REDSTONE);

    private static int CPtoRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        return Registry.register(Registry.ITEM, new Identifier(CopperWireMod.MODID, name),
                new BlockItem(block, new FabricItemSettings().group(group)));
    }
    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new Identifier(CopperWireMod.MODID, name), block);
    }

    public static void registerModBlocks() {
        CopperWireMod.LOGGER.info("Registering the block: " + CopperWire.BLOCK_NAME);
        CopperWireMod.LOGGER.info("Registering the block: " + CopperPowerSource.BLOCK_NAME);
    }
}
