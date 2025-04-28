package com.chaosthedude.explorerscompass.network;

import java.util.function.Supplier;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.config.ConfigHandler;
import com.chaosthedude.explorerscompass.gui.StructureFinderScreen.SearchResult;
import com.chaosthedude.explorerscompass.util.PlayerUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class TeleportPacket {

	private int x;
	private int z;

	public TeleportPacket() {}

	public TeleportPacket(SearchResult result) {
		this.x = result.getX();
		this.z = result.getZ();
	}

	public TeleportPacket(FriendlyByteBuf buf) {
		x = buf.readInt();
		z = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(z);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			final ServerPlayer player = ctx.get().getSender();
			if (ConfigHandler.GENERAL.allowTeleport.get() && PlayerUtils.canTeleport(player.getServer(), player)) {
				final int y = findValidTeleportHeight(player.level, x, z);

				player.stopRiding();
				player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());

				if (!player.isFallFlying()) {
					player.setDeltaMovement(player.getDeltaMovement().x(), 0, player.getDeltaMovement().z());
					player.setOnGround(true);
				}
			} else {
				ExplorersCompass.LOGGER.warn("Player " + player.getDisplayName().getString() + " tried to teleport but does not have permission.");
			}
		});
		ctx.get().setPacketHandled(true);
	}

	private int findValidTeleportHeight(Level level, int x, int z) {
		int upY = level.getSeaLevel();
		int downY = level.getSeaLevel();
		while (!(isValidTeleportPosition(level, new BlockPos(x, upY, z)) || isValidTeleportPosition(level, new BlockPos(x, downY, z)))) {
			upY++;
			downY--;
		}
		BlockPos upPos = new BlockPos(x, upY, z);
		BlockPos downPos = new BlockPos(x, downY, z);
		if (isValidTeleportPosition(level, upPos)) {
			return upY;
		}
		if (isValidTeleportPosition(level, downPos)) {
			return downY;
		}
		return 256;
	}

	private boolean isValidTeleportPosition(Level level, BlockPos pos) {
		return !level.isOutsideBuildHeight(pos) && isFree(level, pos) && isFree(level, pos.above()) && !isFree(level, pos.below());
	}

	private boolean isFree(Level level, BlockPos pos) {
		return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(BlockTags.FIRE) || level.getBlockState(pos).getMaterial().isLiquid() || level.getBlockState(pos).getMaterial().isReplaceable();
	}
}