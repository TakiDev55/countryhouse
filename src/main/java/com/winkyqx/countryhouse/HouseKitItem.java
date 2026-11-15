package com.winkyqx.countryhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Right-click on the ground: builds the house. The house starts at the clicked block and extends forward + to the right. */
public class HouseKitItem extends Item {
	public HouseKitItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		Player player = ctx.getPlayer();
		Direction forward = player != null ? player.getDirection() : Direction.NORTH;
		if (forward.getAxis().isVertical()) {
			forward = Direction.NORTH;
		}

		BlockPos origin = ctx.getClickedPos();
		if (origin.getY() - 12 < serverLevel.getMinY() || origin.getY() + 18 > serverLevel.getMaxY()) {
			if (player != null) {
				player.sendSystemMessage(Component.literal("المكان هنا ما يصلح للبناء (قريب من القاع أو السقف)."));
			}
			return InteractionResult.FAIL;
		}

		HouseFrame frame = new HouseFrame(origin, forward);
		new HouseBuilder(serverLevel, frame).build();
		HouseManager.register(serverLevel, origin, forward);

		if (player != null && !player.hasInfiniteMaterials()) {
			ctx.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}
