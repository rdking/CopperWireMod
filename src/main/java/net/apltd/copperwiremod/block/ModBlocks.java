package net.apltd.copperwiremod.block;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.item.ModItems;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
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
                    .luminance((BlockState blockState) -> {
                        return blockState.get(CopperWire.POWER);
                    })
                    .solidBlock((BlockState state, BlockView world, BlockPos pos) -> {
                        return false;//true;
                    })
            ), ItemGroup.REDSTONE);

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
    }
}
