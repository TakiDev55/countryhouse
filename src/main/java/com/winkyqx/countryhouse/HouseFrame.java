package com.winkyqx.countryhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;

/**
 * Local coordinate system of one house.
 * x = to the player's right, z = forward (away from the player), y = up (0 = ground level).
 * Everything is authored in a "canonical" frame (forward = SOUTH, +x = WEST) and rotated to the real facing.
 */
public final class HouseFrame {
	public final BlockPos origin;
	public final Direction forward;
	public final Rotation rot;
	public final Direction right;

	public HouseFrame(BlockPos origin, Direction forward) {
		this.origin = origin;
		this.forward = forward;
		Rotation found = Rotation.NONE;
		for (Rotation r : Rotation.values()) {
			if (r.rotate(Direction.SOUTH) == forward) {
				found = r;
			}
		}
		this.rot = found;
		this.right = rot.rotate(Direction.WEST);
	}

	public BlockPos at(int x, int y, int z) {
		return origin.relative(right, x).relative(forward, z).above(y);
	}

	/** World-space box covering the given local block range (inclusive). */
	public AABB box(int x1, int y1, int z1, int x2, int y2, int z2) {
		BlockPos a = at(x1, y1, z1);
		BlockPos b = at(x2, y2, z2);
		return new AABB(
				Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
	}

	public AABB ironPlatform() {
		return box(4, -26, 12, 12, -24, 20);
	}

	public AABB xpRoom() {
		return box(6, -26, -12, 36, -23, 0);
	}

	public AABB animalRoom() {
		return box(33, -26, 6, 45, -23, 16);
	}
}
