package net.apltd.copperwiremod.util;

import net.minecraft.util.math.Direction;

public class CopperPower {
    public int power = 0;                       //Strength of the copper wire power
    public Direction sDir = Direction.DOWN;     //Direction from which the power was supplied
    public boolean isFromRedstoneWire = false;  //True if the power reading came from redstone wire.

    @Override
    public String toString() {
        return "CopperPower{power=" + power + ", sDir=" + sDir + ", fromRedstone=" + isFromRedstoneWire + "}";
    }
}
