package fr.nico.roguetower.mob;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.EnumSet;

/**
 * Rend un animal agressif : il fonce sur le joueur le plus proche et le mord.
 * Les animaux n'ont pas d'attribut de dégâts, donc on inflige les dégâts nous-mêmes.
 */
public class AnimalAggroGoal extends Goal {
	private final PathAwareEntity mob;
	private final float damage;
	private final double speed;
	private PlayerEntity target;
	private int cooldown;
	private int repath;

	public AnimalAggroGoal(PathAwareEntity mob, float damage, double speed) {
		this.mob = mob;
		this.damage = damage;
		this.speed = speed;
		setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	private boolean valid(PlayerEntity p) {
		return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && !p.isInvisible()
			&& p.squaredDistanceTo(mob) < 40 * 40;
	}

	@Override
	public boolean canStart() {
		PlayerEntity p = mob.getWorld().getClosestPlayer(mob, 32);
		if (valid(p)) {
			target = p;
			return true;
		}
		return false;
	}

	@Override
	public boolean shouldContinue() {
		return valid(target);
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public void stop() {
		target = null;
		mob.getNavigation().stop();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}
		mob.getLookControl().lookAt(target, 30f, 30f);
		if (--repath <= 0) {
			repath = 8;
			mob.getNavigation().startMovingTo(target, speed);
		}
		if (cooldown > 0) {
			cooldown--;
		}
		double reach = mob.getWidth() * 0.9 + target.getWidth() * 0.5 + 0.9;
		if (cooldown <= 0 && mob.squaredDistanceTo(target) <= reach * reach && mob.canSee(target)) {
			cooldown = 20;
			mob.swingHand(Hand.MAIN_HAND);
			target.damage(mob.getDamageSources().mobAttack(mob), damage);
			double dx = target.getX() - mob.getX();
			double dz = target.getZ() - mob.getZ();
			target.takeKnockback(0.4, -dx, -dz);
		}
	}
}
