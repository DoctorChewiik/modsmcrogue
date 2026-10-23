package fr.nico.roguetower.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Projectile magique des monstres : une boule de particules sans gravité. */
public class MagicBoltEntity extends ProjectileEntity {
	public static final int SPIRIT = 0;
	public static final int VOID = 1;
	public static final int RUNE = 2;
	public static final int SHADOW = 3;

	private static final TrackedData<Integer> KIND = DataTracker.registerData(MagicBoltEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private float damage = 4f;

	public MagicBoltEntity(EntityType<? extends ProjectileEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(KIND, SPIRIT);
	}

	public void setKind(int kind) {
		dataTracker.set(KIND, kind);
	}

	public int getKind() {
		return dataTracker.get(KIND);
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !(entity instanceof RogueMob) && !(entity instanceof MagicBoltEntity);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient) {
			HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
			if (hit.getType() != HitResult.Type.MISS) {
				onCollision(hit);
				if (isRemoved()) {
					return;
				}
			}
			if (age > 80) {
				discard();
				return;
			}
		}
		Vec3d v = getVelocity();
		setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
		if (getWorld().isClient) {
			ParticleEffect main = switch (getKind()) {
				case VOID -> ParticleTypes.REVERSE_PORTAL;
				case RUNE -> ParticleTypes.ELECTRIC_SPARK;
				case SHADOW -> ParticleTypes.SQUID_INK;
				default -> ParticleTypes.SOUL_FIRE_FLAME;
			};
			for (int i = 0; i < 3; i++) {
				getWorld().addParticle(main, getX() + (random.nextDouble() - 0.5) * 0.2, getY() + (random.nextDouble() - 0.5) * 0.2,
					getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
			}
			getWorld().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult result) {
		Entity target = result.getEntity();
		Entity owner = getOwner();
		if (target instanceof LivingEntity living && target != owner) {
			living.damage(getDamageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), damage);
			switch (getKind()) {
				case VOID -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 15, 0), owner);
				case SHADOW -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0), owner);
				case RUNE -> living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), owner);
				default -> living.setOnFireFor(2);
			}
		}
		discard();
	}

	@Override
	protected void onBlockHit(BlockHitResult result) {
		super.onBlockHit(result);
		discard();
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 4096;
	}
}
