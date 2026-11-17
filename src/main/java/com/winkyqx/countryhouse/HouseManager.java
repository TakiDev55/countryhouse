package com.winkyqx.countryhouse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Remembers built houses per world and runs the helper logic every 2 seconds:
 *  - fills / repairs the filter hoppers of the redstone sorter
 *  - (optional fallback) sorts the INPUT chest in code when countryhouse.properties has javaSorter=true
 *  - iron farm: kills spawned golems, collects drops
 *  - xp farm: kills monsters in the dark hall, awards XP orbs on the pad, collects drops
 *  - crop farm: harvests + replants, animal farm: keeps the herd and collects drops
 * All farm output goes to the INPUT chest, so the redstone sorter distributes it.
 */
public final class HouseManager {
	private HouseManager() {}

	private record House(String dim, BlockPos origin, Direction facing) {}

	private static final List<House> HOUSES = new ArrayList<>();
	private static boolean javaSorter = false;
	private static final int[] PLUGS = { 5, 5, 5, 6 };

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("countryhouse_houses.txt");
	}

	public static void clear() {
		HOUSES.clear();
	}

	public static void load(MinecraftServer server) {
		HOUSES.clear();
		loadConfig();
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

	private static void loadConfig() {
		Path cfg = FabricLoader.getInstance().getConfigDir().resolve("countryhouse.properties");
		try {
			if (!Files.exists(cfg)) {
				Files.writeString(cfg, "# true = sort the INPUT chest in code instead of (or if) the redstone sorter misbehaves\njavaSorter=false\n");
			}
			Properties p = new Properties();
			try (InputStream in = Files.newInputStream(cfg)) {
				p.load(in);
			}
			javaSorter = Boolean.parseBoolean(p.getProperty("javaSorter", "false").trim());
		} catch (IOException e) {
			CountryHouseMod.LOGGER.warn("Could not read countryhouse.properties", e);
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
		long cycle = server.getTickCount() / 40;
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
			HouseFrame f = new HouseFrame(h.origin(), h.facing());
			sorterFill(level, f);
			if (javaSorter) {
				javaSort(level, f);
			}
			List<Container> input = chests(level, f, Layout.IN_X, 2, Layout.IN_Z);
			if (input.isEmpty()) {
				continue;
			}
			ironFarm(level, f, input);
			xpFarm(level, f, input);
			crops(level, f, input);
			animals(level, f, input, cycle);
		}
	}

	// ---------------------------------------------------------------- sorter

	private static void sorterFill(ServerLevel level, HouseFrame f) {
		for (int i = 0; i < Layout.FILTERS.length; i++) {
			BlockPos pos = f.at(Layout.tileX(i), Layout.F_Y, Layout.SORT_Z);
			if (!level.hasChunkAt(pos)) {
				continue;
			}
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof Container hopper && hopper.getContainerSize() >= 5 && hopper.isEmpty()) {
				hopper.setItem(0, new ItemStack(Layout.FILTERS[i], 1));
				for (int s = 0; s < PLUGS.length; s++) {
					hopper.setItem(s + 1, new ItemStack(Items.BARRIER, PLUGS[s]));
				}
				hopper.setChanged();
			}
		}
	}

	private static void javaSort(ServerLevel level, HouseFrame f) {
		List<Container> input = chests(level, f, Layout.IN_X, 2, Layout.IN_Z);
		List<Container> overflow = chests(level, f, Layout.pipeEndX(), Layout.F_Y, Layout.SORT_Z);
		for (Container in : input) {
			for (int i = 0; i < in.getContainerSize(); i++) {
				ItemStack stack = in.getItem(i);
				if (stack.isEmpty()) {
					continue;
				}
				List<Container> dest = overflow;
				for (int t = 0; t < Layout.FILTERS.length; t++) {
					Item filter = Layout.FILTERS[t];
					if (stack.is(filter)) {
						dest = chests(level, f, Layout.tileX(t), Layout.CHEST_Y, Layout.SORT_Z);
						break;
					}
				}
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

	// ---------------------------------------------------------------- farms

	private static void ironFarm(ServerLevel level, HouseFrame f, List<Container> out) {
		AABB box = f.ironPlatform();
		if (!level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))) {
			return;
		}
		for (Entity e : level.getEntities((Entity) null, box, en -> en.getType() == EntityTypes.IRON_GOLEM)) {
			e.kill(level);
		}
		vacuum(level, box, out);
	}

	private static void xpFarm(ServerLevel level, HouseFrame f, List<Container> out) {
		AABB box = f.xpRoom();
		if (!level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))) {
			return;
		}
		int kills = 0;
		for (Entity e : level.getEntities((Entity) null, box, en -> en.getType().getCategory() == MobCategory.MONSTER)) {
			e.kill(level);
			kills++;
		}
		if (kills > 0) {
			Vec3 pad = Vec3.atCenterOf(f.at(Layout.PAD_X, Layout.FARM_Y, Layout.PAD_Z));
			ExperienceOrb.award(level, pad, 6 * kills);
		}
		vacuum(level, box, out);
	}

	private static void crops(ServerLevel level, HouseFrame f, List<Container> out) {
		for (int x = 18; x <= 30; x++) {
			for (int z : new int[] { 7, 8, 9, 10, 12, 13, 14, 15 }) {
				BlockPos pos = f.at(x, Layout.FARM_Y + 1, z);
				if (!level.hasChunkAt(pos)) {
					return;
				}
				BlockState state = level.getBlockState(pos);
				if (state.hasProperty(BlockStateProperties.AGE_7) && state.getValue(BlockStateProperties.AGE_7) >= 7) {
					for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
						ItemStack rest = insert(out, drop.copy());
						if (!rest.isEmpty()) {
							Block.popResource(level, pos, rest);
						}
					}
					level.setBlock(pos, state.setValue(BlockStateProperties.AGE_7, 0), Block.UPDATE_ALL);
				}
			}
		}
	}

	private static void animals(ServerLevel level, HouseFrame f, List<Container> out, long cycle) {
		AABB box = f.animalRoom();
		if (!level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))) {
			return;
		}
		EntityType<?>[] types = { EntityTypes.COW, EntityTypes.SHEEP, EntityTypes.CHICKEN };
		for (int t = 0; t < types.length; t++) {
			final EntityType<?> type = types[t];
			List<Entity> herd = level.getEntities((Entity) null, box, en -> en.getType() == type);
			if (herd.size() >= 7) {
				for (int i = 0; i < herd.size() - 4; i++) {
					herd.get(i).kill(level);
				}
			} else if (cycle % 3 == 0) {
				type.spawn(level, f.at(36 + t * 3, Layout.FARM_Y, 9 + t * 2), EntitySpawnReason.MOB_SUMMONED);
			}
		}
		vacuum(level, box, out);
	}

	private static void vacuum(ServerLevel level, AABB box, List<Container> out) {
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

	private static List<Container> chests(ServerLevel level, HouseFrame f, int x, int y, int z) {
		List<Container> result = new ArrayList<>();
		BlockPos pos = f.at(x, y, z);
		if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Container container) {
			result.add(container);
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
