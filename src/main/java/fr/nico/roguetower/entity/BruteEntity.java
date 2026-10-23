package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Brute des Profondeurs : géant lent qui frappe le sol et projette les joueurs. */
public class BruteEntity extends ZombieEntity {
	private static final TrackedData<Integer> WINDUP = DataTracker.registerData(BruteEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int WINDUP_TICKS = 16;
	private int slamCooldown = 60;

	public BruteEntity(EntityType<? extends ZombieEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 20;
	}

	public static DefaultAttributeContainer.Builder createBruteAttributes() {
		return ZombieEntity.createZombieAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
			.add(EntityAttributes.GENERIC_ARMOR, 6.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(WINDUP, 0);
	}

	/** Ticks restants avant le coup au sol (> 0 = bras levés). */
	public int getWindup() {
		return dataTracker.get(WINDUP);
	}

	@Override
	public boolean isBaby() {
		return false;
	}

	@Override
	protected boolean burnsInDaylight() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || !isAlive()) {
			return;
		}
		int windup = getWindup();
		if (windup > 0) {
			getNavigation().stop();
			dataTracker.set(WINDUP, windup - 1);
			// cercle d'avertissement au sol
			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI / 8;
				world.spawnParticles(ParticleTypes.CRIT, getX() + Math.cos(a) * 4.5, getY() + 0.1, getZ() + Math.sin(a) * 4.5, 1, 0, 0, 0, 0);
			}
			if (windup == 1) {
				slam(world);
			}
		} else if (slamCooldown > 0) {
			slamCooldown--;
		} else if (getTarget() != null && squaredDistanceTo(getTarget()) < 4.0 * 4.0) {
			slamCooldown = 110;
			dataTracker.set(WINDUP, WINDUP_TICKS);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.2f, 0.7f);
		}
	}

	private void slam(ServerWorld world) {
		world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, 2.0, 0.1, 2.0, 0.0);
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.0f, 0.6f);
		for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(4.5), p -> !p.isCreative() && !p.isSpectator())) {
			if (p.damage(getDamageSources().mobAttack(this), 7f)) {
				p.setVelocity(p.getVelocity().add(0, 0.8, 0));
				p.velocityModified = true;
			}
		}
	}
}
