package com.winkyqx.countryhouse;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;

/** Sealed underground farm complex ~26 blocks below ground. See HouseManager for the helper logic. */
final class FarmsBuilder {
	private static final Direction S = Direction.SOUTH;
	private static final Direction W = Direction.WEST;

	private final Canvas c;

	FarmsBuilder(Canvas canvas) {
		this.c = canvas;
	}

	void build() {
		ladderShaft();
		corridor();
		ironFarm(1, -18, 9);
		crops();
		animals();
		xpFarm();
	}

	private void ladderShaft() {
		c.fill(0, -25, 1, 2, -7, 3, Blocks.STONE_BRICKS);
		c.clear(1, -25, 2, 1, -6, 2);
		for (int y = -26; y <= -6; y++) {
			c.set(1, y, 2, Canvas.facing(Blocks.LADDER, S));
		}
	}

	private void corridor() {
		c.fill(-1, -28, 1, 47, -22, 5, Blocks.STONE_BRICKS);
		c.clear(0, -26, 2, 46, -23, 4);
		// (re)place the ladder column after the corridor was carved
		for (int y = -26; y <= -6; y++) {
			c.set(1, y, 2, Canvas.facing(Blocks.LADDER, S));
		}
		for (int x = 6; x <= 42; x += 6) {
			c.set(x, -22, 3, Blocks.SEA_LANTERN);
		}
		c.set(Layout.PAD_X, -27, Layout.PAD_Z, Blocks.GOLD_BLOCK); // XP pad
	}

	/** Villager cell + caged zombie + spawn platform. Golems are killed / looted by HouseManager. */
	private void ironFarm(int ox, int oy, int oz) {
		c.fill(2 + ox, -10 + oy, 2 + oz, 12 + ox, -5 + oy, 12 + oz, Blocks.STONE_BRICKS);
		c.clear(3 + ox, -8 + oy, 3 + oz, 11 + ox, -6 + oy, 11 + oz);
		c.fill(5 + ox, -8 + oy, 5 + oz, 10 + ox, -6 + oy, 9 + oz, Blocks.STONE_BRICKS);
		c.clear(6 + ox, -8 + oy, 6 + oz, 7 + ox, -7 + oy, 8 + oz);
		c.clear(9 + ox, -8 + oy, 7 + oz, 9 + ox, -7 + oy, 7 + oz);
		c.set(8 + ox, -8 + oy, 7 + oz, Blocks.GLASS);
		c.set(8 + ox, -7 + oy, 7 + oz, Blocks.GLASS);
		c.set(6 + ox, -6 + oy, 7 + oz, Blocks.SEA_LANTERN);
		c.set(9 + ox, -6 + oy, 7 + oz, Blocks.SEA_LANTERN);
		for (int[] p : new int[][] { { 4, 4 }, { 4, 10 }, { 10, 4 }, { 10, 10 }, { 7, 3 }, { 7, 11 }, { 3, 7 }, { 11, 7 } }) {
			c.set(p[0] + ox, -5 + oy, p[1] + oz, Blocks.SEA_LANTERN);
		}
		for (int z = 6; z <= 8; z++) {
			c.spawn(EntityTypes.VILLAGER, 6 + ox, -7 + oy, z + oz);
		}
		Mob zombie = EntityTypes.ZOMBIE.spawn(c.level, c.f.at(9 + ox, -8 + oy, 7 + oz), EntitySpawnReason.MOB_SUMMONED);
		if (zombie != null) {
			zombie.setNoAi(true);
			zombie.setSilent(true);
			zombie.setPersistenceRequired();
		}
		for (int z = 6; z <= 8; z++) {
			c.bed(6 + ox, -8 + oy, z + oz, W, DyeColor.RED);
		}
	}

	private void crops() {
		c.fill(17, -28, 5, 31, -22, 17, Blocks.STONE_BRICKS);
		c.clear(18, -26, 6, 30, -23, 16);
		c.clear(24, -26, 5, 24, -25, 5);
		c.door(24, -26, 5, Direction.NORTH, Blocks.SPRUCE_DOOR);
		for (int x : new int[] { 20, 24, 28 }) {
			for (int z : new int[] { 8, 12, 15 }) {
				c.set(x, -22, z, Blocks.SEA_LANTERN);
			}
		}
		for (int x = 18; x <= 30; x++) {
			c.set(x, -26, 6, Blocks.DIRT_PATH);
			c.set(x, -26, 16, Blocks.DIRT_PATH);
			c.set(x, -26, 11, Blocks.WATER);
			for (int z : new int[] { 7, 8, 9, 10, 12, 13, 14, 15 }) {
				c.set(x, -26, z, Blocks.FARMLAND);
				var crop = (z == 9 || z == 10) ? Blocks.CARROTS : (z == 12 || z == 13) ? Blocks.POTATOES : Blocks.WHEAT;
				c.set(x, -25, z, crop);
			}
		}
	}

	private void animals() {
		c.fill(32, -28, 5, 46, -22, 17, Blocks.STONE_BRICKS);
		c.clear(33, -26, 6, 45, -23, 16);
		c.clear(39, -26, 5, 39, -25, 5);
		c.door(39, -26, 5, Direction.NORTH, Blocks.SPRUCE_DOOR);
		for (int x : new int[] { 35, 39, 43 }) {
			for (int z : new int[] { 8, 12, 15 }) {
				c.set(x, -22, z, Blocks.SEA_LANTERN);
			}
		}
		c.fill(33, -27, 6, 45, -27, 16, Blocks.GRASS_BLOCK);
		c.spawn(EntityTypes.COW, 36, -26, 9);
		c.spawn(EntityTypes.COW, 40, -26, 12);
		c.spawn(EntityTypes.SHEEP, 36, -26, 13);
		c.spawn(EntityTypes.SHEEP, 42, -26, 10);
		c.spawn(EntityTypes.CHICKEN, 38, -26, 8);
		c.spawn(EntityTypes.CHICKEN, 43, -26, 14);
	}

	/** Dark spawning hall for hostile mobs; sealed, with a small window to the corridor. */
	private void xpFarm() {
		c.fill(5, -28, -13, 37, -22, 1, Blocks.STONE_BRICKS);
		c.clear(6, -26, -12, 36, -23, 0);
		c.fill(18, -25, 1, 20, -24, 1, Blocks.GLASS_PANE);
	}
}
