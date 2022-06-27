package net.apltd.copperwiremod.item;

import net.apltd.copperwiremod.CopperWireMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final String COPPERWIRE_NAME = "copperwire";
//    public static final Item COPPER_WIRE = registerItem(COPPERWIRE_NAME,
//            new Item(new FabricItemSettings().group(ItemGroup.REDSTONE)));

//    private static Item registerItem(String name, Item item) {
//        return Registry.register(Registry.ITEM, new Identifier(CopperWireMod.MODID, name), item);
//    }
    public static void registerModItems() {
        CopperWireMod.LOGGER.info("Registering the item: " + COPPERWIRE_NAME);
    }
}
