package fr.nico.roguetower.entity;

import net.minecraft.entity.AreaEffectCloudEntity;
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

/** Sporelin : champignon marcheur. Gonfle (bien visible) puis éclate en nuage toxique. */
public class SporelingEntity extends RogueMob {
	public SporelingEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
	}

	@Override
	protected void initGoals() {
		addStandardGoals(true, 1.15);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		if (squaredDistanceTo(target) < 6.25) {
			specialCooldown = 1000;
			startAction(ACTION_WINDUP, 24);
			sfx(SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, 1f, 0.7f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getBodyY(0.5), getZ(), 60, 1.5, 0.8, 1.5, 0.05);
		world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getBodyY(0.5), getZ(), 2, 0.3, 0.3, 0.3, 0);
		sfx(SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT, 1.5f, 0.5f);
		for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(3), this::canHurt)) {
			p.damage(getDamageSources().mobAttack(this), 4f);
		}
		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, getX(), getY(), getZ());
		cloud.setOwner(this);
		cloud.setRadius(2.8f);
		cloud.setDuration(80);
		cloud.setRadiusGrowth(-0.02f);
		cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
		world.spawnEntity(cloud);
		discard();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_FUNGUS_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_FUNGUS_BREAK;
	}
}
