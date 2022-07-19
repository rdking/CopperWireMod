package net.apltd.copperwiremod.blockentity;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.block.CopperWire;
import net.apltd.copperwiremod.util.CopperPower;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;


public class CopperWireEntity extends BlockEntity {
    public static final String COPPERWIRE_ENTITYNAME = "copperwire_entity";
    private static final String NBT_NAME = "copper_wire_power";

    //CopperPower Data
    private int powerN = 0;
    private int powerE = 0;
    private int powerS = 0;
    private int powerW = 0;
    private Direction srcDirN = Direction.DOWN;
    private Direction srcDirE = Direction.DOWN;
    private Direction srcDirS = Direction.DOWN;
    private Direction srcDirW = Direction.DOWN;

    //Mode Data
    private boolean hop = false;
    private boolean vertical;

    //Copy data
    private int cPowerN = 0;
    private int cPowerE = 0;
    private int cPowerS = 0;
    private int cPowerW = 0;
    private Direction cSrcDirN = Direction.DOWN;
    private Direction cSrcDirE = Direction.DOWN;
    private Direction cSrcDirS = Direction.DOWN;
    private Direction cSrcDirW = Direction.DOWN;

    //Copy data
    private int oPowerN = 0;
    private int oPowerE = 0;
    private int oPowerS = 0;
    private int oPowerW = 0;
    private Direction oSrcDirN = Direction.DOWN;
    private Direction oSrcDirE = Direction.DOWN;
    private Direction oSrcDirS = Direction.DOWN;
    private Direction oSrcDirW = Direction.DOWN;

    //Dirty state
    private boolean changing = false;
    private boolean modified = false;

