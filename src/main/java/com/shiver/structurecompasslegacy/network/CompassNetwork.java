package com.shiver.structurecompasslegacy.network;

import com.shiver.structurecompasslegacy.StructureCompassLegacy;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.item.CompassTarget;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class CompassNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("structurecompass");

    private CompassNetwork() {
    }

    public static void initialize() {
        CHANNEL.registerMessage(RequestHandler.class, ClientRequest.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ResponseHandler.class, ServerMessage.class, 1, Side.CLIENT);
    }

    public static void openScreen(EntityPlayerMP player, EnumHand hand) {
        NBTTagCompound message = new NBTTagCompound();
        message.setString("type", "catalog");
        message.setInteger("hand", hand.ordinal());
        NBTTagList structures = new NBTTagList();

        for (StructureDefinition structure : StructureCompassLegacy.STRUCTURES.entries()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("id", structure.id.toString());
            entry.setString("english", structure.englishName);
            entry.setString("chinese", structure.chineseName);
            entry.setString("mod", structure.modName);
            entry.setString("method", structure.method.translationKey());
            entry.setBoolean("available", structure.supports(player.getServerWorld()));
            entry.setBoolean("experimental", structure.experimental);
            entry.setString("limitation", structure.limitation);
            entry.setString("unavailableReason", structure.unavailableReason(player.getServerWorld()));
            structures.appendTag(entry);
        }

        message.setTag("structures", structures);
        send(player, message);
    }

    public static void send(EntityPlayerMP player, NBTTagCompound data) {
        CHANNEL.sendTo(new ServerMessage(data), player);
    }

    public static final class RequestHandler implements IMessageHandler<ClientRequest, IMessage> {

        @Override
        public IMessage onMessage(ClientRequest request, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> handle(player, request));
            return null;
        }

        private void handle(EntityPlayerMP player, ClientRequest request) {
            if (request.hand < 0 || request.hand >= EnumHand.values().length) {
                return;
            }

            EnumHand hand = EnumHand.values()[request.hand];
            if (player.getHeldItem(hand).getItem() != StructureCompassLegacy.COMPASS) {
                return;
            }

            if (request.action == ClientRequest.SEARCH) {
                StructureCompassLegacy.SEARCHES.submit(player, hand, request.requestId, request.structureId);
            } else if (request.action == ClientRequest.CANCEL || request.action == ClientRequest.CLEAR_TARGET) {
                StructureCompassLegacy.SEARCHES.cancel(player.getUniqueID());
                if (request.action == ClientRequest.CLEAR_TARGET) {
                    CompassTarget.clear(player.getHeldItem(hand));
                    player.inventory.markDirty();
                }
            }
        }
    }

    public static final class ResponseHandler implements IMessageHandler<ServerMessage, IMessage> {

        @Override
        public IMessage onMessage(ServerMessage message, MessageContext context) {
            StructureCompassLegacy.proxy.receiveClientMessage(message.data);
            return null;
        }
    }
}
