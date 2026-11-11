package com.kaiju;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import com.kaiju.entity.GhidorahEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import java.util.Random;

/** Every ~700 blocks there is a hidden lair for each kaiju. Compasses point to the nearest unclaimed lair. */
public final class LairManager {
	public static final int CELL = 700;

	public static BlockPos lair(long seed, int type, int cx, int cz) {
		Random r = new Random(seed ^ (cx * 341873128712L) ^ (cz * 132897987541L) ^ (type * 0x9E3779B97F4A7C15L));
		r.nextInt();
		return new BlockPos(cx * CELL + 100 + r.nextInt(CELL - 200), 0, cz * CELL + 100 + r.nextInt(CELL - 200));
	}

	public static String key(int type, int cx, int cz) { return type + ":" + cx + ":" + cz; }

	public static BlockPos nearest(ServerWorld w, KaijuState st, int type, int px, int pz) {
		int pcx = Math.floorDiv(px, CELL), pcz = Math.floorDiv(pz, CELL);
		BlockPos best = null;
		double bd = Double.MAX_VALUE;
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
			int cx = pcx + dx, cz = pcz + dz;
			if (st.consumed.contains(key(type, cx, cz))) continue;
			BlockPos l = lair(w.getSeed(), type, cx, cz);
			double d = Math.pow(l.getX() - px, 2) + Math.pow(l.getZ() - pz, 2);
			if (d < bd) { bd = d; best = l; }
		}
		return best;
	}

	public static void tick(MinecraftServer server) {
		ServerWorld ow = server.getOverworld();
		KaijuState st = KaijuState.get(ow);
		for (ServerPlayerEntity p : ow.getPlayers()) {
			for (int type = 0; type < 2; type++) {
				BlockPos l = nearest(ow, st, type, p.getBlockX(), p.getBlockZ());
				if (l == null) continue;
				if (Math.hypot(l.getX() - p.getX(), l.getZ() - p.getZ()) > 70) continue;
				if (!ow.isChunkLoaded(l)) continue;
				st.consumed.add(key(type, Math.floorDiv(l.getX(), CELL), Math.floorDiv(l.getZ(), CELL)));
				st.markDirty();
				spawn(ow, type, l);
			}
		}
	}

	private static void spawn(ServerWorld w, int type, BlockPos l) {
		int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, l.getX(), l.getZ());
		String nameKey;
		if (type == 0) {
			HostileEntity e = KaijuMod.GODZILLA.create(w);
			nameKey = "entity.kaiju_kings.godzilla";
			if (e == null) return;
			y += 10;
			e.refreshPositionAndAngles(l.getX() + 0.5, y, l.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
			w.spawnEntity(e);
			LightningEntity b = EntityType.LIGHTNING_BOLT.create(w);
			if (b != null) { b.refreshPositionAfterTeleport(new Vec3d(l.getX() + 0.5, y, l.getZ() + 0.5)); w.spawnEntity(b); }
			w.playSound(null, l.getX(), y, l.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 20f, 0.5f);
			for (ServerPlayerEntity p : w.getPlayers())
				p.sendMessage(Text.translatable("msg.kaiju_kings.awake", Text.translatable(nameKey)).formatted(Formatting.RED, Formatting.BOLD), false);
		} else {
			GhidorahEntity.spawnMeteor(w, l.getX() + 0.5, y, l.getZ() + 0.5);
			for (ServerPlayerEntity p : w.getPlayers())
				p.sendMessage(Text.translatable("msg.kaiju_kings.meteor").formatted(Formatting.GOLD, Formatting.BOLD), false);
		}
	}
}
