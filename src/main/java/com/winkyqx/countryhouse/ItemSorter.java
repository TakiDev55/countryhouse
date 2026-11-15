package com.winkyqx.countryhouse;

import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Decides which chest an item goes to. Edit JUNK to change what gets "cleaned". */
public final class ItemSorter {
	private ItemSorter() {}

	public static final Set<Item> JUNK = Set.of(
			Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
			Items.GRAVEL, Items.DIRT, Items.ANDESITE, Items.DIORITE, Items.GRANITE,
			Items.COBBLED_DEEPSLATE, Items.TUFF, Items.NETHERRACK);

	private static final Set<String> ORE_NAMES = Set.of(
			"coal", "charcoal", "diamond", "emerald", "lapis_lazuli", "redstone", "quartz",
			"amethyst_shard", "netherite_scrap",
			"coal_block", "iron_block", "gold_block", "copper_block", "diamond_block",
			"emerald_block", "lapis_block", "redstone_block", "netherite_block");

	public static Zone classify(ItemStack stack) {
		Item item = stack.getItem();
		if (JUNK.contains(item)) {
			return Zone.JUNK;
		}
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		if (path.endsWith("_ore") || path.startsWith("raw_") || path.endsWith("_ingot")
				|| path.endsWith("_nugget") || ORE_NAMES.contains(path)) {
			return Zone.ORES;
		}
		if (stack.has(DataComponents.FOOD)) {
			return Zone.FOOD;
		}
		if (stack.isDamageableItem()) {
			return Zone.GEAR;
		}
		if (item instanceof BlockItem) {
			return Zone.BLOCKS;
		}
		return Zone.MISC;
	}
}
