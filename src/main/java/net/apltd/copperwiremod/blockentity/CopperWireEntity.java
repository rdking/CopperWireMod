package net.apltd.copperwiremod.blockentity;

import net.apltd.copperwiremod.CopperWireMod;
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
    private CopperPower active;
    private CopperPower current;
    private CopperPower old;

    //Dirty state
    private boolean changing = false;
    private boolean modified = false;

    public CopperWireEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPERWIRE_ENTITY, pos, state);
        active = new CopperPower();
        current = new CopperPower();
        old = new CopperPower();
    }

    public int getMaxPowerOut(BlockState state) {
        return Math.max(getPowerOut(state, Direction.NORTH),
                Math.max(getPowerOut(state, Direction.EAST),
                        Math.max(getPowerOut(state, Direction.SOUTH), getPowerOut(state, Direction.WEST))));
    }

    public int getPowerOut(BlockState state, Direction dir) {
        return active.getCopperSignal(state, dir);
    }

    public int getRedstonePower(BlockState state, Direction dir) {
        return active.getRedstonePower(state, dir);
    }

    public int getPowerStep(BlockState state, Direction dir) {
        return active.getPowerStep(state, dir);
    }

    public void setPower(int power, int step, Direction dir) {
        active.setPower(power, step, dir);
    }

    public void normalize(BlockState state) {
        active.normalize(state);
        modified = !active.equals(current);
    }

    public void setChanging(boolean changing) {
        if (this.changing != changing) {
            this.changing = changing;

            if (changing) {
                old = active;
                current = active;
            }
            else {
                active = current;
            }
        }
    }

    public boolean isModified() { return modified; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        NbtCompound data = nbt.getCompound(NBT_NAME);
        int powerN = Math.min(255, Math.max(0, data.getInt("powerN")));
        int powerE = Math.min(255, Math.max(0, data.getInt("powerE")));
        int powerS = Math.min(255, Math.max(0, data.getInt("powerS")));
        int powerW = Math.min(255, Math.max(0, data.getInt("powerW")));

        old = active;
        active = new CopperPower(powerN, powerE, powerS, powerW);

        CopperWireMod.LOGGER.trace("Loading entity state @(" + getPos().toShortString() + "), " + toShortString());
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        CopperWireMod.LOGGER.trace("Saving entity state @(" + getPos().toShortString() + "), " + toShortString());
        NbtCompound data = new NbtCompound();
        active.write(data);
        nbt.put(NBT_NAME, data);
        super.writeNbt(nbt);
    }

    public void setChanged() {
        if (modified) {
            modified = false;
            current = active;
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
        return "active-" + active.toString()
                + "old-" + old.toString()
                +"modified-" + modified;
    }

    public String toShortString() {
        return "active-" + active.toString()
                +"modified=" + modified;
    }

    public void clearAll() {
        active.reset();
        modified = true;
    }
}
