package com.winkyqx.countryhouse;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Two-storey rustic mansion: log frame, plank walls, balconies on both wings, tall front hall with a purple window,
 * cross-gable roof covered in mossy cobblestone.
 *
 * Footprint: x 0..42, z 0..12 (left wing 0..14, hall 14..28, right wing 28..42), front hall z -3..-1.
 * Ground floor block y=1 (stand y=2), upper floor block y=7 (stand y=8), ceiling y=12, roof from y=13.
 */
final class MansionBuilder {
	private static final Direction N = Direction.NORTH;
	private static final Direction S = Direction.SOUTH;
	private static final Direction E = Direction.EAST;
	private static final Direction W = Direction.WEST;

	private final Canvas c;

	MansionBuilder(Canvas canvas) {
		this.c = canvas;
	}

	void build() {
		foundation();
		shell();
		frontHall();
		roofs();
		groundFloor();
		upperFloor();
		balconies();
		exterior();
	}

	// ------------------------------------------------------------------ structure

	private void foundation() {
		for (int x = 0; x <= 42; x++) {
			for (int z = 0; z <= 12; z++) {
				c.set(x, 0, z, Blocks.COBBLESTONE);
				c.fillBelow(x, z, 3);
			}
		}
		for (int x = 14; x <= 28; x++) {
			for (int z = -3; z <= -1; z++) {
				c.set(x, 0, z, Blocks.COBBLESTONE);
				c.fillBelow(x, z, 3);
			}
		}
	}

	private void wall(int x, int z, Direction.Axis beam) {
		c.set(x, 2, z, Blocks.STONE_BRICKS);
		c.fill(x, 3, z, x, 6, z, Blocks.OAK_PLANKS);
		c.set(x, 7, z, Canvas.axis(Blocks.DARK_OAK_LOG, beam));
		c.fill(x, 8, z, x, 11, z, Blocks.OAK_PLANKS);
		c.set(x, 12, z, Canvas.axis(Blocks.DARK_OAK_LOG, beam));
	}

