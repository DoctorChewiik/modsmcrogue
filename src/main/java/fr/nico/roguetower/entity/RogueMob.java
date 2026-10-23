package fr.nico.roguetower.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Base des monstres de la tour : gère une action en deux temps
 * (préparation visible puis frappe) synchronisée avec le client pour les animations.
 */
public abstract class RogueMob extends HostileEntity {
	public static final int ACTION_NONE = 0;
	public static final int ACTION_WINDUP = 1;
	public static final int ACTION_STRIKE = 2;

	private static final TrackedData<Integer> ACTION = DataTracker.registerData(RogueMob.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> ACTION_TICKS = DataTracker.registerData(RogueMob.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> ACTION_TOTAL = DataTracker.registerData(RogueMob.class, TrackedDataHandlerRegistry.INTEGER);

	protected int specialCooldown = 60;

	protected RogueMob(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ACTION, ACTION_NONE);
		builder.add(ACTION_TICKS, 0);
		builder.add(ACTION_TOTAL, 1);
	}

	/** Ajoute les comportements de base. Les monstres immobiles ou volants peuvent s'en passer. */
	protected void addStandardGoals(boolean melee, double speed) {
		goalSelector.add(0, new SwimGoal(this));
		if (melee) {
			goalSelector.add(2, new MeleeAttackGoal(this, speed, true));
		}
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12f));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this, RogueMob.class));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	public int getAction() {
		return dataTracker.get(ACTION);
	}

	public boolean isWindingUp() {
		return getAction() == ACTION_WINDUP;
	}

	public boolean isStriking() {
		return getAction() == ACTION_STRIKE;
	}

	/** Progression de l'action en cours, de 0 à 1. */
	public float getActionProgress() {
		if (getAction() == ACTION_NONE) {
			return 0f;
		}
		int total = Math.max(1, dataTracker.get(ACTION_TOTAL));
		return 1f - dataTracker.get(ACTION_TICKS) / (float) total;
	}

	protected void startAction(int action, int ticks) {
		dataTracker.set(ACTION, action);
		dataTracker.set(ACTION_TICKS, ticks);
		dataTracker.set(ACTION_TOTAL, ticks);
	}

	/** Appelé à la fin de la préparation. */
	protected void onWindupDone(ServerWorld world) {
	}

	/** Appelé à la fin de la frappe. */
	protected void onStrikeDone(ServerWorld world) {
	}

	/** Appelé chaque tick serveur hors action : décide de lancer une attaque spéciale. */
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
	}

	/** Pendant une préparation, le monstre reste sur place (sauf s'il vole). */
	protected boolean freezesDuringWindup() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world) || !isAlive()) {
			return;
		}
		int action = getAction();
		if (action != ACTION_NONE) {
			int left = dataTracker.get(ACTION_TICKS) - 1;
			dataTracker.set(ACTION_TICKS, Math.max(0, left));
			if (action == ACTION_WINDUP && freezesDuringWindup()) {
				getNavigation().stop();
				setVelocity(0, getVelocity().y, 0);
				LivingEntity target = getTarget();
				if (target != null) {
					getLookControl().lookAt(target, 90f, 90f);
				}
			}
			if (left <= 0) {
				dataTracker.set(ACTION, ACTION_NONE);
				if (action == ACTION_WINDUP) {
					onWindupDone(world);
				} else {
					onStrikeDone(world);
				}
			}
			return;
		}
		if (specialCooldown > 0) {
			specialCooldown--;
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive()) {
			tickSpecial(world, target);
		}
	}

	// ---------- outils ----------

	protected void sfx(SoundEvent sound, float volume, float pitch) {
		getWorld().playSound(null, getX(), getY(), getZ(), sound, SoundCategory.HOSTILE, volume, pitch);
	}

	protected boolean canHurt(PlayerEntity p) {
		return p.isAlive() && !p.isCreative() && !p.isSpectator();
	}

	protected void shootBolt(ServerWorld world, LivingEntity target, int kind, float damage, double speed, float spreadDegrees) {
		Vec3d from = new Vec3d(getX(), getBodyY(0.6), getZ());
		Vec3d dir = new Vec3d(target.getX(), target.getBodyY(0.5), target.getZ()).subtract(from).normalize();
		if (spreadDegrees != 0) {
			double a = Math.toRadians(spreadDegrees);
			dir = new Vec3d(dir.x * Math.cos(a) - dir.z * Math.sin(a), dir.y, dir.x * Math.sin(a) + dir.z * Math.cos(a));
		}
		MagicBoltEntity bolt = new MagicBoltEntity(ModEntities.MAGIC_BOLT, world);
		bolt.setOwner(this);
		bolt.setKind(kind);
		bolt.setDamage(damage);
		bolt.setPosition(from.add(dir.multiply(0.8)));
		bolt.setVelocity(dir.multiply(speed));
		world.spawnEntity(bolt);
	}
}
