package com.winkyqx.countryhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Builds the whole house. Local frame: x = right, z = forward, y = up (0 = ground).
 * Directions below are written in the canonical frame (forward = SOUTH, +x = WEST);
 * HouseFrame rotates everything to the direction the player is facing.
 *
 * Layout (local):
 *   House        x 0..14, z 0..14, floor y=1, walls y=2..5, gable roof
 *   Storage hall back wall z=13 (chests y=2, barrels y=3..4), see Zone
 *   Garden       x 15..25, z 0..14: crops, animal pen, fast forest (3 sapling plots)
 *   Iron farm    sealed bunker under the house, y -10..-5, x/z 2..12
 */
final class HouseBuilder {
	private static final Direction N = Direction.NORTH;
	private static final Direction S = Direction.SOUTH;
	private static final Direction E = Direction.EAST;
	private static final Direction W = Direction.WEST;

	private final ServerLevel level;
	private final HouseFrame f;

	HouseBuilder(ServerLevel level, HouseFrame frame) {
		this.level = level;
		this.f = frame;
	}

	void build() {
		clearSpace();
		ironFarm();
		ground();
		shell();
		roof();
		interior();
		storage();
		garden();
	}

	// ------------------------------------------------------------------ primitives

	private void set(int x, int y, int z, BlockState s) {
		level.setBlock(f.at(x, y, z), s.rotate(f.rot), Block.UPDATE_ALL);
	}

	private void set(int x, int y, int z, Block b) {
		set(x, y, z, b.defaultBlockState());
	}

