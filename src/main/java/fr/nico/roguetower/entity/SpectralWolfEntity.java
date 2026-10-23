package fr.nico.roguetower.entity;

import fr.nico.roguetower.mob.AnimalAggroGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Loup Spectral : chasse en meute, impossible à apprivoiser. */
public class SpectralWolfEntity extends WolfEntity {
	private int lungeCooldown = 40;
	private int windup;

	public SpectralWolfEntity(EntityType<? extends WolfEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 7;
	}

	public static DefaultAttributeContainer.Builder createSpectralWolfAttributes() {
		return WolfEntity.createWolfAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		this.goalSelector.add(0, new AnimalAggroGoal(this, 4f, 1.3));
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		return ActionResult.PASS;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || !isAlive()) {
			return;
		}
		PlayerEntity target = world.getClosestPlayer(this, 10);
		if (windup > 0) {
			windup--;
			getNavigation().stop();
			if (windup == 0 && target != null) {
				// bond vers le joueur
				Vec3d dir = target.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
				setVelocity(dir.x * 1.1, 0.45, dir.z * 1.1);
				velocityModified = true;
				world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 0.4, getZ(), 8, 0.3, 0.2, 0.3, 0.02);
			}
			return;
		}
		if (--lungeCooldown <= 0 && target != null && !target.isCreative() && !target.isSpectator()) {
			double d = squaredDistanceTo(target);
			if (d > 9 && d < 49 && isOnGround()) {
				lungeCooldown = 70 + random.nextInt(40);
				windup = 8;
				world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.HOSTILE, 1f, 0.7f);
			} else {
				lungeCooldown = 10;
			}
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getWorld().isClient && random.nextInt(3) == 0) {
			getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), 0, 0.01, 0);
		}
	}
}
