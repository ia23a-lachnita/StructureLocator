package com.chaosthedude.explorerscompass.worker;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.config.ConfigHandler;
import com.chaosthedude.explorerscompass.util.StructureUtils;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.common.WorldWorkerManager;

public abstract class StructureSearchWorker<T extends StructurePlacement> implements WorldWorkerManager.IWorker {

	protected String managerId;
	protected ServerLevel level;
	protected Player player;
	protected BlockPos startPos;
	protected BlockPos currentPos;
	protected T placement;
	protected List<Structure> structureSet;
	protected int samples;
	protected boolean finished;
	protected int lastRadiusThreshold;
	protected BiConsumer<ResourceLocation, Pair<Integer, Integer>> onSuccess;
	protected Consumer<ResourceLocation> onFailure;

	public StructureSearchWorker(ServerLevel level, Player player, BlockPos startPos, T placement, List<Structure> structureSet, String managerId,
								 BiConsumer<ResourceLocation, Pair<Integer, Integer>> onSuccess, Consumer<ResourceLocation> onFailure) {
		this.level = level;
		this.player = player;
		this.startPos = startPos;
		this.structureSet = structureSet;
		this.placement = placement;
		this.managerId = managerId;
		this.onSuccess = onSuccess;
		this.onFailure = onFailure;

		currentPos = startPos;
		samples = 0;

		finished = !level.getServer().getWorldData().worldGenSettings().generateStructures();
	}

	public void start() {
		if (ConfigHandler.GENERAL.maxRadius.get() > 0) {
			ExplorersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " starting with " + (shouldLogRadius() ? ConfigHandler.GENERAL.maxRadius.get() + " max radius, " : "") + ConfigHandler.GENERAL.maxSamples.get() + " max samples");
			WorldWorkerManager.addWorker(this);
		} else {
			fail();
		}
	}

	@Override
	public boolean hasWork() {
		return !finished && getRadius() < ConfigHandler.GENERAL.maxRadius.get() && samples < ConfigHandler.GENERAL.maxSamples.get();
	}

	@Override
	public boolean doWork() {
		int radius = getRadius();
		if (radius > 250 && radius / 250 > lastRadiusThreshold) {
			lastRadiusThreshold = radius / 250;
		}
		return false;
	}

	protected Pair<BlockPos, Structure> getStructureGeneratingAt(ChunkPos chunkPos) {
		for (Structure structure : structureSet) {
			StructureCheckResult result = level.structureManager().checkStructurePresence(chunkPos, structure, false);
			if (result != StructureCheckResult.START_NOT_PRESENT) {
				if (result == StructureCheckResult.START_PRESENT) {
					return Pair.of(placement.getLocatePos(chunkPos), structure);
				}

				ChunkAccess chunkAccess = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
				StructureStart structureStart = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunkAccess), structure, chunkAccess);
				if (structureStart != null && structureStart.isValid()) {
					return Pair.of(placement.getLocatePos(structureStart.getChunkPos()), structure);
				}
			}
		}

		return null;
	}

	protected void succeed(BlockPos pos, Structure structure) {
		ExplorersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " succeeded with " + (shouldLogRadius() ? getRadius() + " radius, " : "") + samples + " samples");
		onSuccess.accept(StructureUtils.getKeyForStructure(level, structure), Pair.of(pos.getX(), pos.getZ()));
		finished = true;
	}

	protected void fail() {
		ExplorersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " failed with " + (shouldLogRadius() ? getRadius() + " radius, " : "") + samples + " samples");
		for (Structure structure : structureSet) {
			onFailure.accept(StructureUtils.getKeyForStructure(level, structure));
		}
		finished = true;
	}

	public void stop() {
		ExplorersCompass.LOGGER.info("SearchWorkerManager " + managerId + ": " + getName() + " stopped with " + (shouldLogRadius() ? getRadius() + " radius, " : "") + samples + " samples");
		finished = true;
	}

	protected int getRadius() {
		return StructureUtils.getHorizontalDistanceToLocation(startPos, currentPos.getX(), currentPos.getZ());
	}

	protected int roundRadius(int radius, int roundTo) {
		return ((int) radius / roundTo) * roundTo;
	}

	protected abstract String getName();

	protected abstract boolean shouldLogRadius();
}