package net.apltd.copperwiremod.blockentity;

import net.apltd.copperwiremod.CopperWireMod;
import net.apltd.copperwiremod.block.CopperWire;
import net.apltd.copperwiremod.block.ModBlocks;
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
    private int powerV = 0;
    private int powerH = -1;
    private Direction powerVDir = Direction.NORTH;
    private Direction powerHDir = Direction.DOWN;
    private boolean hop = false;
    private int powerN = 0;
    private int powerE = 0;
    private int powerS = 0;
    private int powerW = 0;
    private boolean vertical = false;

    private int oldV = 0;
    private int oldH = 0;
    private Direction oldVDir = null;
    private Direction oldHDir = null;

    private int cPowerV = 0;
    private int cPowerH = -1;
    private Direction cPowerVDir = Direction.NORTH;
    private Direction cPowerHDir = Direction.DOWN;
    private int cPowerN = 0;
    private int cPowerE = 0;
    private int cPowerS = 0;
    private int cPowerW = 0;

    private boolean changing = false;
    private boolean modified = false;

    public CopperWireEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPERWIRE_ENTITY, pos, state);
        vertical = state.get(CopperWire.VERTICAL);
    }

    public int getPowerOut(Direction dir, Direction ignore) {
        return vertical
                ? dir == Direction.NORTH
                    ? powerN : dir == Direction.EAST
                        ? powerE : dir == Direction.SOUTH
                            ? powerS : powerW
                : !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH)
                    ? (powerVDir != ignore)
                        ? powerV : 0
                    : (powerHDir != ignore)
                        ? powerH : 0;
    }

    public int getPowerOut(Direction dir) {
        return getPowerOut(dir, Direction.UP);   //UP is unused, so it gets the power without ignoring input direction.
    }

    public int getPowerOut() {
        return vertical
                ? Math.max(powerN, Math.max(powerE, Math.max(powerS, powerW)))
                : Math.max(getPowerOut(Direction.NORTH, Direction.UP), getPowerOut(Direction.EAST, Direction.UP));
    }

    public Direction getPowerDir() {
        return vertical ? null : (powerVDir == Direction.DOWN) ? powerHDir : powerVDir;
    }

    public void setPower(Direction dir, int power) {
        boolean isNS = !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH);
        boolean checkOld = changing && modified;
        int newPower = power;
        int oldPower = vertical
                ? dir == Direction.NORTH
                    ? powerN : dir == Direction.EAST
                        ? powerE : dir == Direction.SOUTH
                            ? powerS : powerW
                : checkOld ? isNS ? oldV : oldH : getPowerOut(dir);
        Direction curDir = vertical ? null : checkOld ? isNS ? oldVDir : oldHDir: isNS ? powerVDir : powerHDir;

        if (vertical) {
            if (oldPower != newPower) {
                modified = true;
                switch (dir) {
                    case NORTH -> powerN = newPower;
                    case SOUTH -> powerS = newPower;
                    case EAST -> powerE = newPower;
                    case WEST -> powerW = newPower;
                }
            }
        }
        else if ((curDir == dir) ? newPower != oldPower : newPower > oldPower) {
            reset(isNS);
            if (isNS) {
                modified |= (powerV != power) || (powerVDir != dir);
                powerV = power;
                powerVDir = dir;
            }
            else {
                modified |= (powerH != power) || (powerHDir != dir);
                powerH = power;
                powerHDir = dir;
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

    public boolean isHop() {
        return hop;
    }

    public boolean isChanging() {
        return changing;
    }

    public void setChanging(boolean changing) {
        if (this.changing != changing) {
            this.changing = changing;

            if (changing) {
                preserve();
            }
            else {
                powerV = cPowerV;
                powerH = cPowerH;
                powerVDir = cPowerVDir;
                powerHDir = cPowerHDir;
                powerN = cPowerN;
                powerE = cPowerE;
                powerS = cPowerS;
                powerW = cPowerW;
            }
        }
    }

    public boolean isModified() { return modified; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        NbtCompound data = nbt.getCompound(NBT_NAME);
        powerV = data.getInt("powerV");
        powerVDir = Direction.byName(data.getString("powerVDir"));
        powerH = data.getInt("powerH");
        powerHDir = Direction.byName(data.getString("powerHDir"));
        hop = data.getBoolean("hop");
        vertical = data.getBoolean("vertical");
        powerN = data.getInt("powerN");
        powerE = data.getInt("powerE");
        powerS = data.getInt("powerS");
        powerW = data.getInt("powerW");

        if ((powerV < 0) || (powerV > 239)) {
            powerV = 0;
        }
        if (!Direction.Type.HORIZONTAL.test(powerVDir)) {
            powerVDir = Direction.NORTH;
            if (powerH != -1) {
                powerH = -1;
                powerHDir = Direction.DOWN;
            }
        }
        if (((powerH == -1) && (powerHDir != Direction.DOWN)) || (powerH < 0) || (powerH > 239)) {
            powerH = -1;
            powerHDir = Direction.DOWN;
        }
        CopperWireMod.LOGGER.trace("Loading entity state @(" + getPos().toShortString() + "), " + toShortString());
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        CopperWireMod.LOGGER.trace("Saving entity state @(" + getPos().toShortString() + "), " + toShortString());
        NbtCompound data = new NbtCompound();
        data.putInt("powerV", powerV);
        data.putString("powerVDir", powerVDir.getName());
        data.putInt("powerH", powerH);
        data.putString("powerHDir", powerHDir.getName());
        data.putBoolean("hop", hop);
        data.putBoolean("vertical", vertical);
        data.putInt("powerN", powerN);
        data.putInt("powerE", powerE);
        data.putInt("powerS", powerS);
        data.putInt("powerW", powerW);
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
        return vertical
                ? "CP{" +
                "powerN=" + powerN +
                ", powerE=" + powerE +
                ", powerS=" + powerS +
                ", powerW=" + powerW +
                ", modified=" + modified +
                '}'
                : "CP{" +
                "powerV=" + powerV +
                ", powerVDir=" + powerVDir +
                ", powerH=" + powerH +
                ", powerHDir=" + powerHDir +
                ", hop=" + hop +
                ", oldV=" + oldV +
                ", oldVDir=" + oldVDir +
                ", oldH=" + oldH +
                ", oldHDir=" + oldHDir +
                ", modified=" + modified +
                '}';
    }

    public String toShortString() {
        return vertical
                ? "CP{" +
                "powerN=" + powerN +
                ", powerE=" + powerE +
                ", powerS=" + powerS +
                ", powerW=" + powerW +
                ", modified=" + modified +
                '}'
                : "CP{" +
                "powerV=" + powerV +
                ", powerVDir=" + powerVDir +
                ", powerH=" + powerH +
                ", powerHDir=" + powerHDir +
                ", hop=" + hop +
                ", modified=" + modified +
                '}';
    }

    public void clearAll() {
        powerV = 0;
        powerH = -1;
        powerVDir = Direction.NORTH;
        powerHDir = Direction.DOWN;
        hop = false;
        oldV = 0;
        oldH = 0;
        oldVDir = null;
        oldHDir = null;
        cPowerV = 0;
        cPowerH = -1;
        cPowerVDir = Direction.NORTH;
        cPowerHDir = Direction.DOWN;
        changing = false;
        powerN = 0;
        powerE = 0;
        powerS = 0;
        powerW = 0;
        modified = true;
    }

    public void reset() {
        setHop(false);
        reset(true);
    }

    public void reset(Direction dir) {
        boolean isNS = !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH);
        Direction powerDir = isNS ? powerVDir : powerHDir;

        if (vertical) {
            modified = getPowerOut() != 0;
            switch (dir) {
                case NORTH -> powerN = 0;
                case SOUTH -> powerS = 0;
                case EAST -> powerE = 0;
                case WEST -> powerW = 0;
            }
        }
        else if (powerDir == dir) {
            if (isNS) {
                modified = powerV != 0;
                powerV = 0;
            }
            else {
                modified = powerH != 0;
                powerH = 0;
            }
        }
    }

    private void reset(boolean isNS) {
        if (vertical) {
            modified = getPowerOut() != 0;
            powerN = 0;
            powerE = 0;
            powerS = 0;
            powerW = 0;
        }
        else {
            if (hop) {
                if (isNS) {
                    oldV = powerV;
                    oldVDir = powerVDir;
                } else {
                    oldH = powerH;
                    oldHDir = powerHDir;
                }
            } else {
                if (isNS) {
                    oldV = Math.max(powerV, powerH);
                    oldVDir = (oldV == powerV) ? powerVDir : powerHDir;
                } else {
                    oldH = -1;
                    oldHDir = Direction.DOWN;
                }
            }

            if (isNS) {
                modified = (powerV != 0) || (powerVDir != Direction.NORTH);
                powerV = 0;
                powerVDir = Direction.NORTH;
            } else {
                modified = (powerH > 0) || (powerHDir != ((hop) ? Direction.NORTH : Direction.DOWN));
                powerH = (hop) ? 0 : -1;
                powerHDir = (hop) ? Direction.NORTH : Direction.DOWN;
            }
        }
    }

    private int CPtoRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

    private void preserve() {
        cPowerV = powerV;
        cPowerH = powerH;
        cPowerVDir = powerVDir;
        cPowerHDir = powerHDir;
        cPowerN = powerN;
        cPowerE = powerE;
        cPowerS = powerS;
        cPowerW = powerW;
    }
}
