package com.winkyqx.countryhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Orchestrates the whole build: basement sorter, underground farms, mansion, garden. */
final class HouseBuilder {
	private static final Direction N = Direction.NORTH;
	private static final Direction S = Direction.SOUTH;
	private static final Direction W = Direction.WEST;
	private static final Direction E = Direction.EAST;
	private static final int GX = 28; // garden shift (garden x 43..53)

	private final ServerLevel level;
	private final HouseFrame f;
	private final Canvas c;

	HouseBuilder(ServerLevel level, HouseFrame frame) {
		this.level = level;
		this.f = frame;
		this.c = new Canvas(level, frame);
	}

	void build() {
		clearSpace();
		new SorterBuilder(c).build();
		new FarmsBuilder(c).build();
		new MansionBuilder(c).build();
		garden();
		openings();
		c.finishWires();
	}

	/** Remove trees / grass / terrain bumps above the plot. */
	private void clearSpace() {
		for (int x = -3; x <= 57; x++) {
			for (int z = -16; z <= 16; z++) {
				for (int y = 1; y <= 24; y++) {
					BlockPos p = f.at(x, y, z);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		}
	}

	/** Hopper shaft from the INPUT chest down to the sorter pipe, and stairs down to the basement. */
	private void openings() {
		c.hopper(Layout.IN_X, 1, Layout.IN_Z, Direction.DOWN);
		c.hopper(Layout.IN_X, 0, Layout.IN_Z, Direction.DOWN);
		c.hopper(Layout.IN_X, -1, Layout.IN_Z, Direction.DOWN);

		for (int k = 1; k <= 6; k++) {
			int x = 5 + k;
			int y = 1 - k;
			for (int cy = y + 1; cy <= 1; cy++) {
				c.set(x, cy, 9, Blocks.AIR);
			}
			c.set(x, y, 9, Canvas.facing(Blocks.STONE_BRICK_STAIRS, E));
		}
	}

	private void garden() {
		for (int x = 43; x <= 53; x++) {
			for (int z = 0; z <= 14; z++) {
				c.set(x, 0, z, Blocks.GRASS_BLOCK);
				c.fillBelow(x, z, 3);
			}
		}
		for (int x = 43; x <= 53; x++) {
			c.set(x, 1, 0, Blocks.SPRUCE_FENCE);
			c.set(x, 1, 14, Blocks.SPRUCE_FENCE);
		}
		for (int z = 0; z <= 14; z++) {
			c.set(53, 1, z, Blocks.SPRUCE_FENCE);
		}
		c.set(48, 1, 0, Canvas.facing(Blocks.SPRUCE_FENCE_GATE, N));

		for (int z = 1; z <= 13; z++) {
			c.set(43, 0, z, Blocks.DIRT_PATH);
		}

		// crops: water row in the middle
		for (int x = 44 + 0; x <= 52; x++) {
			c.set(x, 0, 3, Blocks.WATER);
			for (int z : new int[] { 1, 2, 4, 5 }) {
				c.set(x, 0, z, Blocks.FARMLAND);
			}
			c.set(x, 1, 1, Blocks.WHEAT);
			c.set(x, 1, 2, Blocks.WHEAT);
			c.set(x, 1, 4, Blocks.CARROTS);
			c.set(x, 1, 5, Blocks.POTATOES);
		}

		// animal pen
		for (int x = 44; x <= 52; x++) {
			c.set(x, 1, 6, Blocks.SPRUCE_FENCE);
			c.set(x, 1, 11, Blocks.SPRUCE_FENCE);
		}
		for (int z = 6; z <= 11; z++) {
			c.set(44, 1, z, Blocks.SPRUCE_FENCE);
			c.set(52, 1, z, Blocks.SPRUCE_FENCE);
		}
		c.set(44, 1, 8, Canvas.facing(Blocks.SPRUCE_FENCE_GATE, W));
		c.spawn(EntityTypes.COW, 47, 1, 8);
		c.spawn(EntityTypes.COW, 49, 1, 9);
		c.spawn(EntityTypes.SHEEP, 46, 1, 9);
		c.spawn(EntityTypes.SHEEP, 50, 1, 8);
		c.spawn(EntityTypes.CHICKEN, 48, 1, 7);
		c.spawn(EntityTypes.CHICKEN, 51, 1, 10);

		// fast forest: sapling + bone-meal dispenser + button, three plots
		for (int x : new int[] { 46, 49, 52 }) {
			c.set(x, 1, 13, Blocks.OAK_SAPLING);
			c.set(x, 1, 12, Blocks.DISPENSER.defaultBlockState().setValue(BlockStateProperties.FACING, S));
			c.set(x, 2, 12, Blocks.OAK_BUTTON.defaultBlockState()
					.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
					.setValue(BlockStateProperties.HORIZONTAL_FACING, N));
			BlockEntity be = level.getBlockEntity(f.at(x, 1, 12));
			if (be instanceof DispenserBlockEntity dispenser) {
				dispenser.setItem(0, new ItemStack(Items.BONE_MEAL, 64));
				dispenser.setItem(1, new ItemStack(Items.BONE_MEAL, 64));
			}
		}
	}
}