	private void post(int x, int z) {
		c.fill(x, 2, z, x, 12, z, Canvas.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
	}

	private void shell() {
		c.fill(0, 1, 0, 42, 1, 12, Blocks.COBBLESTONE);
		c.fill(1, 1, 1, 41, 1, 11, Blocks.SPRUCE_PLANKS);
		c.fill(1, 7, 1, 41, 7, 11, Blocks.SPRUCE_PLANKS);
		c.fill(1, 12, 1, 41, 12, 11, Blocks.SPRUCE_SLAB); // bottom slabs: no mob spawns in the attic

		for (int x = 0; x <= 42; x++) {
			wall(x, 0, Direction.Axis.X);
			wall(x, 12, Direction.Axis.X);
		}
		for (int z = 1; z <= 11; z++) {
			wall(0, z, Direction.Axis.Z);
			wall(42, z, Direction.Axis.Z);
		}
		for (int x : new int[] { 0, 7, 14, 21, 28, 35, 42 }) {
			post(x, 0);
			post(x, 12);
		}
		for (int z : new int[] { 0, 6, 12 }) {
			post(0, z);
			post(42, z);
		}

		// windows: ground (y 4..5) and upper (y 9..10)
		for (int x : new int[] { 3, 4, 10, 11, 31, 32, 38, 39 }) {
			c.fill(x, 4, 0, x, 5, 0, Blocks.GLASS_PANE);
			c.fill(x, 9, 0, x, 10, 0, Blocks.GLASS_PANE);
		}
		for (int x : new int[] { 3, 4, 10, 11, 17, 18, 24, 25, 31, 32, 38, 39 }) {
			c.fill(x, 4, 12, x, 5, 12, Blocks.GLASS_PANE);
			c.fill(x, 9, 12, x, 10, 12, Blocks.GLASS_PANE);
		}
		for (int z : new int[] { 3, 4, 8, 9 }) {
			for (int x : new int[] { 0, 42 }) {
				c.fill(x, 4, z, x, 5, z, Blocks.GLASS_PANE);
				c.fill(x, 9, z, x, 10, z, Blocks.GLASS_PANE);
			}
		}
		// shutters beside the front windows
		for (int[] pair : new int[][] { { 3, 4 }, { 10, 11 }, { 31, 32 }, { 38, 39 } }) {
			for (int y = 4; y <= 5; y++) {
				c.shutter(pair[0] - 1, y, -1, N, Blocks.SPRUCE_TRAPDOOR);
				c.shutter(pair[1] + 1, y, -1, N, Blocks.SPRUCE_TRAPDOOR);
			}
		}

		// side door to the garden + step
		c.door(42, 2, 3, W, Blocks.SPRUCE_DOOR);
		c.set(43, 1, 3, Blocks.COBBLESTONE_SLAB);
	}

	private void frontHall() {
		// open the main front wall between the wings
		c.clear(15, 2, 0, 27, 6, 0);
		c.clear(15, 8, 0, 27, 11, 0);

		c.fill(14, 1, -3, 28, 1, -1, Blocks.COBBLESTONE);
		c.fill(15, 1, -2, 27, 1, -1, Blocks.SPRUCE_PLANKS);
		c.fill(15, 7, -2, 27, 7, -1, Blocks.SPRUCE_PLANKS);
		c.fill(15, 12, -2, 27, 12, -1, Blocks.SPRUCE_SLAB);
		for (int x = 14; x <= 28; x++) {
			wall(x, -3, Direction.Axis.X);
		}
		for (int z = -2; z <= -1; z++) {
			wall(14, z, Direction.Axis.Z);
			wall(28, z, Direction.Axis.Z);
		}
		for (int x : new int[] { 14, 21, 28 }) {
			post(x, -3);
		}
		post(14, -1);
		post(28, -1);

		// entrance: double-width glass + door
		c.door(21, 2, -3, N, Blocks.SPRUCE_DOOR);
		for (int x : new int[] { 17, 18, 24, 25 }) {
			c.fill(x, 3, -3, x, 5, -3, Blocks.GLASS_PANE);
		}
		// the big purple window
		c.fill(17, 8, -3, 25, 11, -3, Blocks.PURPLE_STAINED_GLASS);
		c.fill(17, 8, -3, 25, 8, -3, Canvas.axis(Blocks.DARK_OAK_LOG, Direction.Axis.X));
		c.fill(21, 8, -3, 21, 11, -3, Canvas.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
		// steps and path
		for (int x = 18; x <= 24; x++) {
			c.set(x, 1, -4, Canvas.facing(Blocks.STONE_BRICK_STAIRS, S));
			c.set(x, 0, -5, Canvas.facing(Blocks.STONE_BRICK_STAIRS, S));
		}
		for (int z = -6; z >= -14; z--) {
			for (int x = 20; x <= 22; x++) {
				c.set(x, 0, z, Blocks.DIRT_PATH);
			}
		}
	}

	// ------------------------------------------------------------------ roofs

	private BlockState mossStairs(int x, int z, int k, Direction facing) {
		Block b = ((x * 7 + z * 13 + k * 3) % 5 == 0) ? Blocks.COBBLESTONE_STAIRS : Blocks.MOSSY_COBBLESTONE_STAIRS;
		return Canvas.facing(b, facing);
	}

	private void roofs() {
		// main roof along x over the 13-deep body (ridge at z=6, y=20)
		for (int k = 0; k <= 6; k++) {
			int y = 13 + k;
			for (int x = -1; x <= 43; x++) {
				c.set(x, y, -1 + k, mossStairs(x, -1 + k, k, S));
				c.set(x, y, 13 - k, mossStairs(x, 13 - k, k, N));
			}
			for (int x : new int[] { 0, 42 }) {
				c.fill(x, y, k, x, y, 12 - k, Blocks.OAK_PLANKS);
			}
		}
		for (int x = -1; x <= 43; x++) {
			c.set(x, 20, 6, Blocks.MOSSY_COBBLESTONE);
		}
		// taller cross gable over the front hall (ridge x=21, y=21)
		for (int z = -4; z <= 7; z++) {
			for (int k = 0; k <= 7; k++) {
				int y = 13 + k;
				c.set(13 + k, y, z, mossStairs(13 + k, z, k, W));
				c.set(29 - k, y, z, mossStairs(29 - k, z, k, E));
			}
			c.set(21, 21, z, Blocks.MOSSY_COBBLESTONE);
		}
		// front and back gable walls of the cross roof
		for (int k = 0; k <= 7; k++) {
			c.fill(14 + k, 13 + k, -3, 28 - k, 13 + k, -3, Blocks.OAK_PLANKS);
			c.fill(14 + k, 13 + k, 7, 28 - k, 13 + k, 7, Blocks.OAK_PLANKS);
		}
		// small round-ish window in the gable
		c.fill(20, 16, -3, 22, 17, -3, Blocks.GLASS_PANE);
	}

	// ------------------------------------------------------------------ interiors

	private void lantern(int x, int y, int z) {
		c.set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}

	private void groundFloor() {
		for (int x : new int[] { 4, 10, 18, 24, 32, 38 }) {
			for (int z : new int[] { 3, 9 }) {
				lantern(x, 6, z);
			}
		}
		lantern(21, 6, -2);

		// entrance hall: red carpet runner + stairs up
		c.fill(20, 2, -2, 22, 2, 11, Blocks.CARPET.pick(DyeColor.RED));
		c.clear(20, 7, 4, 22, 7, 8); // opening in the upper floor
		for (int k = 0; k <= 5; k++) {
			for (int x = 20; x <= 22; x++) {
				c.set(x, 2 + k, 4 + k, Canvas.facing(Blocks.SPRUCE_STAIRS, S));
			}
		}
		// hall furniture
		c.set(16, 2, 2, Blocks.BOOKSHELF);
		c.set(16, 3, 2, Blocks.BOOKSHELF);
		c.set(26, 2, 2, Blocks.BOOKSHELF);
		c.set(26, 3, 2, Blocks.BOOKSHELF);

		// left wing: storage hall
		c.chest(Layout.IN_X, 2, Layout.IN_Z, W);              // INPUT chest (hopper shaft below, see Sorter)
		BlockState barrelN = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, N);
		for (int x = 2; x <= 12; x++) {
			for (int y = 2; y <= 4; y++) {
				c.set(x, y, 11, barrelN);
			}
		}
		BlockState barrelW = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, W);
		for (int z = 7; z <= 10; z++) {
			for (int y = 2; y <= 3; y++) {
				c.set(1, y, z, barrelW);
			}
		}
		c.doubleChest(1, 2, 5, false, W);
		// storage decor
		c.set(8, 2, 8, Blocks.CRAFTING_TABLE);

		// right wing: workshop along the back wall (facing the room = canonical NORTH)
		c.set(30, 2, 11, Blocks.CRAFTING_TABLE);
		c.set(31, 2, 11, Canvas.facing(Blocks.FURNACE, N));
		c.set(32, 2, 11, Canvas.facing(Blocks.SMOKER, N));
		c.set(33, 2, 11, Canvas.facing(Blocks.BLAST_FURNACE, N));
		c.set(34, 2, 11, Blocks.STONECUTTER);
		c.set(35, 2, 11, Blocks.GRINDSTONE);
		c.set(36, 2, 11, Blocks.LOOM);
		c.set(37, 2, 11, Blocks.CARTOGRAPHY_TABLE);
		c.set(38, 2, 11, Blocks.SMITHING_TABLE);
		c.set(39, 2, 11, Blocks.FLETCHING_TABLE);
		c.set(40, 2, 11, Blocks.ANVIL);
		c.set(41, 2, 11, Blocks.COMPOSTER);
		// kitchen corner + dining table
		c.set(41, 2, 2, Blocks.CAULDRON);
		c.set(41, 2, 3, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, W));
		c.set(34, 2, 6, Blocks.SPRUCE_FENCE);
		c.set(34, 3, 6, Blocks.SPRUCE_PRESSURE_PLATE);
		c.set(35, 2, 6, Blocks.SPRUCE_FENCE);
		c.set(35, 3, 6, Blocks.SPRUCE_PRESSURE_PLATE);
		c.set(33, 2, 6, Canvas.facing(Blocks.SPRUCE_STAIRS, E));
		c.set(36, 2, 6, Canvas.facing(Blocks.SPRUCE_STAIRS, W));
	}

	private void upperFloor() {
		for (int x : new int[] { 4, 10, 18, 24, 32, 38 }) {
			for (int z : new int[] { 3, 9 }) {
				lantern(x, 11, z);
			}
		}
		lantern(21, 11, -2);

		// bedrooms: three beds per wing along the outer wall
		for (int z : new int[] { 2, 5, 8 }) {
			c.bed(1, 8, z, S, DyeColor.RED);
			c.bed(41, 8, z, S, DyeColor.BLUE);
		}
		// lounge in the front hall, in front of the purple window
		c.fill(17, 8, -2, 25, 8, -1, Blocks.CARPET.pick(DyeColor.PURPLE));
		c.set(18, 8, -2, Canvas.facing(Blocks.SPRUCE_STAIRS, N));
		c.set(24, 8, -2, Canvas.facing(Blocks.SPRUCE_STAIRS, N));
		c.set(21, 8, -1, Blocks.SPRUCE_FENCE);
		c.set(21, 9, -1, Blocks.SPRUCE_PRESSURE_PLATE);
		for (int x = 16; x <= 26; x++) {
			if (x < 20 || x > 22) {
				c.set(x, 8, 11, Blocks.BOOKSHELF);
				c.set(x, 9, 11, Blocks.BOOKSHELF);
			}
		}
	}

	private void balconies() {
		for (int[] wing : new int[][] { { 1, 13 }, { 29, 41 } }) {
			int a = wing[0];
			int b = wing[1];
			c.fill(a, 7, -2, b, 7, -1, Blocks.SPRUCE_PLANKS);
			for (int x = a; x <= b; x++) {
				c.set(x, 8, -2, Blocks.SPRUCE_FENCE);
			}
			for (int z = -2; z <= -1; z++) {
				c.set(a, 8, z, Blocks.SPRUCE_FENCE);
				c.set(b, 8, z, Blocks.SPRUCE_FENCE);
			}
			// columns under the balcony
			for (int x : new int[] { a, (a + b) / 2, b }) {
				c.fill(x, 2, -2, x, 6, -2, Canvas.axis(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			}
			// balcony door
			int dx = (a + b) / 2;
			c.door(dx, 8, 0, N, Blocks.SPRUCE_DOOR);
			c.set(dx - 1, 8, 0, Blocks.OAK_PLANKS);
			c.set(dx + 1, 8, 0, Blocks.OAK_PLANKS);
			// flower boxes
			for (int x = a + 1; x <= b - 1; x += 2) {
				c.set(x, 8, -1, Blocks.FLOWER_POT);
			}
		}
	}

	private void exterior() {
		for (int x : new int[] { 16, 26 }) {
			c.fill(x, 1, -7, x, 3, -7, Blocks.SPRUCE_FENCE);
			c.set(x, 4, -7, Blocks.LANTERN);
		}
		for (int x : new int[] { 2, 12, 30, 40 }) {
			c.fill(x, 1, -4, x, 2, -4, Blocks.SPRUCE_FENCE);
			c.set(x, 3, -4, Blocks.LANTERN);
		}
		// benches beside the entrance
		c.set(15, 1, -4, Canvas.facing(Blocks.SPRUCE_STAIRS, S));
		c.set(27, 1, -4, Canvas.facing(Blocks.SPRUCE_STAIRS, S));
	}
}