    public CopperWireEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPERWIRE_ENTITY, pos, state);
        vertical = state.get(CopperWire.VERTICAL);
    }

    public int getMaxPowerOut() {
        return Math.max(getPowerOut(Direction.NORTH),
                Math.max(getPowerOut(Direction.EAST),
                        Math.max(getPowerOut(Direction.SOUTH), getPowerOut(Direction.WEST))));
    }

    public int getPowerOut(Direction dir) {
        return hop
                ? (dir == Direction.NORTH) || (dir == Direction.SOUTH)
                    ? Math.max(powerN, powerS)
                    : Math.max(powerE, powerW)
                : vertical
                    ? dir == Direction.NORTH ? powerN
                        : dir == Direction.EAST ? powerE
                        : dir == Direction.SOUTH ? powerS : powerW
                    : Math.max(powerN, Math.max(powerE, Math.max(powerS, powerW)));
    }

    public int getPowerOut(Direction dir, Direction ignore) {
        int retval = 0;
        switch(dir) {
            case NORTH -> {
                if (srcDirN != ignore) {
                    retval = powerN;
                }
            }
            case SOUTH -> {
                if (srcDirS != ignore) {
                    retval = powerS;
                }
            }
            case EAST -> {
                if (srcDirE != ignore) {
                    retval = powerE;
                }
            }
            case WEST -> {
                if (srcDirW != ignore) {
                    retval = powerW;
                }
            }
        }

        return retval;
    }

    private int getOldPowerOut(Direction dir) {
        return hop
                ? (dir == Direction.NORTH) || (dir == Direction.SOUTH)
                ? Math.max(oPowerN, oPowerS)
                : Math.max(oPowerE, oPowerW)
                : vertical
                ? dir == Direction.NORTH ? oPowerN
                : dir == Direction.EAST ? oPowerE
                : dir == Direction.SOUTH ? oPowerS : oPowerW
                : Math.max(oPowerN, Math.max(oPowerE, Math.max(oPowerS, oPowerW)));
    }

    public Direction getPowerSrcDir(Direction dir) {
        Direction retval = null;
        switch (dir) {
            case NORTH -> retval = srcDirN;
            case EAST -> retval = srcDirE;
            case SOUTH -> retval = srcDirS;
            case WEST -> retval = srcDirW;
        }
        return retval;
    }

    public void setPower(Direction cDir, CopperPower p) {
        setPower(cDir, p.sDir, p.power > 0 ? p.power - 1 : 0, p.isFromRedstoneWire);
    }

    private int CPtoRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

    private void setPower(Direction cDir, Direction sDir, int power, boolean fromRedstone) {
        int cPower = getPowerOut(cDir);
        int oldPower = getOldPowerOut(cDir);
        Direction oldDir = getPowerSrcDir(cDir);
        boolean updated = (power != cPower) || (sDir != oldDir);
        modified |= updated;

        if (updated) {
            if (vertical) {
                switch (cDir) {
                    case NORTH -> {
                        powerN = power;
                        srcDirN = sDir;
                    }
                    case SOUTH -> {
                        powerS = power;
                        srcDirS = sDir;
                    }
                    case EAST -> {
                        powerE = power;
                        srcDirE = sDir;
                    }
                    case WEST -> {
                        powerW = power;
                        srcDirW = sDir;
                    }
                }
            } else {
                boolean isH = hop && ((cDir == Direction.EAST) || (cDir == Direction.WEST));
                if (isH) {
                    srcDirE = sDir;
                    srcDirW = sDir;
                } else {
                    srcDirN = sDir;
                    srcDirS = sDir;
                    if (!hop) {
                        srcDirE = sDir;
                        srcDirW = sDir;
                    }
                }

                if (!hop || !isH) {
                    powerN = power;
                    powerS = power;
                }

                if (!hop || isH) {
                    powerE = power;
                    powerW = power;
                }
            }
        }
    }

    public void setHop(boolean hop) {
        modified = (hop != this.hop);
        if (modified) {
            this.hop = hop;
            setChanged();
        }
    }

    public void setChanging(boolean changing) {
        if (this.changing != changing) {
            this.changing = changing;

            if (changing) {
                saveOld();
                preserve();
            }
            else {
                restore();
            }
        }
    }

    public boolean changedForDirection(Direction dir) {
        return dir == Direction.NORTH ? (powerN != oPowerN) || (srcDirN != oSrcDirN)
                : dir == Direction.EAST ? (powerE != oPowerE) || (srcDirE != oSrcDirE)
                : dir == Direction.SOUTH ? (powerS != oPowerS) || (srcDirS != oSrcDirS)
                : dir == Direction.WEST && ((powerW != oPowerW) || (srcDirW != oSrcDirW));
    }

    public boolean isModified() { return modified; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        NbtCompound data = nbt.getCompound(NBT_NAME);
        powerN = data.getInt("powerN");
        powerE = data.getInt("powerE");
        powerS = data.getInt("powerS");
        powerW = data.getInt("powerW");
        srcDirN = Direction.byName(data.getString("srcDirN"));
        srcDirE = Direction.byName(data.getString("srcDirE"));
        srcDirS = Direction.byName(data.getString("srcDirS"));
        srcDirW = Direction.byName(data.getString("srcDirW"));
        hop = data.getBoolean("hop");
        vertical = data.getBoolean("vertical");

        if (srcDirN == null) {
            srcDirN = Direction.DOWN;
        }
        if (srcDirE == null) {
            srcDirE = Direction.DOWN;
        }
        if (srcDirS == null) {
            srcDirS = Direction.DOWN;
        }
        if (srcDirW == null) {
            srcDirW = Direction.DOWN;
        }

        if (powerN < 0) powerN = 0;
        if (powerE < 0) powerE = 0;
        if (powerS < 0) powerS = 0;
        if (powerW < 0) powerW = 0;
        while (powerN > 239) powerN >>= 4;
        while (powerE > 239) powerE >>= 4;
        while (powerS > 239) powerS >>= 4;
        while (powerW > 239) powerW >>= 4;

        saveOld();

        CopperWireMod.LOGGER.trace("Loading entity state @(" + getPos().toShortString() + "), " + toShortString());
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        CopperWireMod.LOGGER.trace("Saving entity state @(" + getPos().toShortString() + "), " + toShortString());
        NbtCompound data = new NbtCompound();
        data.putInt("powerN", powerN);
        data.putInt("powerE", powerE);
        data.putInt("powerS", powerS);
        data.putInt("powerW", powerW);
        data.putString("srcDirN", srcDirN.getName());
        data.putString("srcDirE", srcDirE.getName());
        data.putString("srcDirS", srcDirS.getName());
        data.putString("srcDirW", srcDirW.getName());
        data.putBoolean("hop", hop);
        data.putBoolean("vertical", vertical);
        nbt.put(NBT_NAME, data);
        super.writeNbt(nbt);
    }

    public void setChanged() {
        if (modified) {
            modified = false;
            preserve();
            markDirty();
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public String toString() {
        return "CP{" +
                "powerN=" + powerN +
                ", srcDirN=" + srcDirN +
                ", powerE=" + powerE +
                ", srcDirE=" + srcDirE +
                ", powerS=" + powerS +
                ", srcDirS=" + srcDirS +
                ", powerW=" + powerW +
                ", srcDirW=" + srcDirW +
                ", modified=" + modified +
                '}';
    }

    public String toShortString() {
        return "CP{" +
                "powerN=" + powerN +
                ", powerE=" + powerE +
                ", powerS=" + powerS +
                ", powerW=" + powerW +
                ", modified=" + modified +
                '}';
    }

    public void reset(Direction dir) {
        setPower(dir, Direction.DOWN, 0, false);
    }

    public void clearAll() {
        powerN = 0;
        powerE = 0;
        powerS = 0;
        powerW = 0;
        srcDirN = Direction.DOWN;
        srcDirE = Direction.DOWN;
        srcDirS = Direction.DOWN;
        srcDirW = Direction.DOWN;
        vertical = false;
        hop = false;
        modified = true;
    }

    private void saveOld() {
        oPowerN = powerN;
        oPowerE = powerE;
        oPowerS = powerS;
        oPowerW = powerW;
        oSrcDirN = srcDirN;
        oSrcDirE = srcDirE;
        oSrcDirS = srcDirS;
        oSrcDirW = srcDirW;
    }

    private void preserve() {
        cPowerN = powerN;
        cPowerE = powerE;
        cPowerS = powerS;
        cPowerW = powerW;
        cSrcDirN = srcDirN;
        cSrcDirE = srcDirE;
        cSrcDirS = srcDirS;
        cSrcDirW = srcDirW;
    }

    private void restore() {
        powerN = cPowerN;
        powerE = cPowerE;
        powerS = cPowerS;
        powerW = cPowerW;
        srcDirN = cSrcDirN;
        srcDirE = cSrcDirE;
        srcDirS = cSrcDirS;
        srcDirW = cSrcDirW;
    }
}
