package com.winkyqx.countryhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;

/**
 * Local coordinate system of one house.
 * x = to the player's right, z = forward (away from the player), y = up (0 = ground level).
 * Everything is authored in a "canonical" frame (forward = SOUTH) and rotated to the real facing.
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

	/** World-space box of the golem spawn platform (local x/z 3..11, y -9..-6). */
	public AABB platformBox() {
		BlockPos a = at(3, -9, 3);
		BlockPos b = at(11, -6, 11);
		return new AABB(
				Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
	}
}
