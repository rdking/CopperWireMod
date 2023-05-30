package net.apltd.copperwiremod.util;

import net.apltd.copperwiremod.block.CopperWire;
import net.apltd.copperwiremod.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public class CopperPower {
    private int powerN = 0;
    private int powerE = 0;
    private int powerS = 0;
    private int powerW = 0;

    public CopperPower() {}

    public CopperPower(int n, int e, int s, int w) {
        powerN = n;
        powerE = e;
        powerS = s;
        powerW = w;
        adjustPower();
    }

    public int getCopperSignal(BlockState state, Direction dir) {
        boolean hop = state.get(CopperWire.HOP);
        boolean vertical = state.get(CopperWire.VERTICAL);

        if (Direction.Type.VERTICAL.test(dir)) {
            throw new IllegalArgumentException("The direction cannot be up or down.");
        }

        return hop
                ? (dir.getAxis() == Direction.Axis.Z)
                    ? Math.max(powerN, powerS)
                    : Math.max(powerE, powerW)
                : vertical
                    ? (dir == Direction.NORTH) ? powerN
                        : (dir == Direction.EAST) ? powerE
                        : (dir == Direction.SOUTH) ? powerS : powerW
                    :  Math.max(powerN, Math.max(powerE, Math.max(powerS, powerW)));
    }

    public int getRedstonePower(BlockState state, Direction dir) {
        int power = getCopperSignal(state, dir);
        return power >> 4;
    }

    public int getPowerStep(BlockState state, Direction dir) {
        int power = getCopperSignal(state, dir);
        return power & 15;
    }

    public void setPower(int power, int step, Direction dir) {
        if (Direction.Type.VERTICAL.test(dir)) {
            throw new IllegalArgumentException("The direction cannot be up or down.");
        }

        int cpower = power << 4 | step;
        switch (dir) {
            case NORTH -> powerN = cpower;
            case EAST -> powerE = cpower;
            case SOUTH -> powerS = cpower;
            case WEST -> powerW = cpower;
        }

        adjustPower();
    }

    public void normalize(BlockState state) {
        if (state.isOf(ModBlocks.COPPER_WIRE)) {
            boolean hop = state.get(CopperWire.HOP);
            boolean vertical = state.get(CopperWire.VERTICAL);

            if (hop) {
                powerN = Math.max(powerN, powerS);
                powerE = Math.max(powerE, powerW);
                powerS = powerN;
                powerW = powerE;
            }
            else if (!vertical) {
                powerN = Math.max(powerN, Math.max(powerE, Math.max(powerS, powerW)));
                powerS = powerN;
                powerE = powerS;
                powerW = powerE;
            }
        }
    }

    @Override
    public String toString() {
        return "CP{powerN=" + powerN
                + ", powerE=" + powerE
                + ", powerS=" + powerS
                + ", powerW=" + powerW
                + "}";
    }

    private void adjustPower() {
        powerN = powerN > 15 ? powerN : 0;
        powerE = powerE > 15 ? powerE : 0;
        powerS = powerS > 15 ? powerS : 0;
        powerW = powerW > 15 ? powerW : 0;
    }

    public void write(NbtCompound data) {
        data.putInt("powerN", powerN);
        data.putInt("powerE", powerE);
        data.putInt("powerS", powerS);
        data.putInt("powerW", powerW);
    }

    public void reset() {
        powerN = 0;
        powerE = 0;
        powerS = 0;
        powerW = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CopperPower that = (CopperPower) o;
        return powerN == that.powerN && powerE == that.powerE && powerS == that.powerS && powerW == that.powerW;
    }

    @Override
    public int hashCode() {
        return Objects.hash(powerN, powerE, powerS, powerW);
    }
}
