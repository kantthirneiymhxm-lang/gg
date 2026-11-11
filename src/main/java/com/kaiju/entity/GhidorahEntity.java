package com.kaiju.entity;

import com.kaiju.Abilities;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import com.kaiju.KaijuMod;

/**
 * King Ghidorah life cycle:
 * METEOR (falls from the sky as a fireball) -> WOUNDED (one head, crater) -> ASCEND (flies up into the sky)
 * -> HIDDEN (thunder in the clouds) -> RETURN (comes back, two more heads grow) -> FULL (3 heads, full power).
 */
public class GhidorahEntity extends HostileEntity {
	public static final int METEOR = 0, WOUNDED = 1, ASCEND = 2, HIDDEN = 3, RETURN = 4, FULL = 5;
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(GhidorahEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> GROW = DataTracker.registerData(GhidorahEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final double R = 150;
	private static final int STORM = 480;
	private int cdGrav = 60, cdStorm = 400, busy = 0, stormTicks = 0, timer = 0;
	private double ascendY;
	private Vec3d stormCenter = Vec3d.ZERO, wander = null, impactPos = null, meteorVel = null, returnCenter = null;

	public GhidorahEntity(EntityType<? extends GhidorahEntity> type, World world) {
		super(type, world);
		this.setNoGravity(true);
		this.setPersistent();
		this.experiencePoints = 3000;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 400)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 20)
			.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
			.add(EntityAttributes.GENERIC_FLYING_SPEED, 1.0)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 600)
			.add(EntityAttributes.GENERIC_ARMOR, 8);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(PHASE, FULL);
		dataTracker.startTracking(GROW, 1f);
	}

	public int getPhase() { return dataTracker.get(PHASE); }
	private void setPhase(int p) { dataTracker.set(PHASE, p); }
	/** 0 = only the centre head, 1 = all three heads fully grown. */
	public float getGrow() { return dataTracker.get(GROW); }
	private void setGrow(float g) { dataTracker.set(GROW, g); }

	/** Spawn King Ghidorah as a meteor that crashes onto (x, groundY, z). */
	public static GhidorahEntity spawnMeteor(ServerWorld w, double x, double groundY, double z) {
		GhidorahEntity e = KaijuMod.GHIDORAH.create(w);
		if (e == null) return null;
		double a = w.random.nextDouble() * 6.283;
		double sx = x + Math.cos(a) * 80, sz = z + Math.sin(a) * 80, sy = Math.min(groundY + 420, 700);
		e.refreshPositionAndAngles(sx, sy, sz, 0f, 0f);
		e.impactPos = new Vec3d(x, groundY, z);
		e.meteorVel = e.impactPos.subtract(sx, sy, sz).multiply(1.0 / 100);
		e.setPhase(METEOR);
		e.setGrow(0f);
		e.setInvisible(true);
		e.setInvulnerable(true);
		w.spawnEntity(e);
		return e;
	}

	@Override
	protected void initGoals() {
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, GodzillaEntity.class, 10, false, false, null));
		targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	private Vec3d headPos(int off) {
		Vec3d f = Vec3d.fromPolar(0, getYaw());
		Vec3d r = new Vec3d(-f.z, 0, f.x);
		return getPos().add(0, 115 + Math.abs(off) * 10, 0).add(f.multiply(125)).add(r.multiply(off * 55));
	}

	private void steer(Vec3d dest, double maxSpeed) {
		Vec3d v = dest.subtract(getPos());
		double len = v.length();
		if (len > 0.5) v = v.normalize().multiply(Math.min(maxSpeed, len * 0.05)); else v = Vec3d.ZERO;
		setVelocity(getVelocity().multiply(0.85).add(v.multiply(0.5)));
	}

	private void faceYaw(Vec3d dir) {
		if (dir.horizontalLengthSquared() < 0.01) return;
		float yaw = (float) (MathHelper.atan2(dir.z, dir.x) * 57.29578) - 90f;
		setYaw(yaw);
		bodyYaw = yaw;
		headYaw = yaw;
	}

	private void strike(ServerWorld w, double x, double z) {
		BlockPos bp = BlockPos.ofFloored(x, 64, z);
		if (!w.isChunkLoaded(bp)) return;
		BlockPos top = w.getTopPosition(Heightmap.Type.MOTION_BLOCKING, bp);
		LightningEntity b = EntityType.LIGHTNING_BOLT.create(w);
		if (b != null) { b.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(top)); w.spawnEntity(b); }
	}

	private void announce(ServerWorld w, String key, Formatting f) {
		for (ServerPlayerEntity p : w.getPlayers())
			if (Math.hypot(p.getX() - getX(), p.getZ() - getZ()) < 1500)
				p.sendMessage(Text.translatable(key).formatted(f, Formatting.BOLD), false);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) return;
		ServerWorld w = (ServerWorld) getWorld();
		switch (getPhase()) {
			case METEOR -> { meteorTick(w); return; }
			case ASCEND -> { ascendTick(w); return; }
			case HIDDEN -> { hiddenTick(w); return; }
			case RETURN -> { returnTick(w); return; }
			default -> { }
		}
		combat(w, getPhase() == FULL);
	}

	// ------------------------------------------------------------ 1. METEOR
	private void meteorTick(ServerWorld w) {
		if (impactPos == null || meteorVel == null) { // reloaded mid-fall: just land
			setInvisible(false); setInvulnerable(false); setPhase(WOUNDED); setGrow(0f); return;
		}
		timer++;
		setVelocity(meteorVel);
		Vec3d c = getPos().add(0, 68, 0);
		Vec3d back = meteorVel.normalize().multiply(-1);
		Abilities.fx(w, ParticleTypes.FLAME, c.x, c.y, c.z, 150, 30, 30, 30, 0.05);
		Abilities.fx(w, ParticleTypes.LAVA, c.x, c.y, c.z, 40, 30, 30, 30, 0);
		Abilities.fx(w, ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 12, 12, 12, 0.1);
		Abilities.fx(w, ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 30, 30, 30, 0.05);
		for (int k = 1; k <= 14; k++) {
			Vec3d p = c.add(back.multiply(k * 35));
			Abilities.fx(w, ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 25, 15 + k * 3, 15 + k * 3, 15 + k * 3, 0.02);
			Abilities.fx(w, ParticleTypes.FLAME, p.x, p.y, p.z, 20, 12 + k * 2, 12 + k * 2, 12 + k * 2, 0.04);
		}
		if (timer % 6 == 0) w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 25f, 0.5f);
		if (getY() <= impactPos.y + 4 || timer > 200) {
			Vec3d g = new Vec3d(impactPos.x, impactPos.y, impactPos.z);
			setPosition(g.x, g.y, g.z);
			setVelocity(Vec3d.ZERO);
			Abilities.detonate(w, this, g, 160, 20f, 80f, 6.0);
			for (int i = 0; i < 5; i++) {
				double a = i * 1.2566;
				w.createExplosion(this, g.x + Math.cos(a) * 30, g.y, g.z + Math.sin(a) * 30, 10f, World.ExplosionSourceType.MOB);
			}
			setInvisible(false);
			setInvulnerable(false);
			setPhase(WOUNDED);
			timer = 0;
			cdGrav = 100;
		}
	}

	// ------------------------------------------------------------ 3. ASCEND / HIDDEN / RETURN
	private void startAscend(ServerWorld w) {
		setPhase(ASCEND);
		timer = 0;
		busy = 0;
		stormTicks = 0;
		ascendY = Math.min(getY() + 450, 700);
		setInvulnerable(true);
		Abilities.sound(w, getPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 30f, 0.5f);
		announce(w, "msg.kaiju_kings.ghidorah_ascend", Formatting.GOLD);
	}

	private void ascendTick(ServerWorld w) {
		timer++;
		setVelocity(0, 6, 0);
		Abilities.fx(w, ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 60, getZ(), 60, 50, 60, 50, 0.6);
		Abilities.fx(w, ParticleTypes.CLOUD, getX(), getY(), getZ(), 40, 40, 10, 40, 0.2);
		if (timer % 8 == 0) strike(w, getX() + (random.nextDouble() - 0.5) * 160, getZ() + (random.nextDouble() - 0.5) * 160);
		if (getY() >= ascendY || timer > 160) {
			setPhase(HIDDEN);
			timer = 0;
			setInvisible(true);
			setVelocity(Vec3d.ZERO);
		}
	}

	private void hiddenTick(ServerWorld w) {
		timer++;
		setVelocity(Vec3d.ZERO);
		if (timer == 1) w.setWeather(0, 1600, true, true);
		if (timer % 25 == 0) {
			PlayerEntity p = w.getClosestPlayer(this, 2000.0);
			if (p != null) {
				strike(w, p.getX() + (random.nextDouble() - 0.5) * 160, p.getZ() + (random.nextDouble() - 0.5) * 160);
				w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 30f, 0.5f);
				if (timer % 50 == 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 20f, 0.4f);
			}
		}
		if (timer >= 240) beginReturn(w);
	}

	private void beginReturn(ServerWorld w) {
		PlayerEntity p = w.getClosestPlayer(this, 2000.0);
		Vec3d center = p != null ? p.getPos() : getPos().subtract(0, 380, 0);
		double a = random.nextDouble() * 6.283;
		double sx = center.x + Math.cos(a) * 100, sz = center.z + Math.sin(a) * 100, sy = Math.min(center.y + 420, 700);
		refreshPositionAfterTeleport(sx, sy, sz);
		setInvisible(false);
		setInvulnerable(false);
		setHealth(getMaxHealth());
		setGrow(0f);
		returnCenter = center;
		setPhase(RETURN);
		timer = 0;
		Abilities.fx(w, ParticleTypes.FLASH, sx, sy + 60, sz, 8, 60, 60, 60, 0);
		Abilities.sound(w, new Vec3d(sx, sy, sz), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 30f, 0.4f);
		Abilities.sound(w, new Vec3d(sx, sy, sz), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 30f, 0.5f);
		announce(w, "msg.kaiju_kings.ghidorah_return", Formatting.GOLD);
	}

	private void returnTick(ServerWorld w) {
		if (returnCenter == null) returnCenter = getPos().subtract(0, 380, 0);
		timer++;
		setGrow(Math.min(1f, timer / 90f));
		Vec3d dest = returnCenter.add(0, 40, 0);
		Vec3d v = dest.subtract(getPos()).multiply(0.04);
		if (v.length() > 4.5) v = v.normalize().multiply(4.5);
		setVelocity(v);
		faceYaw(returnCenter.subtract(getPos()));
		if (timer < 100) {
			for (int off = -1; off <= 1; off += 2) {
				Vec3d hp = headPos(off);
				Abilities.fx(w, ParticleTypes.ELECTRIC_SPARK, hp.x, hp.y, hp.z, 40, 25, 25, 25, 0.5);
				Abilities.fx(w, ParticleTypes.END_ROD, hp.x, hp.y, hp.z, 15, 15, 15, 15, 0.1);
			}
		}
		if (timer == 45 || timer == 90) {
			Abilities.sound(w, getPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 30f, 0.6f);
			Abilities.sound(w, getPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 30f, 0.6f);
		}
		if (timer >= 110) {
			setGrow(1f);
			setPhase(FULL);
			timer = 0;
			cdStorm = 0;
			cdGrav = 0;
			wander = null;
			for (int i = 0; i < 8; i++) {
				double a = i * Math.PI / 4;
				strike(w, returnCenter.x + Math.cos(a) * 70, returnCenter.z + Math.sin(a) * 70);
			}
			Abilities.fx(w, ParticleTypes.FLASH, getX(), getY() + 90, getZ(), 8, 60, 60, 60, 0);
		}
	}

	// ------------------------------------------------------------ 2 & 5. COMBAT
	private void combat(ServerWorld w, boolean full) {
		if (cdGrav > 0) cdGrav--;
		if (cdStorm > 0) cdStorm--;
		if (busy > 0) busy--;
		if (!full) {
			timer++;
			if (timer > 1500 || getHealth() < getMaxHealth() * 0.7f) { startAscend(w); return; }
		}
		LivingEntity t = getTarget();
		boolean hasT = t != null && t.isAlive();

		if (stormTicks > 0) {
			stormTicks--;
			if (hasT) stormCenter = t.getPos();
			double ang = (STORM - stormTicks) * 0.0135;
			Vec3d dest = stormCenter.add(Math.cos(ang) * R, 40, Math.sin(ang) * R);
			Vec3d v = dest.subtract(getPos()).multiply(0.35);
			if (v.length() > 5.0) v = v.normalize().multiply(5.0);
			setVelocity(v);
			faceYaw(v);
			Abilities.fx(w, ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 60, getZ(), 50, 40, 30, 40, 0.6);
			Abilities.fx(w, ParticleTypes.CLOUD, getX(), getY() + 40, getZ(), 15, 40, 15, 40, 0.02);
		} else if (hasT) {
			Vec3d away = getPos().subtract(t.getPos());
			away = new Vec3d(away.x, 0, away.z);
			if (away.lengthSquared() < 1) away = new Vec3d(1, 0, 0);
			steer(t.getPos().add(away.normalize().multiply(full ? 300 : 200)).add(0, full ? 40 : 6, 0), full ? 2.5 : 1.0);
			faceYaw(t.getPos().subtract(getPos()));
			double d = distanceTo(t);
			if (busy <= 0) {
				if (full && cdStorm <= 0 && d < 500) {
					stormTicks = STORM;
					cdStorm = 1000;
					busy = STORM + 5;
					stormCenter = t.getPos();
					Abilities.lightningStorm(this, () -> stormCenter, R, 100, STORM);
				} else if (cdGrav <= 0 && d < 550 && canSee(t)) {
					cdGrav = full ? 240 : 320;
					busy = 75;
					int from = full ? -1 : 0, to = full ? 1 : 0;
					for (int i = from; i <= to; i++) {
						final int off = i;
						final Vec3d[] last = {null};
						Abilities.breath(this, () -> headPos(off), () -> {
							Vec3d o = headPos(off);
							if (t.isAlive() || last[0] == null) last[0] = t.getEyePos().subtract(o);
							return last[0];
						}, 600, 14.0, 10f, 15, 55, true);
					}
				}
			}
		} else {
			double alt = full ? 50 : 12;
			if (wander == null || squaredDistanceTo(wander) < 2500 || age % 300 == 0) {
				double a = random.nextDouble() * 6.283, r = 100 + random.nextDouble() * 150;
				double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
				BlockPos bp = BlockPos.ofFloored(x, getY(), z);
				double gy = w.isChunkLoaded(bp) ? w.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) x, (int) z) : getY() - alt;
				wander = new Vec3d(x, Math.min(gy + alt, 330), z);
			}
			steer(wander, full ? 1.5 : 0.8);
			faceYaw(getVelocity());
		}
	}

	// ------------------------------------------------------------ misc
	@Override public void move(MovementType type, Vec3d mv) {
		if (type == MovementType.SELF) setPosition(getX() + mv.x, getY() + mv.y, getZ() + mv.z);
		else super.move(type, mv);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound n) {
		super.writeCustomDataToNbt(n);
		n.putInt("KPhase", getPhase());
		n.putInt("KTimer", timer);
		n.putFloat("KGrow", getGrow());
		n.putDouble("KAscY", ascendY);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound n) {
		super.readCustomDataFromNbt(n);
		if (!n.contains("KPhase")) return;
		int p = n.getInt("KPhase");
		timer = n.getInt("KTimer");
		ascendY = n.getDouble("KAscY");
		setGrow(n.getFloat("KGrow"));
		if (p == METEOR) { p = WOUNDED; setInvisible(false); setInvulnerable(false); setGrow(0f); }
		setPhase(p);
	}

	@Override public void onDeath(DamageSource source) { setNoGravity(false); super.onDeath(source); }
	@Override public boolean handleFallDamage(float fall, float mult, DamageSource src) { return false; }
	@Override public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() == this) return false;
		return super.damage(source, amount);
	}
	@Override protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dims) { return 86f; }
	@Override public boolean canImmediatelyDespawn(double d) { return false; }
	@Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT; }
	@Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_ENDER_DRAGON_HURT; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ENDER_DRAGON_DEATH; }
	@Override protected float getSoundVolume() { return 10f; }
}
