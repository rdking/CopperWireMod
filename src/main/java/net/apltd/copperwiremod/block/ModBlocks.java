package net.apltd.copperwiremod.block;

import net.apltd.copperwiremod.CopperWireMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;

public class ModBlocks {
    public static final Block COPPER_WIRE = registerBlock(CopperWire.BLOCK_NAME,
            new CopperWire(FabricBlockSettings.of(Material.BAMBOO)
                    .collidable(false)
                    .strength(0, 1)
                    .nonOpaque()
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> false)
            ), ItemGroup.REDSTONE);

    public static final Block COPPER_POWERSOURCE = registerBlock(CopperPowerSource.BLOCK_NAME,
            new CopperPowerSource(FabricBlockSettings.copy(Blocks.REDSTONE_BLOCK)
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> false)
            ), ItemGroup.REDSTONE);
    public static final Block COPPER_POWERMETER = registerBlock(CopperPowerMeter.BLOCK_NAME,
            new CopperPowerMeter(FabricBlockSettings.copy(Blocks.RED_STAINED_GLASS)
                    .nonOpaque()
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> true)
            ), ItemGroup.REDSTONE);
    public static final Block COPPER_RESISTOR = registerBlock(CopperResistor.BLOCK_NAME,
            new CopperResistor(FabricBlockSettings.copy(Blocks.REPEATER)
                    .nonOpaque()
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> false)
            ), ItemGroup.REDSTONE);

    private static void registerBlockItem(String name, Block block, ItemGroup group) {
        Registry.register(Registry.ITEM, new Identifier(CopperWireMod.MODID, name),
                new BlockItem(block, new FabricItemSettings().group(group)));
    }

    @SuppressWarnings("SameParameterValue")
    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new Identifier(CopperWireMod.MODID, name), block);
    }

    public static void registerModBlocks() {
        CopperWireMod.LOGGER.info("Registering the block: " + CopperWire.BLOCK_NAME);
        CopperWireMod.LOGGER.info("Registering the block: " + CopperPowerSource.BLOCK_NAME);
    }
}
