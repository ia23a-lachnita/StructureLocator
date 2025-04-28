package com.chaosthedude.explorerscompass.network;

import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.gui.StructureFinderScreen;
import com.chaosthedude.explorerscompass.util.StructureDataManager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class StructureFoundPacket {

    private ResourceLocation structureKey;
    private int x;
    private int z;

    public StructureFoundPacket() {}

    public StructureFoundPacket(ResourceLocation structureKey, int x, int z) {
        this.structureKey = structureKey;
        this.x = x;
        this.z = z;
    }

    public StructureFoundPacket(FriendlyByteBuf buf) {
        structureKey = buf.readResourceLocation();
        x = buf.readInt();
        z = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(structureKey);
        buf.writeInt(x);
        buf.writeInt(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This will be executed on the client
            ExplorersCompass.LOGGER.info("Received StructureFoundPacket for " + structureKey + " at X: " + x + ", Z: " + z);

            // Always save found structures to persistent storage
            StructureDataManager.init(); // Make sure manager is initialized
            StructureDataManager.addFoundStructure(structureKey, x, z);

            // Also update UI if it's open
            if (Minecraft.getInstance().screen instanceof StructureFinderScreen) {
                StructureFinderScreen screen = (StructureFinderScreen) Minecraft.getInstance().screen;
                ExplorersCompass.LOGGER.info("Updating UI with found structure: " + structureKey);
                screen.addSearchResult(structureKey, x, z);
                screen.finishSearch(structureKey); // Mark search as complete
            } else {
                ExplorersCompass.LOGGER.info("Structure found but StructureFinderScreen is not open - saved to persistent storage");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}