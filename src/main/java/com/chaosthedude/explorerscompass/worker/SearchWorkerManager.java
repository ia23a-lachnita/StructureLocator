package com.chaosthedude.explorerscompass.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang3.RandomStringUtils;
import com.mojang.datafixers.util.Pair;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.util.StructureUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class SearchWorkerManager {

	// Add a static map to track worker managers by player UUID
	private static Map<UUID, List<SearchWorkerManager>> playerWorkers = new HashMap<>();

	private final String id = RandomStringUtils.random(8, "0123456789abcdef");

	private List<StructureSearchWorker<?>> workers;
	// Add cleanup method and boolean for tracking if finished
	private boolean finished = false;

	public SearchWorkerManager() {
		workers = new ArrayList<StructureSearchWorker<?>>();
	}

	// Modify constructor
	public SearchWorkerManager(Player player) {
		this();
		// Add this worker to the player's list
		playerWorkers.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(this);
	}

	public void createWorkers(ServerLevel level, Player player, List<Structure> structures, BlockPos startPos,
							  BiConsumer<ResourceLocation, Pair<Integer, Integer>> onSuccess, Consumer<ResourceLocation> onFailure) {
		workers.clear();

		Map<StructurePlacement, List<Structure>> placementToStructuresMap = new Object2ObjectArrayMap<>();

		for (Structure structure : structures) {
			for (StructurePlacement structureplacement : level.getChunkSource().getGenerator().getPlacementsForStructure(StructureUtils.getHolderForStructure(level, structure), level.getChunkSource().randomState())) {
				placementToStructuresMap.computeIfAbsent(structureplacement, (holderSet) -> {
					return new ObjectArrayList<Structure>();
				}).add(structure);
			}
		}

		for (Map.Entry<StructurePlacement, List<Structure>> entry : placementToStructuresMap.entrySet()) {
			StructurePlacement placement = entry.getKey();
			if (placement instanceof ConcentricRingsStructurePlacement) {
				workers.add(new ConcentricRingsSearchWorker(level, player, startPos, (ConcentricRingsStructurePlacement) placement, entry.getValue(), id, onSuccess, onFailure));
			} else if (placement instanceof RandomSpreadStructurePlacement) {
				workers.add(new RandomSpreadSearchWorker(level, player, startPos, (RandomSpreadStructurePlacement) placement, entry.getValue(), id, onSuccess, onFailure));
			} else {
				workers.add(new GenericSearchWorker(level, player, startPos, placement, entry.getValue(), id, onSuccess, onFailure));
			}
		}
	}

	// Returns true if a worker starts, false otherwise
	public boolean start() {
		if (!workers.isEmpty()) {
			workers.get(0).start();
			return true;
		}
		return false;
	}

	public void pop() {
		if (!workers.isEmpty()) {
			workers.remove(0);
		}
	}

	// Update stop method
	public void stop() {
		for (StructureSearchWorker<?> worker : workers) {
			worker.stop();
		}
		finished = true;
	}

	// Add static method
	public static void stopAllForPlayer(Player player) {
		List<SearchWorkerManager> managers = playerWorkers.get(player.getUUID());
		if (managers != null) {
			for (SearchWorkerManager manager : managers) {
				manager.stop();
			}
			managers.clear();
		}
	}

	public void clear() {
		workers.clear();
	}

	// Clean up method that should be called when work completes
	public void cleanup(Player player) {
		if (finished && player != null) {
			List<SearchWorkerManager> managers = playerWorkers.get(player.getUUID());
			if (managers != null) {
				managers.remove(this);
			}
		}
	}
}