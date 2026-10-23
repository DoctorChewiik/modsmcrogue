package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Rampecroc : mille-pattes cuirassé. Se cabre puis bondit pour une morsure venimeuse. */
public class CrawlerEntity extends RogueMob {
	public CrawlerEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 7;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected void initGoals() {
		addStandardGoals(true, 1.2);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		double d = squaredDistanceTo(target);
		if (d > 6 && d < 64 && isOnGround()) {
			specialCooldown = 80;
			startAction(ACTION_WINDUP, 9);
			playSound(SoundEvents.ENTITY_SILVERFISH_AMBIENT, 1f, 0.6f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		LivingEntity target = getTarget();
		if (target != null) {
			Vec3d dir = target.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
			setVelocity(dir.x * 1.1, 0.4, dir.z * 1.1);
			velocityModified = true;
		}
		startAction(ACTION_STRIKE, 10);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && isStriking()) {
			for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(0.5), this::canHurt)) {
				if (p.damage(getDamageSources().mobAttack(this), 4f)) {
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 0), this);
					startAction(ACTION_NONE, 1);
				}
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SILVERFISH_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return 0.4f;
	}
}
