package com.winkyqx.countryhouse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Remembers built houses (per world) and runs the item filter + iron farm helper every 2 seconds. */
public final class HouseManager {
	private HouseManager() {}

	private record House(String dim, BlockPos origin, Direction facing) {}

	private static final List<House> HOUSES = new ArrayList<>();

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("countryhouse_houses.txt");
	}

	public static void clear() {
		HOUSES.clear();
	}

	public static void load(MinecraftServer server) {
		HOUSES.clear();
		Path path = file(server);
		if (!Files.exists(path)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(path)) {
				String[] p = line.split("\t");
				if (p.length != 5) {
					continue;
				}
				Direction facing = Direction.byName(p[4]);
				if (facing == null) {
					continue;
				}
				HOUSES.add(new House(p[0],
						new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])), facing));
			}
		} catch (IOException | NumberFormatException e) {
			CountryHouseMod.LOGGER.warn("Could not read houses file", e);
		}
	}

	private static void save(MinecraftServer server) {
		List<String> lines = new ArrayList<>();
		for (House h : HOUSES) {
			lines.add(h.dim() + "\t" + h.origin().getX() + "\t" + h.origin().getY() + "\t" + h.origin().getZ()
					+ "\t" + h.facing().getName());
		}
		try {
			Files.write(file(server), lines);
		} catch (IOException e) {
			CountryHouseMod.LOGGER.warn("Could not save houses file", e);
		}
	}

	public static void register(ServerLevel level, BlockPos origin, Direction facing) {
		HOUSES.add(new House(level.dimension().toString(), origin.immutable(), facing));
		save(level.getServer());
	}

	public static void tick(MinecraftServer server) {
		if (HOUSES.isEmpty() || server.getTickCount() % 40 != 0) {
			return;
		}
		for (House h : HOUSES) {
			ServerLevel level = null;
			for (ServerLevel l : server.getAllLevels()) {
				if (l.dimension().toString().equals(h.dim())) {
					level = l;
					break;
				}
			}
			if (level == null || !level.hasChunkAt(h.origin())) {
				continue;
			}
			HouseFrame frame = new HouseFrame(h.origin(), h.facing());
			sortInput(level, frame);
			golemFarm(level, frame);
		}
	}

	// ---------------------------------------------------------------- item filter

	private static void sortInput(ServerLevel level, HouseFrame frame) {
		for (Container in : containers(level, frame, Zone.INPUT)) {
			for (int i = 0; i < in.getContainerSize(); i++) {
				ItemStack stack = in.getItem(i);
				if (stack.isEmpty()) {
					continue;
				}
				List<Container> dest = containers(level, frame, ItemSorter.classify(stack));
				if (dest.isEmpty()) {
					continue;
				}
				int before = stack.getCount();
				ItemStack rest = insert(dest, stack.copy());
				if (rest.getCount() != before) {
					in.setItem(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
					in.setChanged();
				}
			}
		}
	}

	// ---------------------------------------------------------------- iron golem farm

	private static void golemFarm(ServerLevel level, HouseFrame frame) {
		AABB box = frame.platformBox();
		BlockPos probe = BlockPos.containing(box.minX, box.minY, box.minZ);
		if (!level.hasChunkAt(probe)) {
			return;
		}

		// 1) kill golems that spawned on the platform (drops iron + poppies)
		for (Entity e : level.getEntities((Entity) null, box, en -> en.getType() == EntityType.IRON_GOLEM)) {
			e.kill(level);
		}

		// 2) vacuum the drops into the IRON chest in the storage hall
		List<Container> out = containers(level, frame, Zone.IRON);
		if (out.isEmpty()) {
			return;
		}
		for (Entity e : level.getEntities((Entity) null, box, en -> en instanceof ItemEntity)) {
			ItemEntity drop = (ItemEntity) e;
			ItemStack rest = insert(out, drop.getItem().copy());
			if (rest.isEmpty()) {
				drop.discard();
			} else {
				drop.setItem(rest);
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	private static List<Container> containers(ServerLevel level, HouseFrame frame, Zone zone) {
		List<Container> result = new ArrayList<>();
		for (int[] c : zone.cells()) {
			BlockPos pos = frame.at(c[0], 2, c[1]);
			if (!level.hasChunkAt(pos)) {
				continue;
			}
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof Container container) {
				result.add(container);
			}
		}
		return result;
	}

	/** Inserts the stack into the containers; returns what did not fit. */
	private static ItemStack insert(List<Container> targets, ItemStack stack) {
		for (Container c : targets) {
			for (int i = 0; i < c.getContainerSize() && !stack.isEmpty(); i++) {
				ItemStack there = c.getItem(i);
				if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack)
						&& there.getCount() < there.getMaxStackSize()) {
					int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
					there.grow(move);
					stack.shrink(move);
					c.setChanged();
				}
			}
		}
		for (Container c : targets) {
			for (int i = 0; i < c.getContainerSize() && !stack.isEmpty(); i++) {
				if (c.getItem(i).isEmpty()) {
					int move = Math.min(stack.getCount(), stack.getMaxStackSize());
					c.setItem(i, stack.copyWithCount(move));
					stack.shrink(move);
					c.setChanged();
				}
			}
		}
		return stack;
	}
}
