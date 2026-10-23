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

/** Œil du Néant : flotte à distance, se contracte puis tire des orbes du néant, et se téléporte. */
public class VoidEyeEntity extends FlyingRogueMob {
	public VoidEyeEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected Vec3d hoverPosition(LivingEntity target) {
		Vec3d away = getPos().subtract(target.getPos()).multiply(1, 0, 1);
		away = away.lengthSquared() < 0.01 ? new Vec3d(1, 0, 0) : away.normalize();
		return target.getPos().add(away.multiply(7)).add(0, 3, 0);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		if (canSee(target)) {
			specialCooldown = 55;
			startAction(ACTION_WINDUP, 16);
			sfx(SoundEvents.BLOCK_BEACON_AMBIENT, 1.2f, 1.8f);
		}
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		LivingEntity target = getTarget();
		if (target == null) {
			return;
		}
		shootBolt(world, target, MagicBoltEntity.VOID, 5f, 0.6, -6f);
		shootBolt(world, target, MagicBoltEntity.VOID, 5f, 0.6, 6f);
		sfx(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);
		if (random.nextInt(3) == 0) {
			double a = random.nextDouble() * Math.PI * 2;
			world.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getBodyY(0.5), getZ(), 30, 0.4, 0.4, 0.4, 0.05);
			teleport(target.getX() + Math.cos(a) * 6, target.getY() + 3, target.getZ() + Math.sin(a) * 6, true);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ENDERMITE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ENDERMITE_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return 0.5f;
	}
}