	private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					set(x, y, z, s);
				}
			}
		}
	}

	private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
		fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
	}

	private static BlockState log(Block b, Direction.Axis axis) {
		return b.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
	}

	private static BlockState stairs(Block b, Direction facing) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	private static BlockState facing(Block b, Direction d) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, d);
	}

	private void door(int x, int z, Direction facing, Block door) {
		BlockState lower = door.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		set(x, 2, z, lower);
		set(x, 3, z, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
	}

	/** Bed: foot at (x,y,z), head one block further in the given canonical direction. */
	private void bed(int x, int y, int z, Direction dir) {
		int dx = 0;
		int dz = 0;
		switch (dir) {
			case SOUTH -> dz = 1;
			case NORTH -> dz = -1;
			case WEST -> dx = 1;
			default -> dx = -1;
		}
		BlockState base = Blocks.RED_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
		set(x, y, z, base.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
		set(x + dx, y, z + dz, base.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
	}

	private void doubleChest(Zone zone) {
		Direction wf = f.rot.rotate(zone.facing());
		int[] c0 = zone.cells().get(0);
		int[] c1 = zone.cells().get(1);
		BlockPos a = f.at(c0[0], 2, c0[1]);
		BlockPos b = f.at(c1[0], 2, c1[1]);
		BlockPos rightPos;
		BlockPos leftPos;
		if (b.equals(a.relative(wf.getClockWise()))) {
			rightPos = a;
			leftPos = b;
		} else {
			rightPos = b;
			leftPos = a;
		}
		BlockState base = Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, wf);
		level.setBlock(rightPos, base.setValue(BlockStateProperties.CHEST_TYPE, ChestType.RIGHT), Block.UPDATE_ALL);
		level.setBlock(leftPos, base.setValue(BlockStateProperties.CHEST_TYPE, ChestType.LEFT), Block.UPDATE_ALL);
	}

	private void spawn(EntityType<?> type, int x, int y, int z) {
		type.spawn(level, f.at(x, y, z), EntitySpawnReason.MOB_SUMMONED);
	}

	// ------------------------------------------------------------------ steps

	/** Remove trees/grass/etc. above the plot so the house has clean space. */
	private void clearSpace() {
		for (int x = -2; x <= 27; x++) {
			for (int z = -3; z <= 16; z++) {
				for (int y = 1; y <= 16; y++) {
					BlockPos p = f.at(x, y, z);
					if (!level.getBlockState(p).isAir()) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		}
	}

	private void fillBelow(int x, int z) {
		for (int y = -1; y >= -3; y--) {
			BlockPos p = f.at(x, y, z);
			BlockState s = level.getBlockState(p);
			if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) {
				level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private void ground() {
		for (int x = 0; x <= 14; x++) {
			for (int z = 0; z <= 14; z++) {
				set(x, 0, z, Blocks.COBBLESTONE);
				fillBelow(x, z);
			}
		}
		for (int x = 15; x <= 25; x++) {
			for (int z = 0; z <= 14; z++) {
				set(x, 0, z, Blocks.GRASS_BLOCK);
				fillBelow(x, z);
			}
		}
	}

	private void wall(int x, int z, Direction.Axis beamAxis) {
		set(x, 2, z, Blocks.COBBLESTONE);
		set(x, 3, z, Blocks.BIRCH_PLANKS);
		set(x, 4, z, Blocks.BIRCH_PLANKS);
		set(x, 5, z, log(Blocks.DARK_OAK_LOG, beamAxis));
	}

	private void shell() {
		fill(0, 1, 0, 14, 1, 14, Blocks.COBBLESTONE);
		fill(1, 1, 1, 13, 1, 13, Blocks.SPRUCE_PLANKS);

		for (int x = 0; x <= 14; x++) {
			wall(x, 0, Direction.Axis.X);
			wall(x, 14, Direction.Axis.X);
		}
		for (int z = 1; z <= 13; z++) {
			wall(0, z, Direction.Axis.Z);
			wall(14, z, Direction.Axis.Z);
		}
		int[] posts = { 0, 5, 9, 14 };
		for (int a : posts) {
			for (int b : new int[] { 0, 14 }) {
				fill(a, 2, b, a, 5, b, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
				fill(b, 2, a, b, 5, a, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
			}
		}

		// windows
		for (int x : new int[] { 2, 3, 11, 12 }) {
			fill(x, 3, 0, x, 4, 0, Blocks.GLASS_PANE);
		}
		for (int z : new int[] { 3, 4, 10, 11 }) {
			fill(0, 3, z, 0, 4, z, Blocks.GLASS_PANE);
		}
		for (int z : new int[] { 7, 8, 11, 12 }) {
			fill(14, 3, z, 14, 4, z, Blocks.GLASS_PANE);
		}

		// doors: front + side door to the garden
		door(7, 0, N, Blocks.SPRUCE_DOOR);
		door(14, 3, W, Blocks.SPRUCE_DOOR);
		for (int x = 6; x <= 8; x++) {
			set(x, 1, -1, Blocks.COBBLESTONE_SLAB);
		}
		set(15, 1, 3, Blocks.COBBLESTONE_SLAB);

		// ceiling (bottom slabs: mobs cannot spawn in the attic)
		fill(0, 6, 0, 14, 6, 14, Blocks.SPRUCE_SLAB);
	}

	private void roof() {
		for (int k = 0; k <= 7; k++) {
			int y = 6 + k;
			for (int x = -1; x <= 15; x++) {
				set(x, y, k - 1, stairs(Blocks.SPRUCE_STAIRS, S));
				set(x, y, 15 - k, stairs(Blocks.SPRUCE_STAIRS, N));
			}
			// gable ends
			for (int x : new int[] { 0, 14 }) {
				fill(x, y, k, x, y, 14 - k, Blocks.BIRCH_PLANKS);
			}
		}
		for (int x = -1; x <= 15; x++) {
			set(x, 14, 7, Blocks.SPRUCE_PLANKS);
		}
	}

	private void interior() {
		// lanterns
		BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
		for (int x : new int[] { 3, 7, 11 }) {
			for (int z : new int[] { 3, 7, 11 }) {
				set(x, 5, z, lantern);
			}
		}

		// beds along the left wall
		bed(1, 2, 2, S);
		bed(1, 2, 5, S);

		// bookshelves
		fill(1, 2, 8, 1, 4, 8, Blocks.BOOKSHELF);

		// rug + dining table with two chairs
		fill(6, 2, 3, 8, 2, 5, Blocks.RED_CARPET);
		set(5, 2, 6, Blocks.SPRUCE_FENCE);
		set(5, 3, 6, Blocks.SPRUCE_PRESSURE_PLATE);
		set(4, 2, 6, stairs(Blocks.SPRUCE_STAIRS, E));
		set(6, 2, 6, stairs(Blocks.SPRUCE_STAIRS, W));

		// workstations along the right wall (facing the room = canonical EAST)
		set(13, 2, 5, Blocks.CRAFTING_TABLE);
		set(13, 2, 6, facing(Blocks.FURNACE, E));
		set(13, 2, 7, facing(Blocks.SMOKER, E));
		set(13, 2, 8, facing(Blocks.BLAST_FURNACE, E));
		set(12, 2, 5, Blocks.STONECUTTER);
		set(12, 2, 8, Blocks.GRINDSTONE);
		set(13, 2, 1, Blocks.CAULDRON);
		set(13, 2, 2, Blocks.COMPOSTER);
		set(12, 2, 1, Blocks.SMITHING_TABLE);
	}

	private void storage() {
		// colored markers + double chests
		for (Zone z : Zone.values()) {
			doubleChest(z);
			for (int[] c : z.cells()) {
				if (z.alongX) {
					set(c[0], 5, c[1], z.marker);
				} else {
					set(c[0], 3, c[1], z.marker);
				}
			}
		}
		// bulk barrel wall above the chests + left wall block
		BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, N);
		for (int x = 1; x <= 12; x++) {
			set(x, 3, 13, barrel);
			set(x, 4, 13, barrel);
		}
		BlockState barrelW = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, W);
		for (int z = 9; z <= 12; z++) {
			set(1, 2, z, barrelW);
			set(1, 3, z, barrelW);
		}
	}

	private void garden() {
		// fence ring: front (with gate), back, far side
		for (int x = 15; x <= 25; x++) {
			set(x, 1, 0, Blocks.SPRUCE_FENCE);
			set(x, 1, 14, Blocks.SPRUCE_FENCE);
		}
		for (int z = 0; z <= 14; z++) {
			set(25, 1, z, Blocks.SPRUCE_FENCE);
		}
		set(20, 1, 0, facing(Blocks.SPRUCE_FENCE_GATE, N));

		// path from the side door
		for (int z = 1; z <= 13; z++) {
			set(15, 0, z, Blocks.DIRT_PATH);
		}

		// crops: water row in the middle, farmland on both sides
		for (int x = 16; x <= 24; x++) {
			set(x, 0, 3, Blocks.WATER);
			set(x, 0, 1, Blocks.FARMLAND);
			set(x, 0, 2, Blocks.FARMLAND);
			set(x, 0, 4, Blocks.FARMLAND);
			set(x, 0, 5, Blocks.FARMLAND);
			set(x, 1, 1, Blocks.WHEAT);
			set(x, 1, 2, Blocks.WHEAT);
			set(x, 1, 4, Blocks.CARROTS);
			set(x, 1, 5, Blocks.POTATOES);
		}

		// animal pen
		for (int x = 16; x <= 24; x++) {
			set(x, 1, 6, Blocks.SPRUCE_FENCE);
			set(x, 1, 11, Blocks.SPRUCE_FENCE);
		}
		for (int z = 6; z <= 11; z++) {
			set(16, 1, z, Blocks.SPRUCE_FENCE);
			set(24, 1, z, Blocks.SPRUCE_FENCE);
		}
		set(16, 1, 8, facing(Blocks.SPRUCE_FENCE_GATE, W));
		spawn(EntityType.COW, 19, 1, 8);
		spawn(EntityType.COW, 21, 1, 9);
		spawn(EntityType.SHEEP, 18, 1, 9);
		spawn(EntityType.SHEEP, 22, 1, 8);
		spawn(EntityType.CHICKEN, 20, 1, 7);
		spawn(EntityType.CHICKEN, 23, 1, 10);

		// fast forest: sapling + bone-meal dispenser + button, three plots
		for (int x : new int[] { 18, 21, 24 }) {
			set(x, 1, 13, Blocks.OAK_SAPLING);
			set(x, 1, 12, Blocks.DISPENSER.defaultBlockState().setValue(BlockStateProperties.FACING, S));
			set(x, 2, 12, Blocks.OAK_BUTTON.defaultBlockState()
					.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
					.setValue(BlockStateProperties.HORIZONTAL_FACING, N));
			BlockEntity be = level.getBlockEntity(f.at(x, 1, 12));
			if (be instanceof DispenserBlockEntity dispenser) {
				dispenser.setItem(0, new ItemStack(Items.BONE_MEAL, 64));
				dispenser.setItem(1, new ItemStack(Items.BONE_MEAL, 64));
			}
		}
	}

	/** Sealed underground bunker: 3 villagers + beds + a caged zombie, spawn platform, golems are handled by HouseManager. */
	private void ironFarm() {
		// solid shell, then carve the platform room
		fill(2, -10, 2, 12, -5, 12, Blocks.STONE_BRICKS);
		fill(3, -8, 3, 11, -6, 11, Blocks.AIR);

		// villager cell + zombie cell (one solid block, then carve)
		fill(5, -8, 5, 10, -6, 9, Blocks.STONE_BRICKS);
		fill(6, -8, 6, 7, -7, 8, Blocks.AIR);
		set(9, -8, 7, Blocks.AIR);
		set(9, -7, 7, Blocks.AIR);
		set(8, -8, 7, Blocks.GLASS);
		set(8, -7, 7, Blocks.GLASS);

		// light everything so hostile mobs cannot spawn in the bunker
		set(6, -6, 7, Blocks.SEA_LANTERN);
		set(9, -6, 7, Blocks.SEA_LANTERN);
		for (int[] p : new int[][] { { 4, 4 }, { 4, 10 }, { 10, 4 }, { 10, 10 }, { 7, 3 }, { 7, 11 }, { 3, 7 }, { 11, 7 } }) {
			set(p[0], -5, p[1], Blocks.SEA_LANTERN);
		}

		// entities first (spawned above the bed height), then beds
		for (int z = 6; z <= 8; z++) {
			spawn(EntityType.VILLAGER, 6, -7, z);
		}
		Mob zombie = EntityType.ZOMBIE.spawn(level, f.at(9, -8, 7), EntitySpawnReason.MOB_SUMMONED);
		if (zombie != null) {
			zombie.setNoAi(true);
			zombie.setSilent(true);
			zombie.setPersistenceRequired();
		}
		for (int z = 6; z <= 8; z++) {
			bed(6, -8, z, W);
		}
	}
}
