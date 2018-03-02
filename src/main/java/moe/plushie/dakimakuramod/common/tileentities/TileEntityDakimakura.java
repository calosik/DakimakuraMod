package moe.plushie.dakimakuramod.common.tileentities;

import java.util.List;

import moe.plushie.dakimakuramod.DakimakuraMod;
import moe.plushie.dakimakuramod.common.config.ConfigHandler;
import moe.plushie.dakimakuramod.common.dakimakura.Daki;
import moe.plushie.dakimakuramod.common.dakimakura.serialize.DakiNbtSerializer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityDakimakura extends TileEntity {
    
    private static final String TAG_FLIPPED = "flipped";
    
    private String packDirName;
    private String dakiDirName;
    private boolean flipped;
    
    public void setDaki(Daki daki) {
        if (daki != null) {
            packDirName = daki.getPackDirectoryName();
            dakiDirName = daki.getDakiDirectoryName();
        } else {
            packDirName = null;
            dakiDirName = null;
        }
        markDirty();
        //worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    public Daki getDaki() {
        return DakimakuraMod.getProxy().getDakimakuraManager().getDakiFromMap(packDirName, dakiDirName);
    }
    
    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        markDirty();
        syncWithClients();
        //worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    public boolean isFlipped() {
        return flipped;
    }
    
    public void flip() {
        setFlipped(!flipped);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey(DakiNbtSerializer.TAG_DAKI_PACK_NAME, NBT.TAG_STRING) & compound.hasKey(DakiNbtSerializer.TAG_DAKI_DIR_NAME, NBT.TAG_STRING)) {
            packDirName = compound.getString(DakiNbtSerializer.TAG_DAKI_PACK_NAME);
            dakiDirName = compound.getString(DakiNbtSerializer.TAG_DAKI_DIR_NAME);
        }
        flipped = compound.getBoolean(TAG_FLIPPED);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (packDirName != null & dakiDirName != null) {
            compound.setString(DakiNbtSerializer.TAG_DAKI_PACK_NAME, packDirName);
            compound.setString(DakiNbtSerializer.TAG_DAKI_DIR_NAME, dakiDirName);
        }
        compound.setBoolean(TAG_FLIPPED, flipped);
        return compound;
    }
    
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = super.getUpdateTag();
        writeToNBT(compound);
        return compound;
    }
    
    public Packet getDescriptionPacket() {

        return new SPacketUpdateTileEntity(getPos(), 5, getUpdateTag());
    }
    
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), getUpdateTag());
    }
    
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        NBTTagCompound compound = packet.getNbtCompound();
        readFromNBT(compound);
        super.onDataPacket(net, packet);
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public double getMaxRenderDistanceSquared() {
        return ConfigHandler.dakiRenderDist;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return super.getRenderBoundingBox();
        /*
        if (BlockDakimakura.isStanding(getBlockMetadata())) {
            return AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 2, zCoord + 1);
        }
        AxisAlignedBB[] rots = {
                AxisAlignedBB.getBoundingBox(0, 0, -1, 1, 0.28F, 1),
                AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 0.28F, 2),
                AxisAlignedBB.getBoundingBox(-1, 0, 0, 1, 0.28F, 1),
                AxisAlignedBB.getBoundingBox(0, 0, 0, 2, 0.28F, 1)
                };
        
        ForgeDirection rot = BlockDakimakura.getRotation(getBlockMetadata());
        return rots[(rot.ordinal() - 2) & 3].offset(xCoord, yCoord, zCoord);
        */
    }
    
    public void syncWithClients() {
        if (!worldObj.isRemote) {
            syncWithNearbyPlayers(this);
        }
    }
    
    public static void syncWithNearbyPlayers(TileEntity tileEntity) {
        World world = tileEntity.getWorld();
        List<EntityPlayer> players = world.playerEntities;
        for (EntityPlayer player : players) {
            if (player instanceof EntityPlayerMP) {
                EntityPlayerMP mp = (EntityPlayerMP)player;
                if (tileEntity.getDistanceSq(mp.posX, mp.posY, mp.posZ) < 64) {
                    mp.connection.sendPacket(tileEntity.getUpdatePacket());
                }
            }
        }
    }
}
