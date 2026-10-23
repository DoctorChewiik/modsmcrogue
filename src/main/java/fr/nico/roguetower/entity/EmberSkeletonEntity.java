package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Squelette Ardent : ses flèches sont remplacées par des boules de feu. */
public class EmberSkeletonEntity extends SkeletonEntity {
	private int novaCooldown = 60;
	private int windup;

	public EmberSkeletonEntity(EntityType<? extends SkeletonEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder createEmberSkeletonAttributes() {
		return AbstractSkeletonEntity.createAbstractSkeletonAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0);
	}

	@Override
	protected boolean isAffectedByDaylight() {
		return false;
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		Vec3d dir = new Vec3d(target.getX() - getX(), target.getBodyY(0.5) - getBodyY(0.5), target.getZ() - getZ()).normalize();
		SmallFireballEntity fireball = new SmallFireballEntity(getWorld(), this, dir);
		fireball.setPosition(getX() + dir.x, getBodyY(0.5) + 0.3, getZ() + dir.z);
		getWorld().spawnEntity(fireball);
		getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.2f);
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
			world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 4, 0.4, 0.6, 0.4, 0.02);
			if (windup == 0) {
				// Nova de flammes puis saut en arrière
				for (int i = 0; i < 32; i++) {
					double a = i * Math.PI / 16;
					world.spawnParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * 3, getY() + 0.3, getZ() + Math.sin(a) * 3, 2, 0.1, 0.1, 0.1, 0.01);
				}
				world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1f, 0.8f);
				for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(3.2), p -> !p.isCreative() && !p.isSpectator())) {
					if (p.damage(getDamageSources().mobAttack(this), 4f)) {
						p.setOnFireFor(3);
					}
				}
				PlayerEntity near = world.getClosestPlayer(this, 6);
				if (near != null) {
					Vec3d away = getPos().subtract(near.getPos()).multiply(1, 0, 1).normalize();
					setVelocity(away.x * 0.9, 0.5, away.z * 0.9);
					velocityModified = true;
				}
			}
			return;
		}
		if (--novaCooldown <= 0) {
			PlayerEntity near = world.getClosestPlayer(this, 3.5);
			if (near != null && !near.isCreative() && !near.isSpectator()) {
				novaCooldown = 160;
				windup = 12;
				world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 1.5f);
			} else {
				novaCooldown = 10;
			}
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getWorld().isClient && random.nextInt(4) == 0) {
			getWorld().addParticle(ParticleTypes.SMALL_FLAME, getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), 0, 0.02, 0);
		}
	}
}
