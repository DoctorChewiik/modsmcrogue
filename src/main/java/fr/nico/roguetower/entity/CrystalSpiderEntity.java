package fr.nico.roguetower.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Araignée de Cristal : petite et rapide, gèle à la morsure et éclate en éclats tranchants. */
public class CrystalSpiderEntity extends SpiderEntity {
	private int pounceCooldown = 40;
	private int windup;

	public CrystalSpiderEntity(EntityType<? extends SpiderEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createCrystalSpiderAttributes() {
		return SpiderEntity.createSpiderAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || !isAlive()) {
			return;
		}
		PlayerEntity target = world.getClosestPlayer(this, 9);
		if (windup > 0) {
			windup--;
			getNavigation().stop();
			world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 1, 0.3, 0.1, 0.3, 0.0);
			if (windup == 0 && target != null) {
				Vec3d dir = target.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
				setVelocity(dir.x * 1.0, 0.55, dir.z * 1.0);
				velocityModified = true;
				world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 1f, 1.6f);
			}
			return;
		}
		if (--pounceCooldown <= 0 && target != null && !target.isCreative() && !target.isSpectator() && isOnGround()) {
			double d = squaredDistanceTo(target);
			if (d > 9 && d < 64) {
				pounceCooldown = 80 + random.nextInt(40);
				windup = 10;
			} else {
				pounceCooldown = 10;
			}
		}
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity living) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
			living.setFrozenTicks(Math.min(living.getFrozenTicks() + 80, 300));
		}
		return hit;
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld world) {
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.getDefaultState()),
				getX(), getY() + 0.4, getZ(), 40, 0.8, 0.4, 0.8, 0.2);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.HOSTILE, 1.2f, 1f);
			for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(2.5), p -> !p.isCreative() && !p.isSpectator())) {
				p.damage(getDamageSources().thorns(this), 3f);
			}
		}
	}
}
