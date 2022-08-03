package net.apltd.copperwiremod.util;

import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.Direction;

public final class CopperTools {
    public static int CPtoRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

    public static EnumProperty<WireConnection> propForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> RedstoneWireBlock.WIRE_CONNECTION_NORTH;
            case EAST -> RedstoneWireBlock.WIRE_CONNECTION_EAST;
            case SOUTH -> RedstoneWireBlock.WIRE_CONNECTION_SOUTH;
            case WEST -> RedstoneWireBlock.WIRE_CONNECTION_WEST;
            default -> null;
        };
    }
}
