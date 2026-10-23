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

/** Mantelame : insecte à faux, rapide. Lève ses lames puis tranche en arc devant lui. */
public class BladeMantisEntity extends RogueMob {
	public BladeMantisEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
	}

	@Override
	protected void initGoals() {
		addStandardGoals(true, 1.2);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		if (squaredDistanceTo(target) < 9) {
			specialCooldown = 70;
			startAction(ACTION_WINDUP, 11);
			sfx(SoundEvents.ENTITY_SPIDER_AMBIENT, 1f, 1.8f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		startAction(ACTION_STRIKE, 6);
		sfx(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.3f);
		Vec3d look = getRotationVector().multiply(1, 0, 1).normalize();
		Vec3d center = getPos().add(look.multiply(1.5));
		world.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, getBodyY(0.6), center.z, 2, 0.5, 0.1, 0.5, 0);
		for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(3), this::canHurt)) {
			Vec3d to = p.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
			if (to.dotProduct(look) > 0.3 && squaredDistanceTo(p) < 9.5) {
				p.damage(getDamageSources().mobAttack(this), 7f);
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
		return 0.5f;
	}
}
