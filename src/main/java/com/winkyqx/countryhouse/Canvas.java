package com.winkyqx.countryhouse;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.item.DyeColor;

/** Drawing primitives in the local house frame. Directions are canonical (forward = SOUTH, +x = WEST). */
final class Canvas {
	final ServerLevel level;
	final HouseFrame f;
	private final List<BlockPos> wires = new ArrayList<>();

	Canvas(ServerLevel level, HouseFrame frame) {
		this.level = level;
		this.f = frame;
	}

	void set(int x, int y, int z, BlockState s) {
		level.setBlock(f.at(x, y, z), s.rotate(f.rot), Block.UPDATE_ALL);
	}

	void set(int x, int y, int z, Block b) {
		set(x, y, z, b.defaultBlockState());
	}

	void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					set(x, y, z, s);
				}
			}
		}
	}

	void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
		fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
	}

	void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
		fill(x1, y1, z1, x2, y2, z2, Blocks.AIR);
	}

	static BlockState axis(Block b, Direction.Axis a) {
		return b.defaultBlockState().setValue(BlockStateProperties.AXIS, a);
	}

	static BlockState facing(Block b, Direction d) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, d);
	}

	void door(int x, int y, int z, Direction facing, Block door) {
		BlockState lower = facing(door, facing);
		set(x, y, z, lower);
		set(x, y + 1, z, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
	}

	/** Open trapdoor shutter (panel against the wall behind it). */
	void shutter(int x, int y, int z, Direction facing, Block trapdoor) {
		set(x, y, z, facing(trapdoor, facing).setValue(BlockStateProperties.OPEN, true));
	}

	/** Bed: foot at (x,y,z), head one block further in the given canonical direction. */
	void bed(int x, int y, int z, Direction dir, DyeColor color) {
		int dx = 0;
		int dz = 0;
		switch (dir) {
			case SOUTH -> dz = 1;
			case NORTH -> dz = -1;
			case WEST -> dx = 1;
			default -> dx = -1;
		}
		BlockState base = facing(Blocks.BED.pick(color), dir);
		set(x, y, z, base.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
		set(x + dx, y, z + dz, base.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
	}

	void chest(int x, int y, int z, Direction facing) {
		set(x, y, z, facing(Blocks.CHEST, facing));
	}

	/** Double chest occupying (x0..x0+1, z) or (x, z0..z0+1). */
	void doubleChest(int x0, int y, int z0, boolean alongX, Direction canonFacing) {
		Direction wf = f.rot.rotate(canonFacing);
		BlockPos a = f.at(x0, y, z0);
		BlockPos b = alongX ? f.at(x0 + 1, y, z0) : f.at(x0, y, z0 + 1);
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

	void hopper(int x, int y, int z, Direction dir) {
		set(x, y, z, Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, dir));
	}

	void wire(int x, int y, int z) {
		set(x, y, z, Blocks.REDSTONE_WIRE);
		wires.add(f.at(x, y, z));
	}

	/** Re-evaluates dust connections once all neighbours exist. */
	void finishWires() {
		for (BlockPos p : wires) {
			BlockState s = level.getBlockState(p);
			level.setBlock(p, Block.updateFromNeighbourShapes(s, level, p), Block.UPDATE_ALL);
		}
		wires.clear();
	}

	void spawn(EntityType<?> type, int x, int y, int z) {
		type.spawn(level, f.at(x, y, z), EntitySpawnReason.MOB_SUMMONED);
	}

	void fillBelow(int x, int z, int depth) {
		for (int y = -1; y >= -depth; y--) {
			BlockPos p = f.at(x, y, z);
			BlockState s = level.getBlockState(p);
			if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) {
				level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}
}
