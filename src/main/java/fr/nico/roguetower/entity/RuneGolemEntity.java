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

/** Golem Runique (boss) : lève les bras puis frappe le sol, ou charge ses runes et les tire en éventail. */
public class RuneGolemEntity extends RogueMob {
	private boolean slamming;

	public RuneGolemEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 60;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_ARMOR, 8.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initGoals() {
		addStandardGoals(true, 1.0);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		double d = squaredDistanceTo(target);
		if (d < 30) {
			slamming = true;
			specialCooldown = 70;
			startAction(ACTION_WINDUP, 20);
			sfx(SoundEvents.ENTITY_WARDEN_ROAR, 0.8f, 1.5f);
		} else if (d < 22 * 22 && canSee(target)) {
			slamming = false;
			specialCooldown = 90;
			startAction(ACTION_WINDUP, 18);
			sfx(SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.7f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		LivingEntity target = getTarget();
		if (slamming) {
			startAction(ACTION_STRIKE, 8);
			sfx(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.2f, 0.5f);
			for (int ring = 1; ring <= 6; ring++) {
				for (int i = 0; i < ring * 6; i++) {
					double a = i * Math.PI * 2 / (ring * 6);
					world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(a) * ring, getY() + 0.1, getZ() + Math.sin(a) * ring, 1, 0, 0.05, 0, 0.0);
				}
			}
			for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(6), this::canHurt)) {
				if (squaredDistanceTo(p) < 40 && p.damage(getDamageSources().mobAttack(this), 9f)) {
					p.setVelocity(p.getVelocity().add(0, 0.9, 0));
					p.velocityModified = true;
				}
			}
		} else if (target != null) {
			for (int i = -2; i <= 2; i++) {
				shootBolt(world, target, MagicBoltEntity.RUNE, 6f, 0.5, i * 14f);
			}
			sfx(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.2f, 0.6f);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLOCK_DEEPSLATE_STEP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_DEEPSLATE_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_DEEPSLATE_BREAK;
	}

	@Override
	public float getSoundPitch() {
		return 0.5f;
	}
}
