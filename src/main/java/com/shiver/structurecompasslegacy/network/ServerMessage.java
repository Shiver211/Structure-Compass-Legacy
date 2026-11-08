package com.shiver.structurecompasslegacy.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public final class ServerMessage implements IMessage {

    public NBTTagCompound data;

    public ServerMessage() {
    }

    public ServerMessage(NBTTagCompound data) {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        data = ByteBufUtils.readTag(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeTag(buffer, data);
    }
}
