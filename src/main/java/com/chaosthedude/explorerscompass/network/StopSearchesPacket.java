package com.chaosthedude.explorerscompass.network;

import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.chaosthedude.explorerscompass.worker.SearchWorkerManager;

public class StopSearchesPacket {

    public StopSearchesPacket() {}

    public StopSearchesPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            // Get player UUID and stop any active searches for this player
            ExplorersCompass.LOGGER.info("Stopping all searches for player: " + player.getName().getString());

            // Stop search worker manager
            // You'll need to implement this in your SearchWorkerManager
            SearchWorkerManager.stopAllForPlayer(player);
        });
        ctx.get().setPacketHandled(true);
    }
}