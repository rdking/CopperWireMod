package net.apltd.copperwiremod.util;

import static net.apltd.copperwiremod.util.CopperTools.CPtoRP;
import net.minecraft.util.math.Direction;

import java.util.Objects;

@SuppressWarnings("unused")
public class CopperPower {
    private int power;                  //Strength of the copper wire power
    private Direction sDir;             //Direction from which the power was supplied
    private boolean fromRedstone;     //True if the power reading came from redstone.
    private boolean fromCopperWire;   //True if the power reading came from copper wire.

    public CopperPower() {
        power = 0;
        sDir = Direction.DOWN;
        fromRedstone = false;
        fromCopperWire = false;
    }

    public CopperPower(int power, Direction source, boolean isFromRedstone, boolean isFromCopperWire) {
        this.power = power;
        sDir = source;
        fromRedstone = isFromRedstone;
        fromCopperWire = isFromCopperWire;
    }

    public int getRPower() { return CPtoRP(power); }
    public void setRPower(int p) {
        p %= 16;
        if (p < 0) { p += 16; }
        power = p * 16;
    }
    public int getCPower() { return power; }
    public void setCPower(int p) {
        p %= 241;
        if (p < 0) { p += 241; }
        power = p;
    }

    public Direction getDir() { return sDir; }
    public void setDir(Direction d) { sDir = d; }

    public boolean isFromRedstone() { return fromRedstone; }
    public void setFromRedstone(boolean fr) { fromRedstone = fr; }

    public boolean isFromCopperWire() { return fromCopperWire; }
    public void setFromCopperWire(boolean fc) { fromCopperWire = fc; }

    @Override
    public String toString() {
        return "CopperPower{power=" + power + ", sDir=" + sDir +
                ", fromRedstone=" + fromRedstone + ", fromCopperWire=" + fromCopperWire + "}";
    }

    public String toShortString() {
        return "{power=" + power + ", sDir=" + sDir + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CopperPower that = (CopperPower) o;
        return power == that.power && sDir == that.sDir;
    }

    @Override
    public int hashCode() {
        return Objects.hash(power, sDir);
    }
}
