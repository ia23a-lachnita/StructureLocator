package com.chaosthedude.explorerscompass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chaosthedude.explorerscompass.network.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.chaosthedude.explorerscompass.client.ClientEventHandler;
import com.chaosthedude.explorerscompass.config.ConfigHandler;
import com.chaosthedude.explorerscompass.gui.GuiWrapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(ExplorersCompass.MODID)
public class ExplorersCompass {

	public static final String MODID = "explorerscompass";

	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static SimpleChannel network;

	public static boolean canTeleport;
	public static List<ResourceLocation> allowedStructureKeys;
	public static ListMultimap<ResourceLocation, ResourceLocation> dimensionKeysForAllowedStructureKeys;
	public static Map<ResourceLocation, ResourceLocation> structureKeysToTypeKeys;
	public static ListMultimap<ResourceLocation, ResourceLocation> typeKeysToStructureKeys;

	@OnlyIn(Dist.CLIENT)
	public static KeyMapping structureFinderKey;

	public ExplorersCompass() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeyBindings);
		});

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.GENERAL_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigHandler.CLIENT_SPEC);

		MinecraftForge.EVENT_BUS.register(this);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		network = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> "1.0", s -> true, s -> true);

		// Server packets
		network.registerMessage(0, CompassSearchPacket.class, CompassSearchPacket::toBytes, CompassSearchPacket::new, CompassSearchPacket::handle);
		network.registerMessage(1, TeleportPacket.class, TeleportPacket::toBytes, TeleportPacket::new, TeleportPacket::handle);
		network.registerMessage(2, SyncRequestPacket.class, SyncRequestPacket::toBytes, SyncRequestPacket::new, SyncRequestPacket::handle);

		// Client packets
		network.registerMessage(3, SyncPacket.class, SyncPacket::toBytes, SyncPacket::new, SyncPacket::handle);
		network.registerMessage(4, StructureFoundPacket.class, StructureFoundPacket::toBytes, StructureFoundPacket::new, StructureFoundPacket::handle);
		network.registerMessage(5, StructureNotFoundPacket.class, StructureNotFoundPacket::toBytes, StructureNotFoundPacket::new, StructureNotFoundPacket::handle);
		network.registerMessage(6, StopSearchesPacket.class, StopSearchesPacket::toBytes, StopSearchesPacket::new, StopSearchesPacket::handle);


		allowedStructureKeys = new ArrayList<ResourceLocation>();
		dimensionKeysForAllowedStructureKeys = ArrayListMultimap.create();
		structureKeysToTypeKeys = new HashMap<ResourceLocation, ResourceLocation>();
		typeKeysToStructureKeys = ArrayListMultimap.create();
	}

	@OnlyIn(Dist.CLIENT)
	public void registerKeyBindings(RegisterKeyMappingsEvent event) {
		structureFinderKey = new KeyMapping("key.explorerscompass.structurefinder", GLFW.GLFW_KEY_J, "key.categories.explorerscompass");
		event.register(structureFinderKey);
	}

	@OnlyIn(Dist.CLIENT)
	public void clientSetup(FMLClientSetupEvent event) {
		MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		Player player = Minecraft.getInstance().player;
		if (player != null && structureFinderKey != null && structureFinderKey.consumeClick()) {
			GuiWrapper.openStructureFinderUI(Minecraft.getInstance().level, player);
		}
	}
}