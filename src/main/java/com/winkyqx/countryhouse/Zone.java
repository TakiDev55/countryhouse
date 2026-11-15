package com.winkyqx.countryhouse;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Double-chest slots in the storage hall (local coordinates, y = 2). */
public enum Zone {
	INPUT(1, 13, true, Blocks.WHITE_WOOL),
	JUNK(3, 13, true, Blocks.BLACK_WOOL),
	ORES(5, 13, true, Blocks.LIGHT_BLUE_WOOL),
	FOOD(7, 13, true, Blocks.RED_WOOL),
	GEAR(9, 13, true, Blocks.GRAY_WOOL),
	BLOCKS(11, 13, true, Blocks.BROWN_WOOL),
	MISC(13, 9, false, Blocks.YELLOW_WOOL),
	IRON(13, 11, false, Blocks.ORANGE_WOOL);

	public final int x;
	public final int z;
	public final boolean alongX;
	public final Block marker;

	Zone(int x, int z, boolean alongX, Block marker) {
		this.x = x;
		this.z = z;
		this.alongX = alongX;
		this.marker = marker;
	}

	/** Canonical-frame direction the chest front faces. */
	public Direction facing() {
		return alongX ? Direction.NORTH : Direction.EAST;
	}

	/** The two (x, z) cells of the double chest. */
	public List<int[]> cells() {
		return alongX
				? List.of(new int[] { x, z }, new int[] { x + 1, z })
				: List.of(new int[] { x, z }, new int[] { x, z + 1 });
	}
}
