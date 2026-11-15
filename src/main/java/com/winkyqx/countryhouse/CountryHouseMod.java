package com.winkyqx.countryhouse;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountryHouseMod implements ModInitializer {
	public static final String MOD_ID = "countryhouse";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Item HOUSE_KIT;

	@Override
	public void onInitialize() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("house_kit"));
		HOUSE_KIT = Registry.register(BuiltInRegistries.ITEM, key,
				new HouseKitItem(new Item.Properties().setId(key).stacksTo(1)));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.accept(HOUSE_KIT));

		ServerLifecycleEvents.SERVER_STARTED.register(HouseManager::load);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> HouseManager.clear());
		ServerTickEvents.END_SERVER_TICK.register(HouseManager::tick);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
