package net.apltd.copperwiremod;

import net.apltd.copperwiremod.block.CopperWire;
import net.apltd.copperwiremod.block.ModBlocks;
import net.apltd.copperwiremod.blockentity.ModBlockEntities;
import net.apltd.copperwiremod.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopperWireMod implements ModInitializer {
	public static final String MODID = "copperwiremod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static CopperWire COPPERWIRE = null;

	@Override
	public void onInitialize() {
		LOGGER.info("Loading CopperWireMod");
		ModBlocks.registerModBlocks();
		ModItems.registerModItems();
		ModBlockEntities.registerBlockEntities();
	}
}
