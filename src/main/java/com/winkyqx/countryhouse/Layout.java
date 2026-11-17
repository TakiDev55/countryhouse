package com.winkyqx.countryhouse;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Shared coordinates (local frame: x = right, z = forward, y = up, 0 = ground level). */
public final class Layout {
	private Layout() {}

	// ---- mansion: x 0..42, z 0..12 (+ front hall z -2..-1), floors at y=1 and y=7
	public static final int HOUSE_X2 = 42;

	// ---- input chest on the ground floor (feeds the sorter through a hopper shaft)
	public static final int IN_X = 2;
	public static final int IN_Z = 4;

	// ---- redstone sorter (basement)
	public static final int SORT_Z = 4;      // pipe / filter hopper line
	public static final int SORT_X0 = 6;     // first filter column
	public static final int PITCH = 3;
	public static final int PIPE_Y = -2;
	public static final int F_Y = -3;        // filter hoppers
	public static final int O_Y = -4;        // output hoppers
	public static final int CHEST_Y = -5;    // resource chests

	public static final Item[] FILTERS = {
			Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.COAL,
			Items.DIAMOND, Items.EMERALD, Items.REDSTONE, Items.LAPIS_LAZULI,
			Items.RAW_IRON, Items.COBBLESTONE, Items.WHEAT, Items.CARROT };

	public static int tileX(int i) {
		return SORT_X0 + PITCH * i;
	}

	public static int pipeEndX() {
		return tileX(FILTERS.length - 1) + 2;
	}

	// ---- underground farms (floor block y=-27, air y=-26..-23, ceiling y=-22)
	public static final int FARM_Y = -26;  // first air layer
	public static final int PAD_X = 19;    // XP pad in the farm corridor
	public static final int PAD_Z = 3;
}
