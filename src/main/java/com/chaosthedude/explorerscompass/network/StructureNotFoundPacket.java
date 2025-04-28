package com.chaosthedude.explorerscompass.network;

import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.gui.StructureFinderScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class StructureNotFoundPacket {

    private ResourceLocation structureKey;

    public StructureNotFoundPacket() {}

    public StructureNotFoundPacket(ResourceLocation structureKey) {
        this.structureKey = structureKey;
    }

    public StructureNotFoundPacket(FriendlyByteBuf buf) {
        structureKey = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(structureKey);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ExplorersCompass.LOGGER.info("Received StructureNotFoundPacket for " + structureKey);

            if (Minecraft.getInstance().screen instanceof StructureFinderScreen) {
                StructureFinderScreen screen = (StructureFinderScreen) Minecraft.getInstance().screen;
                ExplorersCompass.LOGGER.info("Updating UI to mark structure as not found: " + structureKey);
                screen.finishSearch(structureKey); // Mark search as complete
            } else {
                ExplorersCompass.LOGGER.warn("Structure not found packet received but StructureFinderScreen is not open");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}