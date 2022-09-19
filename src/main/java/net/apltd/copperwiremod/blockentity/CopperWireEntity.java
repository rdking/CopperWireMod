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

    //Mode Data
    private boolean hop = false;
    private boolean vertical;

    //Dirty state
    private boolean changing = false;
    private boolean modified = false;

    //CopperPower Data
    private CopperPower powerN = new CopperPower();
    private CopperPower powerE = new CopperPower();
    private CopperPower powerS = new CopperPower();
    private CopperPower powerW = new CopperPower();

    //Copy data
    private CopperPower cPowerN = new CopperPower();
    private CopperPower cPowerE = new CopperPower();
    private CopperPower cPowerS = new CopperPower();
    private CopperPower cPowerW = new CopperPower();

    //Previous data
    private CopperPower oPowerN = new CopperPower();
    private CopperPower oPowerE = new CopperPower();
    private CopperPower oPowerS = new CopperPower();
    private CopperPower oPowerW = new CopperPower();

    public CopperWireEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPERWIRE_ENTITY, pos, state);
        vertical = state.contains(CopperWire.VERTICAL) && state.get(CopperWire.VERTICAL);
    }

    public int getMaxPowerOut() {
        return Math.max(getPowerOut(Direction.NORTH),
                Math.max(getPowerOut(Direction.EAST),
                        Math.max(getPowerOut(Direction.SOUTH), getPowerOut(Direction.WEST))));
    }

    public int getPowerOut(Direction dir) {
        return hop
                ? (dir == Direction.NORTH) || (dir == Direction.SOUTH)
                    ? Math.max(powerN.getCPower(), powerS.getCPower())
                    : Math.max(powerE.getCPower(), powerW.getCPower())
                : vertical
                    ? dir == Direction.NORTH ? powerN.getCPower()
                        : dir == Direction.EAST ? powerE.getCPower()
                        : dir == Direction.SOUTH ? powerS.getCPower() : powerW.getCPower()
                    : Math.max(powerN.getCPower(), Math.max(powerE.getCPower(), Math.max(powerS.getCPower(), powerW.getCPower())));
    }

    public int getPowerOut(Direction dir, Direction ignore) {
        int retval = 0;
        switch(dir) {
            case NORTH -> {
                if (powerN.getDir() != ignore) {
                    retval = powerN.getCPower();
                }
            }
            case EAST -> {
                if (powerE.getDir() != ignore) {
                    retval = powerE.getCPower();
                }
            }
            case SOUTH -> {
                if (powerS.getDir() != ignore) {
                    retval = powerS.getCPower();
                }
            }
            case WEST -> {
                if (powerW.getDir() != ignore) {
                    retval = powerW.getCPower();
                }
            }
        }

        return retval;
    }

    public void setPower(Direction cDir, CopperPower power) {
        power.setCPower(Math.max(0, power.getCPower() - (power.isFromCopperWire() ? 1 : 0)));
        if (changing) {
            switch (cDir) {
                case NORTH -> {
                    if (!powerN.equals(power)) {
                        powerN = power;
                    }
                }
                case EAST -> {
                    if (!powerE.equals(power)) {
                        powerE = power;
                    }
                }
                case SOUTH -> {
                    if (!powerS.equals(power)) {
                        powerS = power;
                    }
                }
                case WEST -> {
                    if (!powerW.equals(power)) {
                        powerW = power;
                    }
                }
            }
        }
        validatePower();
    }

    public void resolve() {
        if (!vertical) {
            if (hop) {
                int pN = powerN.getCPower();
                int pE = powerE.getCPower();
                int pS = powerS.getCPower();
                int pW = powerW.getCPower();

                if (pE != pW) {
                    if (pE > pW) {
                        powerW = powerE;
                    }
                    else {
                        powerE = powerW;
                    }
                }
                if (pN != pS) {
                    if (pN > pS) {
                        powerS = powerN;
                    }
                    else {
                        powerN = powerS;
                    }
                }
            }
            else {
                int power = getMaxPowerOut();
                Direction dir = oPowerN.getDir();
                CopperPower p = dir == Direction.NORTH ? powerN
                        : dir == Direction.EAST ? powerE
                        : dir == Direction.SOUTH ? powerS
                        : dir == Direction.WEST ? powerW : oPowerN;
                CopperPower max = powerN.getCPower() == power ? powerN
                        : powerE.getCPower() == power ? powerE
                        : powerS.getCPower() == power ? powerS : powerW;

                if ((power > p.getCPower()) && (!max.isFromRedstone() || (max.getRPower() != oPowerN.getRPower()))) {
                    p = max;
                }

                powerN = p;
                powerE = p;
                powerS = p;
                powerW = p;
            }
        }

        modified = !powerN.equals(cPowerN) || !powerE.equals(cPowerE) ||
                !powerS.equals(cPowerS) || !powerW.equals(cPowerW);
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
        return dir == Direction.NORTH ? !powerN.equals(oPowerN)
                : dir == Direction.EAST ? !powerE.equals(oPowerE)
                : dir == Direction.SOUTH ? !powerS.equals(oPowerS)
                : dir == Direction.WEST && !powerW.equals(oPowerW);
    }

    public boolean isModified() { return modified; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        NbtCompound data = nbt.getCompound(NBT_NAME);

        powerN.setCPower(data.getInt("powerN"));
        powerE.setCPower(data.getInt("powerE"));
        powerS.setCPower(data.getInt("powerS"));
        powerW.setCPower(data.getInt("powerW"));
        powerN.setDir(Direction.byName(data.getString("srcDirN")));
        powerE.setDir(Direction.byName(data.getString("srcDirE")));
        powerS.setDir(Direction.byName(data.getString("srcDirS")));
        powerW.setDir(Direction.byName(data.getString("srcDirW")));
        hop = data.getBoolean("hop");
        vertical = data.getBoolean("vertical");

        if (powerN.getDir() == null) {
            powerN.setDir(Direction.DOWN);
        }
        if (powerE.getDir() == null) {
            powerE.setDir(Direction.DOWN);
        }
        if (powerS.getDir() == null) {
            powerS.setDir(Direction.DOWN);
        }
        if (powerW.getDir() == null) {
            powerW.setDir(Direction.DOWN);
        }

        validatePower();
        saveOld();

        CopperWireMod.LOGGER.trace("Loading entity state @(" + getPos().toShortString() + "), " + toShortString());
    }

    private void validatePower() {
        if (powerN.getCPower() < 0) powerN.setCPower(0);
        if (powerE.getCPower() < 0) powerE.setCPower(0);
        if (powerS.getCPower() < 0) powerS.setCPower(0);
        if (powerW.getCPower() < 0) powerW.setCPower(0);
        if (powerN.getCPower() > 240) powerN.setCPower((powerN.getCPower() % 16) * 16);
        if (powerE.getCPower() > 240) powerE.setCPower((powerE.getCPower() % 16) * 16);
        if (powerS.getCPower() > 240) powerS.setCPower((powerS.getCPower() % 16) * 16);
        if (powerW.getCPower() > 240) powerW.setCPower((powerW.getCPower() % 16) * 16);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        CopperWireMod.LOGGER.trace("Saving entity state @(" + getPos().toShortString() + "), " + toShortString());
        NbtCompound data = new NbtCompound();
        data.putInt("powerN", powerN.getCPower());
        data.putInt("powerE", powerE.getCPower());
        data.putInt("powerS", powerS.getCPower());
        data.putInt("powerW", powerW.getCPower());
        data.putString("srcDirN", powerN.getDir().getName());
        data.putString("srcDirE", powerE.getDir().getName());
        data.putString("srcDirS", powerS.getDir().getName());
        data.putString("srcDirW", powerW.getDir().getName());
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
                "powerN=" + powerN.toShortString() +
                ", powerE=" + powerE.toShortString() +
                ", powerS=" + powerS.toShortString() +
                ", powerW=" + powerW.toShortString() +
                ", modified=" + modified +
                '}';
    }

    public String toShortString() {
        return "CP{" +
                "powerN=" + powerN.getCPower() +
                ", powerE=" + powerE.getCPower() +
                ", powerS=" + powerS.getCPower() +
                ", powerW=" + powerW.getCPower() +
                ", modified=" + modified +
                '}';
    }

    public void clearAll() {
        powerN.setCPower(0);
        powerE.setCPower(0);
        powerS.setCPower(0);
        powerW.setCPower(0);
        powerN.setDir(Direction.DOWN);
        powerE.setDir(Direction.DOWN);
        powerS.setDir(Direction.DOWN);
        powerW.setDir(Direction.DOWN);
        vertical = false;
        hop = false;
        modified = true;
    }

    private void saveOld() {
        oPowerN = powerN;
        oPowerE = powerE;
        oPowerS = powerS;
        oPowerW = powerW;
    }

    private void preserve() {
        cPowerN = powerN;
        cPowerE = powerE;
        cPowerS = powerS;
        cPowerW = powerW;
    }

    private void restore() {
        powerN = cPowerN;
        powerE = cPowerE;
        powerS = cPowerS;
        powerW = cPowerW;
    }
}
