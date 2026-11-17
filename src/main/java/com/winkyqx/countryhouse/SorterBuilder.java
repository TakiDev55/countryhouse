package com.winkyqx.countryhouse;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Basement redstone item sorter (hopper pipe + one compact filter tile per resource).
 *
 * Per tile at column x0 (z = Layout.SORT_Z = zs):
 *   P  pipe hopper        (x0, -2, zs)   passes items east
 *   F  filter hopper      (x0, -3, zs)   1 filter item + 21 plugs (filled by HouseManager)
 *   O  output hopper      (x0, -4, zs)   facing down into the resource chest (x0, -5, zs)
 *   C  comparator         (x0, -3, zs+1) reads F; D1 (x0,-3,zs+2) and D2 (x0-1,-3,zs+2) dust decay by 1,
 *                                        so D2 is only powered when F holds one extra item
 *   X  block under D2     (x0-1,-4,zs+2) torch T at (x0-2,-4,zs+2) turns OFF when X is powered
 *   Z1,Z2 dust + R repeater lock O while T is lit (rest state); the extra item unlocks O for one transfer
 */
final class SorterBuilder {
	private static final Direction N = Direction.NORTH;
	private static final Direction E = Direction.EAST;
	private static final Direction W = Direction.WEST;

	private static final DyeColor[] COLORS = {
			DyeColor.WHITE, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.BLACK, DyeColor.LIGHT_BLUE, DyeColor.LIME,
			DyeColor.RED, DyeColor.BLUE, DyeColor.GRAY, DyeColor.LIGHT_GRAY, DyeColor.BROWN, DyeColor.PINK };

	private final Canvas c;

	SorterBuilder(Canvas canvas) {
		this.c = canvas;
	}

	void build() {
		int zs = Layout.SORT_Z;
		// hall: solid shell, carved interior (x 1..42, z 2..11, y -5..-2)
		c.fill(0, -7, 1, 43, -1, 12, Blocks.STONE_BRICKS);
		c.clear(1, -5, 2, 42, -2, 11);
		// supports behind the chest row for dust / repeaters
		c.fill(3, -5, zs, 41, -5, zs + 3, Blocks.STONE_BRICKS);

		int n = Layout.FILTERS.length;
		for (int i = 0; i < n; i++) {
			tile(Layout.tileX(i), zs, COLORS[i % COLORS.length]);
		}

		// pipe line
		int end = Layout.pipeEndX();
		for (int x = 2; x < end; x++) {
			c.hopper(x, Layout.PIPE_Y, zs, E);
		}
		c.hopper(end, Layout.PIPE_Y, zs, Direction.DOWN);
		c.chest(end, Layout.F_Y, zs, N); // overflow chest: everything that is not filtered

		// lighting
		for (int x = 4; x <= 40; x += 6) {
			c.set(x, -1, 3, Blocks.SEA_LANTERN);
			c.set(x, -1, 9, Blocks.SEA_LANTERN);
		}
	}

	private void tile(int x0, int zs, DyeColor color) {
		c.hopper(x0, Layout.F_Y, zs, N);                 // filter hopper (faces the label block, no output)
		c.hopper(x0, Layout.O_Y, zs, Direction.DOWN);    // output hopper
		c.chest(x0, Layout.CHEST_Y, zs, N);              // resource chest
		c.set(x0, Layout.F_Y, zs - 1, Blocks.WOOL.pick(color)); // colour label

		c.set(x0, -4, zs + 1, Blocks.STONE_BRICKS);      // comparator support
		c.set(x0, -3, zs + 1, Canvas.facing(Blocks.COMPARATOR, N)); // input side = north = F
		c.set(x0, -4, zs + 2, Blocks.STONE_BRICKS);      // D1 support
		c.set(x0 - 1, -4, zs + 2, Blocks.STONE_BRICKS);  // X
		c.wire(x0, -3, zs + 2);                          // D1
		c.wire(x0 - 1, -3, zs + 2);                      // D2
		c.set(x0 - 2, -4, zs + 2, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, W));
		c.wire(x0 - 2, -4, zs + 1);                      // Z1
		c.wire(x0 - 2, -4, zs);                          // Z2
		c.set(x0 - 1, -4, zs, Canvas.facing(Blocks.REPEATER, W)); // input west, output east into O
	}
}
