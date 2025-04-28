package com.chaosthedude.explorerscompass.network;

import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;

import com.chaosthedude.explorerscompass.config.ConfigHandler;
import com.chaosthedude.explorerscompass.util.PlayerUtils;
import com.chaosthedude.explorerscompass.util.StructureUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class SyncRequestPacket {

    public SyncRequestPacket() {}

    public SyncRequestPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            ServerLevel level = player.getLevel();

            // Get whether player can teleport
            boolean canTeleport =
                    ConfigHandler.GENERAL.allowTeleport.get() &&
                            PlayerUtils.canTeleport(player.getServer(), player);

            // Send the structure data to the client
            ExplorersCompass.network.sendTo(
                    new SyncPacket(canTeleport,
                            StructureUtils.getAllowedStructureKeys(level),
                            StructureUtils.getGeneratingDimensionsForAllowedStructures(level),
                            StructureUtils.getStructureKeysToTypeKeys(level),
                            StructureUtils.getTypeKeysToStructureKeys(level)),
                    player.connection.getConnection(),
                    NetworkDirection.PLAY_TO_CLIENT);
        });
        ctx.get().setPacketHandled(true);
    }
}