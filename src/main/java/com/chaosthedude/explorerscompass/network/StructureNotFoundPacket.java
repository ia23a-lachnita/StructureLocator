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
            if (Minecraft.getInstance().screen instanceof StructureFinderScreen) {
                StructureFinderScreen screen = (StructureFinderScreen) Minecraft.getInstance().screen;
                screen.finishSearch(structureKey); // Mark search as complete

                // Add a log message
                ExplorersCompass.LOGGER.info("Could not find structure: " + structureKey);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleOnClient() {
        if (Minecraft.getInstance().screen instanceof StructureFinderScreen) {
            // Update the UI to show this structure as not found
            // We don't need to add it to search results as null; the UI already handles displaying
            // structures with no search results as "Not found"
        }
    }
}