package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Sanglier Cornu : bête enragée qui charge les joueurs. */
public class BoarEntity extends HoglinEntity {
	private int chargeCooldown = 60;
	private int windup;
	private PlayerEntity chargeTarget;

	public BoarEntity(EntityType<? extends HoglinEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder createBoarAttributes() {
		return HoglinEntity.createHoglinAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 34.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
	}

	@Override
	public boolean canConvert() {
		return false;
	}

	@Nullable
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
		setBaby(false);
		return data;
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
			// gratte le sol avant de charger
			world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 2, 0.4, 0.0, 0.4, 0.01);
			if (chargeTarget != null) {
				getLookControl().lookAt(chargeTarget, 60f, 60f);
			}
			if (windup == 0 && chargeTarget != null && chargeTarget.isAlive()) {
				Vec3d dir = chargeTarget.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
				setVelocity(dir.x * 1.4, 0.2, dir.z * 1.4);
				velocityModified = true;
				world.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 12, 0.5, 0.1, 0.5, 0.02);
			}
			return;
		}
		if (--chargeCooldown <= 0) {
			PlayerEntity target = world.getClosestPlayer(this, 12);
			if (target != null && !target.isCreative() && !target.isSpectator() && squaredDistanceTo(target) > 9 && canSee(target)) {
				chargeCooldown = 100;
				windup = 14;
				chargeTarget = target;
				world.sendEntityStatus(this, (byte) 4);
				world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_HOGLIN_ANGRY, SoundCategory.HOSTILE, 1.2f, 0.8f);
			} else {
				chargeCooldown = 20;
			}
		}
	}
}
