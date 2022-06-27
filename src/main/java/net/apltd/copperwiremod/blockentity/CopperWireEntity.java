package net.apltd.copperwiremod.blockentity;

import net.apltd.copperwiremod.CopperWireMod;
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
    private int powerV = 0;
    private int powerH = -1;
    private Direction powerVDir = Direction.NORTH;
    private Direction powerHDir = Direction.DOWN;
    private boolean hop = false;

    private int oldV = 0;
    private int oldH = 0;
    private Direction oldVDir = null;
    private Direction oldHDir = null;

    private int cPowerV = 0;
    private int cPowerH = -1;
    private Direction cPowerVDir = Direction.NORTH;
    private Direction cPowerHDir = Direction.DOWN;

    private boolean changing = false;
    private boolean modified = false;

    public CopperWireEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPERWIRE_ENTITY, pos, state);
    }

    public int getPowerIn(Direction dir) {
        return !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH)
                ? (powerVDir == dir)
                    ? powerV : 0
                : (powerHDir == dir)
                    ? powerH : 0;
    }

    public int getPowerOut(Direction dir, Direction ignore) {
        return !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH)
                ? (powerVDir != ignore)
                    ? powerV : 0
                : (powerHDir != ignore)
                    ? powerH : 0;
    }

    public int getPowerOut(Direction dir) {
        return getPowerOut(dir, Direction.UP);   //UP is unused, so it gets the power without ignoring input direction.
    }

    public int getPowerOut() {
        return getPowerOut(Direction.NORTH, Direction.UP);
    }

    public Direction getPowerDir() {
        return (powerVDir == Direction.DOWN) ? powerHDir : powerVDir;
    }

    public void setPower(Direction dir, int power, boolean srcIsRedstone) {
        boolean isNS = !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH);
        boolean checkOld = changing && modified;
        int newPower = power;
        int oldPower = checkOld ? isNS ? oldV : oldH : getPowerOut(dir);
        Direction curDir = checkOld ? isNS ? oldVDir : oldHDir: isNS ? powerVDir : powerHDir;

        if (srcIsRedstone) {
            oldPower = CPtoRP(oldPower);
            newPower = CPtoRP(newPower);
        }

        if ((curDir == dir) ? newPower != oldPower : newPower > oldPower) {
            reset(isNS);
            if (isNS) {
                powerV = power;
                powerVDir = dir;
            }
            else {
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
            }
        }
    }

    public boolean isModified() { return modified; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        NbtCompound data = nbt.getCompound("copper_wire_power");
        powerV = data.getInt("powerV");
        powerVDir = Direction.byName(data.getString("powerVDir"));
        powerH = data.getInt("powerH");
        powerHDir = Direction.byName(data.getString("powerHDir"));
        hop = data.getBoolean("hop");

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
        nbt.put("copper_wire_power", data);
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
        return "CP{" +
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
        modified = true;
    }

    public void reset() {
        setHop(false);
        reset(true);
    }

    public void reset(Direction dir) {
        boolean isNS = !hop || (dir == Direction.NORTH) || (dir == Direction.SOUTH);
        Direction powerDir = isNS ? powerVDir : powerHDir;

        if (powerDir == dir) {
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
        if (hop) {
            if (isNS) {
                oldV = powerV;
                oldVDir = powerVDir;
            }
            else {
                oldH = powerH;
                oldHDir = powerHDir;
            }
        }
        else {
            if (isNS) {
                oldV = Math.max(powerV, powerH);
                oldVDir = (oldV == powerV) ? powerVDir : powerHDir;
            }
            else {
                oldH = -1;
                oldHDir = Direction.DOWN;
            }
        }

        if (isNS) {
            powerV = 0;
            powerVDir = Direction.NORTH;
        }
        else {
            powerH = (hop) ? 0 : -1;
            powerHDir = (hop) ? Direction.NORTH : Direction.DOWN;
        }

        modified = true;
    }

    private int CPtoRP(int cp) {
        return (cp >> 4) + ((cp & 0x0f) > 0 ? 1 : 0);
    }

    private void preserve() {
        cPowerV = powerV;
        cPowerH = powerH;
        cPowerVDir = powerVDir;
        cPowerHDir = powerHDir;
    }
}
