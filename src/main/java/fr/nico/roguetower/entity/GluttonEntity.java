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

/** Glouton : sac gélatineux à énorme gueule. Ouvre grand la bouche puis bondit pour engloutir. */
public class GluttonEntity extends RogueMob {
	public GluttonEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
	}

	@Override
	protected void initGoals() {
		addStandardGoals(true, 1.1);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!getWorld().isClient && isOnGround() && getVelocity().horizontalLengthSquared() > 0.002 && random.nextInt(8) == 0) {
			jump();
		}
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		double d = squaredDistanceTo(target);
		if (d > 6 && d < 49 && canSee(target)) {
			specialCooldown = 90;
			startAction(ACTION_WINDUP, 14);
			sfx(SoundEvents.ENTITY_RAVAGER_ROAR, 0.7f, 1.8f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		LivingEntity target = getTarget();
		if (target != null) {
			Vec3d dir = target.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
			setVelocity(dir.x * 1.2, 0.45, dir.z * 1.2);
			velocityModified = true;
		}
		startAction(ACTION_STRIKE, 10);
		sfx(SoundEvents.ENTITY_GENERIC_EAT, 1.2f, 0.6f);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && isStriking()) {
			for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(0.6), this::canHurt)) {
				if (p.damage(getDamageSources().mobAttack(this), 7f)) {
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 100, 1), this);
					startAction(ACTION_NONE, 1);
				}
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SLIME_SQUISH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_HONEY_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_SLIME_BLOCK_BREAK;
	}

	@Override
	public float getSoundPitch() {
		return 0.6f + random.nextFloat() * 0.2f;
	}
}
