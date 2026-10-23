package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Œil du Néant : globe oculaire volant qui se téléporte et tire des boules de feu. */
public class VoidEyeEntity extends BlazeEntity {
	private static final TrackedData<Integer> CHARGE = DataTracker.registerData(VoidEyeEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private int blinkCooldown = 80;

	public VoidEyeEntity(EntityType<? extends BlazeEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 15;
	}

	public static DefaultAttributeContainer.Builder createVoidEyeAttributes() {
		return BlazeEntity.createBlazeAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CHARGE, 0);
	}

	/** Ticks restants avant la téléportation (l'œil se contracte). */
	public int getCharge() {
		return dataTracker.get(CHARGE);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld world && isAlive()) {
			LivingEntity target = getTarget();
			if (target != null && --blinkCooldown == 15) {
				dataTracker.set(CHARGE, 15);
			}
			if (getCharge() > 0) {
				dataTracker.set(CHARGE, getCharge() - 1);
				world.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getBodyY(0.5), getZ(), 3, 0.5, 0.5, 0.5, 0.02);
			}
			if (target != null && blinkCooldown <= 0) {
				blinkCooldown = 100 + random.nextInt(60);
				double angle = random.nextDouble() * Math.PI * 2;
				double x = target.getX() + Math.cos(angle) * 6;
				double z = target.getZ() + Math.sin(angle) * 6;
				world.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getBodyY(0.5), getZ(), 30, 0.4, 0.4, 0.4, 0.05);
				if (teleport(x, target.getY() + 2.5, z, true)) {
					world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 0.6f);
				}
			}
			if (target instanceof PlayerEntity player && age % 60 == 0 && squaredDistanceTo(player) < 100 && canSee(player)) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 40, 0), this);
			}
		}
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0f;
	}
}
