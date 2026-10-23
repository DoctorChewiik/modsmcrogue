package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Gargouille de Braise : tourne au-dessus du joueur, déploie ses ailes puis pique en flammes. */
public class EmberGargoyleEntity extends FlyingRogueMob {
	public EmberGargoyleEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected Vec3d hoverPosition(LivingEntity target) {
		double a = age * 0.03 + getId();
		return target.getPos().add(Math.cos(a) * 5, 4.5, Math.sin(a) * 5);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		if (squaredDistanceTo(target) < 12 * 12 && canSee(target)) {
			specialCooldown = 80;
			startAction(ACTION_WINDUP, 16);
			playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 1f, 0.7f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		startAction(ACTION_STRIKE, 22);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && isAlive()) {
			if (isWindingUp()) {
				world.spawnParticles(ParticleTypes.FLAME, getX(), getBodyY(0.5), getZ(), 3, 0.5, 0.3, 0.5, 0.01);
			}
			LivingEntity target = getTarget();
			if (isStriking() && target != null) {
				Vec3d dir = target.getPos().add(0, 0.5, 0).subtract(getPos()).normalize();
				setVelocity(dir.multiply(0.85));
				world.spawnParticles(ParticleTypes.FLAME, getX(), getBodyY(0.5), getZ(), 2, 0.2, 0.2, 0.2, 0.0);
				for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(0.6), this::canHurt)) {
					if (p.damage(getDamageSources().mobAttack(this), 6f)) {
						p.setOnFireFor(3);
					}
					startAction(ACTION_NONE, 1);
					setVelocity(getVelocity().multiply(-0.5).add(0, 0.6, 0));
				}
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_PHANTOM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_BASALT_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_BASALT_BREAK;
	}

	@Override
	public float getSoundPitch() {
		return 0.6f;
	}
}
