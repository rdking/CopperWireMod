package net.apltd.copperwiremod.blockentity;

import net.apltd.copperwiremod.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {

    public static BlockEntityType<CopperWireEntity> COPPERWIRE_ENTITY;

    public static void registerBlockEntities() {
        COPPERWIRE_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                CopperWireEntity.COPPERWIRE_ENTITYNAME,
                FabricBlockEntityTypeBuilder.create(CopperWireEntity::new, ModBlocks.COPPER_WIRE)
                        .build(null));
    }
}
