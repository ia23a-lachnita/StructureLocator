package com.chaosthedude.explorerscompass.gui;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.network.SyncPacket;

import com.chaosthedude.explorerscompass.network.SyncRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GuiWrapper {

	public static void openStructureFinderUI(Level level, Player player) {
		// If we already have structures, show UI immediately
		if (!ExplorersCompass.allowedStructureKeys.isEmpty()) {
			Minecraft.getInstance().setScreen(new StructureFinderScreen(level, player, ExplorersCompass.allowedStructureKeys));
			return;
		}

		// Otherwise, show loading screen and request data
		Minecraft.getInstance().setScreen(new LoadingScreen(() -> {
			ExplorersCompass.network.sendToServer(new SyncRequestPacket());
		}));
	}
}