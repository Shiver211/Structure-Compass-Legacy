package com.shiver.structurecompasslegacy.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public final class ClientRequest implements IMessage {

    public static final int SEARCH = 0;
    public static final int CANCEL = 1;
    public static final int CLEAR_TARGET = 2;

    public int action;
    public int hand;
    public long requestId;
    public String structureId = "";

    public ClientRequest() {
    }

    public ClientRequest(int action, int hand, long requestId, String structureId) {
        this.action = action;
        this.hand = hand;
        this.requestId = requestId;
        this.structureId = structureId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        action = buffer.readUnsignedByte();
        hand = buffer.readUnsignedByte();
        requestId = buffer.readLong();
        structureId = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeByte(action);
        buffer.writeByte(hand);
        buffer.writeLong(requestId);
        ByteBufUtils.writeUTF8String(buffer, structureId);
    }
}
