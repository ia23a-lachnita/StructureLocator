package com.chaosthedude.explorerscompass.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.config.ConfigHandler;
import com.chaosthedude.explorerscompass.util.StructureUtils;
import com.chaosthedude.explorerscompass.worker.SearchWorkerManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class CompassSearchPacket {

	private ResourceLocation groupKey;
	private List<ResourceLocation> structureKeys;
	private int x;
	private int y;
	private int z;

	// Remove the workerManager field - create it in the handle method instead

	public CompassSearchPacket() {
		// Empty constructor - initialize fields in handle method
	}

	public CompassSearchPacket(ResourceLocation groupKey, List<ResourceLocation> structureKeys, BlockPos pos) {
		this.groupKey = groupKey;
		this.structureKeys = structureKeys;
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public CompassSearchPacket(FriendlyByteBuf buf) {
		groupKey = buf.readResourceLocation();

		structureKeys = new ArrayList<ResourceLocation>();
		int numStructures = buf.readInt();
		for (int i = 0; i < numStructures; i++) {
			structureKeys.add(buf.readResourceLocation());
		}

		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeResourceLocation(groupKey);

		buf.writeInt(structureKeys.size());
		for (ResourceLocation key : structureKeys) {
			buf.writeResourceLocation(key);
		}

		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerLevel level = ctx.get().getSender().getLevel();
			BlockPos pos = new BlockPos(x, y, z);

			List<Structure> structures = new ArrayList<Structure>();
			for (ResourceLocation key : structureKeys) {
				structures.add(StructureUtils.getStructureForKey(level, key));
			}

			// Create the worker manager here with the context
			SearchWorkerManager workerManager = new SearchWorkerManager(ctx.get().getSender());

			workerManager.stop();
			workerManager.createWorkers(level, ctx.get().getSender(), structures, pos,
					(resourceLocation, coordinates) -> {
						int foundX = coordinates.getFirst();
						int foundZ = coordinates.getSecond();
						ExplorersCompass.network.sendTo(
								new StructureFoundPacket(resourceLocation, foundX, foundZ),
								ctx.get().getSender().connection.getConnection(),
								NetworkDirection.PLAY_TO_CLIENT
						);
					},
					(resourceLocation) -> {
						ExplorersCompass.network.sendTo(
								new StructureNotFoundPacket(resourceLocation),
								ctx.get().getSender().connection.getConnection(),
								NetworkDirection.PLAY_TO_CLIENT
						);
					}
			);

			workerManager.start();
		});
		ctx.get().setPacketHandled(true);
	}
}