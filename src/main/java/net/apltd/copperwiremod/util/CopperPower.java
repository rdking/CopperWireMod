package net.apltd.copperwiremod.util;

import net.minecraft.util.math.Direction;

public class CopperPower {
    public int power = 0;                       //Strength of the copper wire power
    public Direction sDir = Direction.DOWN;     //Direction from which the power was supplied
    public boolean isFromRedstone = false;      //True if the power reading came from redstone.
    public boolean isFromCopperWire = false;    //True if the power reading came from copper wire.

    public CopperPower() {}

    public CopperPower(int power, Direction sDir, boolean isFromRedstone, boolean isFromCopperWire) {
        this.power = power;
        this.sDir = sDir;
        this.isFromRedstone = isFromRedstone;
        this.isFromCopperWire = isFromCopperWire;
    }

    @Override
    public String toString() {
        return "CopperPower{power=" + power + ", sDir=" + sDir +
                ", fromRedstone=" + isFromRedstone + ", fromCopperWire=" + isFromCopperWire + "}";
    }
}
