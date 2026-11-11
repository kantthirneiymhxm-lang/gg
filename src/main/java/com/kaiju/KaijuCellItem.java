package com.kaiju;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Right-click = move 1, Sneak + right-click = move 2. Needs the full armor set of the same kaiju. */
public class KaijuCellItem extends Item {
	private final int type;

	public KaijuCellItem(int type, Settings s) { super(s); this.type = type; }

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack st = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(st);
		if (!KaijuMod.fullSet(user, type)) {
			user.sendMessage(Text.translatable("msg.kaiju_kings.need_set"), true);
			return TypedActionResult.fail(st);
		}
		int move = user.isSneaking() ? 1 : 0;
		int idx = type * 2 + move;
		long now = world.getTime();
		long[] cd = Abilities.COOLDOWNS.computeIfAbsent(user.getUuid(), k -> new long[4]);
		if (now < cd[idx]) {
			user.sendMessage(Text.translatable("msg.kaiju_kings.cooldown", (cd[idx] - now) / 20 + 1), true);
			return TypedActionResult.fail(st);
		}
		ServerWorld w = (ServerWorld) world;
		PlayerEntity p = user;
		if (type == 0) {
			if (move == 0) {
				Abilities.breath(p, () -> p.getEyePos().add(p.getRotationVec(1f).multiply(1.5)), () -> p.getRotationVec(1f),
					60, 2.0, 12f, 8, 50, false);
				cd[idx] = now + 120;
			} else {
				Abilities.atomicNuke(p, () -> p.getPos().add(Vec3d.fromPolar(0, p.getYaw()).multiply(14)), 40, 7f, 22, 35f);
				cd[idx] = now + 500;
			}
		} else {
			if (move == 0) {
				Abilities.breath(p, () -> p.getEyePos().add(p.getRotationVec(1f).multiply(1.5)), () -> p.getRotationVec(1f),
					60, 2.0, 6f, 8, 60, true);
				cd[idx] = now + 160;
			} else {
				Abilities.lightningStorm(p, p::getPos, 20, 18, 120);
				cd[idx] = now + 700;
			}
		}
		return TypedActionResult.success(st);
	}
}
