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

/** Totem Hurleur : tourelle immobile qui hurle puis tire des esprits. */
public class HowlingTotemEntity extends RogueMob {
	public HowlingTotemEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 34.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
			.add(EntityAttributes.GENERIC_ARMOR, 4.0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(7, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 24f));
		targetSelector.add(1, new net.minecraft.entity.ai.goal.RevengeGoal(this, RogueMob.class));
		targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		if (squaredDistanceTo(target) < 22 * 22 && canSee(target)) {
			specialCooldown = 50;
			startAction(ACTION_WINDUP, 20);
			playSound(SoundEvents.ENTITY_WOLF_HOWL, 0.9f, 0.6f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		LivingEntity target = getTarget();
		if (target == null) {
			return;
		}
		for (int i = -1; i <= 1; i++) {
			shootBolt(world, target, MagicBoltEntity.SPIRIT, 4f, 0.55, i * 12f);
		}
		playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1f, 1.4f);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_WOOD_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_WOOD_BREAK;
	}
}
