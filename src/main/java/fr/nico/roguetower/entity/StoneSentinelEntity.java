package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Sentinelle de Pierre : boss colossal qui fait trembler le sol. */
public class StoneSentinelEntity extends IronGolemEntity {
	private int quakeCooldown = 100;
	private int windup;

	public StoneSentinelEntity(EntityType<? extends IronGolemEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 60;
	}

	public static DefaultAttributeContainer.Builder createSentinelAttributes() {
		return IronGolemEntity.createIronGolemAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 140.0)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10f));
		this.goalSelector.add(8, new LookAroundGoal(this));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || !isAlive()) {
			return;
		}
		if (windup > 0) {
			windup--;
			getNavigation().stop();
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.6, getZ(), 3, 0.6, 0.2, 0.6, 0.01);
			if (windup == 0) {
				quake(world);
			}
			return;
		}
		if (getTarget() != null && squaredDistanceTo(getTarget()) < 49 && --quakeCooldown <= 0) {
			quakeCooldown = 160;
			windup = 22;
			world.sendEntityStatus(this, (byte) 4);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.0f, 1.3f);
		}
	}

	private void quake(ServerWorld world) {
		{
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.2f, 0.5f);
			for (int ring = 1; ring <= 6; ring++) {
				for (int i = 0; i < ring * 6; i++) {
					double a = i * Math.PI * 2 / (ring * 6);
					world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + Math.cos(a) * ring, getY() + 0.1, getZ() + Math.sin(a) * ring, 1, 0, 0.05, 0, 0.0);
				}
			}
			for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(7), p -> !p.isCreative() && !p.isSpectator() && p.isOnGround())) {
				p.damage(getDamageSources().mobAttack(this), 8f);
				p.setVelocity(p.getVelocity().add(0, 1.0, 0));
				p.velocityModified = true;
			}
		}
	}
}
