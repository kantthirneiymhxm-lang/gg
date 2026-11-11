package com.kaiju.entity;

import com.kaiju.Abilities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/** 90 blocks tall. Walks in a straight line (no pathfinding) and smashes through anything in its way. */
public class GodzillaEntity extends HostileEntity {
	private static final TrackedData<Boolean> BREATHING = DataTracker.registerData(GodzillaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private int beamCd = 80, nukeCd = 300, busy = 0;
	private Vec3d wander = null;

	public GodzillaEntity(EntityType<? extends GodzillaEntity> type, World world) {
		super(type, world);
		this.setStepHeight(22f);
		this.setPersistent();
		this.experiencePoints = 3000;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 500)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 40)
			.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 3.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.0)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 600)
			.add(EntityAttributes.GENERIC_ARMOR, 10);
	}

	@Override
	protected void initGoals() {
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, GhidorahEntity.class, 10, false, false,
			e -> e instanceof GhidorahEntity g && (g.getPhase() == GhidorahEntity.WOUNDED || g.getPhase() == GhidorahEntity.FULL)));
		targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, false, false, null));
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(BREATHING, false);
	}

	public boolean isBreathing() { return dataTracker.get(BREATHING); }

	private Vec3d mouth() { return getEyePos().add(getRotationVec(1f).multiply(30)).add(0, -6, 0); }

	private void smash(ServerWorld w) {
		if (!w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) return;
		Vec3d f = Vec3d.fromPolar(0, getYaw()).multiply(18);
		int cx = (int) (getX() + f.x), cz = (int) (getZ() + f.z);
		int y0 = (int) getY() + 23, y1 = (int) getY() + 60;
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int x = cx - 15; x <= cx + 15; x++) for (int z = cz - 15; z <= cz + 15; z++) {
			p.set(x, y0, z);
			if (!w.isChunkLoaded(p)) continue;
			for (int y = y0; y <= y1; y++) {
				p.set(x, y, z);
				BlockState st = w.getBlockState(p);
				if (st.isAir() || st.getHardness(w, p) < 0 || !st.getFluidState().isEmpty()) continue;
				w.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
		Abilities.fx(w, ParticleTypes.EXPLOSION, getX() + f.x, getY() + 40, getZ() + f.z, 6, 12, 15, 12, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) return;
		ServerWorld w = (ServerWorld) getWorld();
		if (beamCd > 0) beamCd--;
		if (nukeCd > 0) nukeCd--;
		boolean b = busy > 0;
		if (dataTracker.get(BREATHING) != b) dataTracker.set(BREATHING, b);
		LivingEntity t = getTarget();
		boolean has = t != null && t.isAlive();
		if (b) {
			busy--;
			if (has) getLookControl().lookAt(t.getX(), t.getEyeY(), t.getZ(), 60f, 60f);
		} else if (has) {
			double d = distanceTo(t);
			getLookControl().lookAt(t, 30f, 30f);
			if (nukeCd <= 0 && d < 240 && canSee(t)) {
				Abilities.atomicNuke(this, this::getPos, 50, 20f, 200, 60f);
				busy = 62;
				nukeCd = 800;
			} else if (beamCd <= 0 && d < 520 && canSee(t)) {
				Abilities.breath(this, this::mouth, () -> t.getEyePos().subtract(mouth()), 525, 22.0, 20f, 20, 60, false);
				busy = 85;
				beamCd = 260;
			} else {
				double hd = Math.hypot(t.getX() - getX(), t.getZ() - getZ());
				double reach = 20 + t.getWidth();
				if (hd > reach * 0.8) getMoveControl().moveTo(t.getX(), t.getY(), t.getZ(), 1.0);
				else if (age % 20 == 0) tryAttack(t);
			}
		} else {
			if (wander == null || squaredDistanceTo(wander) < 400 || age % 600 == 0) {
				double a = random.nextDouble() * 6.283, r = 100 + random.nextDouble() * 150;
				wander = new Vec3d(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r);
			}
			getMoveControl().moveTo(wander.x, wander.y, wander.z, 0.5);
		}
		if (!b && horizontalCollision && age % 10 == 0) smash(w);
		if (age % 14 == 0 && isOnGround() && getVelocity().horizontalLengthSquared() > 1.0E-4) {
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_STEP, getSoundCategory(), 10f, 0.4f);
			Abilities.fx(w, ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 40, 15, 0.5, 15, 0.1);
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(getPos(), getPos()).expand(45, 8, 45),
				x -> x != this && x.isAlive() && !(x instanceof GhidorahEntity))) {
				e.damage(w.getDamageSources().mobAttack(this), 4f);
				e.addVelocity(0, 0.5, 0);
				e.velocityModified = true;
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() == this) return false;
		return super.damage(source, amount);
	}

	@Override public boolean handleFallDamage(float fall, float mult, DamageSource src) { return false; }
	@Override protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dims) { return 78.75f; }
	@Override public boolean canImmediatelyDespawn(double d) { return false; }
	@Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_ENDER_DRAGON_GROWL; }
	@Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_RAVAGER_HURT; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ENDER_DRAGON_DEATH; }
	@Override protected float getSoundVolume() { return 10f; }
}
