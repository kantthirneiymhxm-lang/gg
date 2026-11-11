package com.kaiju;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/** All the cool moves, shared by the mobs and by players wearing the armor. */
public final class Abilities {
	public static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();
	private static final List<Job> JOBS = new ArrayList<>();
	private static final List<Job> PENDING = new ArrayList<>();
	private static final Random RNG = new Random();
	private static final DustParticleEffect BLUE = new DustParticleEffect(new Vector3f(0.25f, 0.65f, 1f), 2.5f);
	private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1f, 0.8f, 0.1f), 2.5f);

	private static final class Job {
		final IntPredicate fn; int t;
		Job(IntPredicate fn) { this.fn = fn; }
	}

	/** fn gets the tick number (0,1,2..) and returns true when finished. */
	public static void schedule(IntPredicate fn) { PENDING.add(new Job(fn)); }

	public static void tickJobs() {
		if (!PENDING.isEmpty()) { JOBS.addAll(PENDING); PENDING.clear(); }
		JOBS.removeIf(j -> {
			try { return j.fn.test(j.t++); }
			catch (Exception e) { KaijuMod.LOGGER.error("ability job failed", e); return true; }
		});
	}

	public static void fx(ServerWorld w, ParticleEffect p, double x, double y, double z, int n, double dx, double dy, double dz, double sp) {
		for (ServerPlayerEntity pl : w.getPlayers()) {
			if (pl.squaredDistanceTo(x, y, z) > 500 * 500) continue;
			w.spawnParticles(pl, p, true, x, y, z, n, dx, dy, dz, sp);
		}
	}

	public static void sound(World w, Vec3d p, SoundEvent s, float vol, float pitch) {
		w.playSound(null, p.x, p.y, p.z, s, SoundCategory.HOSTILE, vol, pitch);
	}

	// ---------------------------------------------------------------- BEAMS
	/** Atomic breath (gravity=false) or gravity beam (gravity=true). */
	public static void breath(LivingEntity src, Supplier<Vec3d> origin, Supplier<Vec3d> aim, double range, double width,
							  float dmg, int windup, int dur, boolean gravity) {
		ServerWorld w = (ServerWorld) src.getWorld();
		sound(w, origin.get(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 12f, gravity ? 1.2f : 0.55f);
		ParticleEffect col = gravity ? GOLD : BLUE;
		schedule(t -> {
			if (!src.isAlive() || t >= windup + dur) return true;
			Vec3d o = origin.get();
			if (t < windup) {
				double s = 1.8 * (1 - (double) t / windup) + 0.3;
				fx(w, col, o.x, o.y, o.z, 6, s, s, s, 0);
				fx(w, gravity ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK, o.x, o.y, o.z, 4, s, s, s, 0.2);
				return false;
			}
			int k = t - windup;
			if (k == 0) sound(w, o, gravity ? SoundEvents.ENTITY_BEACON_ACTIVATE : SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 14f, 0.6f);
			if (k % 4 == 0) sound(w, o, gravity ? SoundEvents.BLOCK_BEACON_AMBIENT : SoundEvents.ENTITY_BLAZE_SHOOT, 6f, 0.5f);
			beam(w, src, o, aim.get().normalize(), range, width, dmg, gravity, k);
			return false;
		});
	}

	private static void beam(ServerWorld w, LivingEntity src, Vec3d start, Vec3d dir, double range, double width,
							 float dmg, boolean gravity, int tick) {
		Vec3d end = start.add(dir.multiply(range));
		HitResult hit = w.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, src));
		boolean hitBlock = hit.getType() != HitResult.Type.MISS;
		Vec3d stop = hitBlock ? hit.getPos() : end;
		double len = stop.subtract(start).length();
		ParticleEffect core = gravity ? GOLD : BLUE;
		ParticleEffect edge = gravity ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME;
		double r = width * 0.35;
		double step = Math.max(1.0, width * 0.4);
		int cnt = Math.max(3, (int) width);
		int ecnt = Math.max(1, (int) (width / 3));
		for (double d = 0; d < len; d += step) {
			Vec3d p = start.add(dir.multiply(d));
			fx(w, core, p.x, p.y, p.z, cnt, r, r, r, 0);
			fx(w, edge, p.x, p.y, p.z, ecnt, r, r, r, 0.02);
			if (((int) (d / step) + tick) % 3 == 0) fx(w, ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, ecnt, width * 0.6, width * 0.6, width * 0.6, 0.3);
		}
		DamageSource ds = src instanceof PlayerEntity pe ? w.getDamageSources().playerAttack(pe) : w.getDamageSources().mobAttack(src);
		Box box = new Box(start, stop).expand(width + 3);
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> x != src && x.isAlive() && x.getType() != src.getType())) {
			Vec3d ep = e.getPos().add(0, e.getHeight() / 2, 0);
			double along = ep.subtract(start).dotProduct(dir);
			if (along < 0 || along > len) continue;
			double dist = ep.subtract(start.add(dir.multiply(along))).length();
			if (dist > width + Math.max(e.getWidth(), e.getHeight()) * 0.5) continue;
			if (gravity) {
				e.damage(ds, dmg);
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 30, 3));
				Vec3d pull = start.subtract(e.getPos()).normalize().multiply(0.3);
				e.addVelocity(pull.x, 0.35, pull.z);
				e.velocityModified = true;
			} else {
				e.damage(ds, dmg);
				e.setOnFireFor(8);
			}
		}
		if (hitBlock) {
			if (gravity) {
				fx(w, ParticleTypes.REVERSE_PORTAL, stop.x, stop.y + 1, stop.z, 25 + (int) width * 3, width, width, width, 0.15);
				fx(w, GOLD, stop.x, stop.y + 1, stop.z, 8 + (int) width, width * 0.8, width * 0.8, width * 0.8, 0);
			} else {
				double sp = Math.max(1.5, width * 0.7);
				fx(w, ParticleTypes.EXPLOSION, stop.x, stop.y, stop.z, 2 + (int) width / 3, sp, sp * 0.7, sp, 0);
				fx(w, ParticleTypes.FLAME, stop.x, stop.y, stop.z, 15 + (int) width * 2, sp, sp * 0.7, sp, 0.1);
				if (tick % 6 == 0) w.createExplosion(src, stop.x, stop.y, stop.z, (float) Math.max(3, Math.min(8, width * 0.35)), World.ExplosionSourceType.MOB);
			}
		}
	}

	// ---------------------------------------------------------------- ATOMIC EXPLOSION
	public static void atomicNuke(LivingEntity src, Supplier<Vec3d> center, int charge, float power, double radius, float dmg) {
		ServerWorld w = (ServerWorld) src.getWorld();
		double sc = Math.max(1.0, src.getHeight() / 12.0);
		sound(w, src.getPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 14f, 0.4f);
		schedule(t -> {
			if (!src.isAlive()) return true;
			if (t < charge) {
				double prog = (double) t / charge;
				Vec3d b = src.getPos();
				double ry = b.y + prog * src.getHeight() * 0.7;
				for (int i = 0; i < 10; i++) {
					double a = t * 0.5 + i * Math.PI / 5;
					double rr = ((1 - prog) * 9 + 1.2) * sc;
					fx(w, BLUE, b.x + Math.cos(a) * rr, ry, b.z + Math.sin(a) * rr, (int) Math.max(1, sc), sc * 0.3, sc * 0.3, sc * 0.3, 0);
				}
				double sp = (3 * (1 - prog) + 0.5) * sc;
				fx(w, ParticleTypes.ELECTRIC_SPARK, b.x, ry, b.z, (int) (10 * Math.min(sc, 4)), sp, 1.5 * sc, sp, 0.4);
				if (t % 8 == 0) sound(w, b, SoundEvents.BLOCK_BEACON_POWER_SELECT, 10f, 0.5f + (float) prog);
				return false;
			}
			detonate(w, src, center.get(), radius, power, dmg, sc);
			return true;
		});
	}

	public static void detonate(ServerWorld w, LivingEntity src, Vec3d c, double radius, float power, float dmg, double sc) {
		w.createExplosion(src, c.x, c.y + 1, c.z, Math.min(power, 20f), World.ExplosionSourceType.MOB);
		sound(w, c, SoundEvents.ENTITY_GENERIC_EXPLODE, 30f, 0.4f);
		sound(w, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 30f, 0.4f);
		fx(w, ParticleTypes.FLASH, c.x, c.y + 3 * sc, c.z, 6, 3 * sc, 2 * sc, 3 * sc, 0);
		fx(w, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 2 * sc, c.z, 4 + (int) sc * 2, 4 * sc, sc, 4 * sc, 0);
		for (double r = 3 * sc; r <= radius; r += 3 * sc) {
			int n = (int) (r * 1.5);
			for (int i = 0; i < n; i++) {
				double a = Math.PI * 2 * i / n;
				fx(w, ParticleTypes.CLOUD, c.x + Math.cos(a) * r, c.y + 1, c.z + Math.sin(a) * r, 1, 0, 0.2 * sc, 0, 0.08);
			}
		}
		for (double y = 0; y < 36 * sc; y += 2 * sc) {
			fx(w, ParticleTypes.FLAME, c.x, c.y + y, c.z, 8, 1.3 * sc, sc, 1.3 * sc, 0.06);
			fx(w, ParticleTypes.LARGE_SMOKE, c.x, c.y + y, c.z, 6, 1.6 * sc, sc, 1.6 * sc, 0.03);
			fx(w, BLUE, c.x, c.y + y, c.z, 3, sc, sc, sc, 0);
		}
		for (int i = 0; i < 64; i++) {
			double a = Math.PI * 2 * i / 64;
			double cap = (9 + RNG.nextDouble() * 3) * sc;
			double cy = c.y + 36 * sc + RNG.nextDouble() * 3 * sc;
			fx(w, ParticleTypes.LARGE_SMOKE, c.x + Math.cos(a) * cap, cy, c.z + Math.sin(a) * cap, 2, sc, sc, sc, 0.02);
			fx(w, ParticleTypes.FLAME, c.x + Math.cos(a) * cap * 0.8, c.y + 36 * sc, c.z + Math.sin(a) * cap * 0.8, 1, 0.5 * sc, 0.5 * sc, 0.5 * sc, 0.03);
		}
		DamageSource ds = src instanceof PlayerEntity pe ? w.getDamageSources().playerAttack(pe) : w.getDamageSources().mobAttack(src);
		Box box = new Box(c, c).expand(radius);
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> x != src && x.isAlive())) {
			double d = e.getPos().distanceTo(c);
			if (d > radius) continue;
			float f = (float) (1 - d / radius);
			e.damage(ds, Math.max(4f, dmg * f));
			e.setOnFireFor(6);
			Vec3d push = e.getPos().subtract(c).normalize().multiply(1.6 * f + 0.4);
			e.addVelocity(push.x, 0.8 * f + 0.2, push.z);
			e.velocityModified = true;
		}
	}

	// ---------------------------------------------------------------- LIGHTNING STORM
	public static void lightningStorm(LivingEntity src, Supplier<Vec3d> center, double R, double cloudY, int dur) {
		ServerWorld w = (ServerWorld) src.getWorld();
		double ss = Math.max(1.0, R / 24.0);
		int bolts = Math.max(1, (int) (R / 40));
		w.setWeather(0, dur + 400, true, true);
		sound(w, center.get(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 30f, 0.6f);
		schedule(t -> {
			if (!src.isAlive() || t >= dur) return true;
			Vec3d c = center.get();
			for (int i = 0; i < 14; i++) {
				double a = RNG.nextDouble() * Math.PI * 2;
				double r = R + RNG.nextGaussian() * 2.5 * ss;
				double x = c.x + Math.cos(a) * r, y = c.y + cloudY + RNG.nextGaussian() * 1.5 * ss, z = c.z + Math.sin(a) * r;
				fx(w, ParticleTypes.LARGE_SMOKE, x, y, z, Math.max(1, (int) ss), ss, 0.6 * ss, ss, 0.02);
				if (i % 2 == 0) fx(w, ParticleTypes.ELECTRIC_SPARK, x, y, z, 2 * Math.max(1, (int) ss), 1.5 * ss, ss, 1.5 * ss, 0.3);
			}
			if (t % 3 == 0) {
				for (int k = 0; k < bolts; k++) {
					double a = RNG.nextDouble() * Math.PI * 2;
					double r = Math.sqrt(RNG.nextDouble()) * R;
					double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
					boolean safe = !(src instanceof PlayerEntity) || Math.hypot(x - src.getX(), z - src.getZ()) > 6;
					BlockPos bp = BlockPos.ofFloored(x, c.y, z);
					if (safe && w.isChunkLoaded(bp)) {
						BlockPos top = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING, bp);
						LightningEntity b = EntityType.LIGHTNING_BOLT.create(w);
						if (b != null) { b.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(top)); w.spawnEntity(b); }
					}
				}
			}
			return false;
		});
	}
}
