package com.chaosthedude.explorerscompass.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {

	private static final Minecraft mc = Minecraft.getInstance();

	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event) {
		// This method is now empty as we no longer need to render HUD info
		// for the compass item. UI information is displayed in the StructureFinderScreen.
	}

}