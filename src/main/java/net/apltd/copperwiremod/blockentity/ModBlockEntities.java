package net.apltd.copperwiremod.blockentity;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.block.CopperWire;
import net.apltd.copperwiremod.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ModBlockEntities {

    public static BlockEntityType<CopperWireEntity> COPPERWIRE_ENTITY;

    public static void registerBlockEntities() {
        COPPERWIRE_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                CopperWireEntity.COPPERWIRE_ENTITYNAME,
                FabricBlockEntityTypeBuilder.create(CopperWireEntity::new, CopperWireMod.COPPERWIRE)
                        .build(null));
    }
}
