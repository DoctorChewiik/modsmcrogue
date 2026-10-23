package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Monstre volant : plane autour de sa cible au lieu de marcher. */
public abstract class FlyingRogueMob extends RogueMob {
	protected FlyingRogueMob(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 24f));
		targetSelector.add(1, new RevengeGoal(this, RogueMob.class));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	protected boolean freezesDuringWindup() {
		return false;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	/** Position que le monstre cherche à atteindre quand il ne fait rien de spécial. */
	protected abstract Vec3d hoverPosition(LivingEntity target);

	protected void flyTowards(Vec3d dest, double speed) {
		Vec3d delta = dest.subtract(getPos());
		double len = delta.length();
		Vec3d wanted = len < 0.2 ? Vec3d.ZERO : delta.multiply(Math.min(speed, len * 0.3) / len);
		setVelocity(getVelocity().multiply(0.75).add(wanted.multiply(0.25 / 0.25 * 0.35)));
	}

	protected void face(LivingEntity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) (MathHelper.atan2(dz, dx) * (180f / Math.PI)) - 90f;
		setYaw(yaw);
		setBodyYaw(yaw);
		setHeadYaw(yaw);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || !isAlive()) {
			return;
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			setVelocity(getVelocity().multiply(0.8).add(0, Math.sin(age * 0.1) * 0.01, 0));
			return;
		}
		face(target);
		if (!isStriking()) {
			flyTowards(hoverPosition(target), 0.35);
		}
	}
}
