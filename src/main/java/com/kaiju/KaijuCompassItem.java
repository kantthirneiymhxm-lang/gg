package com.kaiju;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class KaijuCompassItem extends Item {
	private final int type;

	public KaijuCompassItem(int type, Settings s) { super(s); this.type = type; }

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (world.isClient || !(entity instanceof ServerPlayerEntity p) || world.getTime() % 10 != 0) return;
		NbtCompound n = stack.getOrCreateNbt();
		if (world.getRegistryKey() != World.OVERWORLD) {
			if (n.getBoolean("Has")) n.putBoolean("Has", false);
			return;
		}
		KaijuState st = KaijuState.get(p.getServer().getOverworld());
		BlockPos l = LairManager.nearest((ServerWorld) world, st, type, p.getBlockX(), p.getBlockZ());
		if (l == null) return;
		if (!n.getBoolean("Has") || n.getInt("TX") != l.getX() || n.getInt("TZ") != l.getZ()) {
			n.putInt("TX", l.getX());
			n.putInt("TZ", l.getZ());
			n.putBoolean("Has", true);
		}
		if (selected || p.getOffHandStack() == stack) {
			int dist = (int) Math.hypot(l.getX() - p.getX(), l.getZ() - p.getZ());
			p.sendMessage(Text.translatable("msg.kaiju_kings.compass",
				Text.translatable(type == 0 ? "entity.kaiju_kings.godzilla" : "entity.kaiju_kings.king_ghidorah"), dist), true);
		}
	}
}
