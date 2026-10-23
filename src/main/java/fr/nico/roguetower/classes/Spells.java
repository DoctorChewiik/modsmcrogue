package fr.nico.roguetower.classes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/** Effets de tous les pouvoirs. Chaque méthode renvoie vrai si le pouvoir a été lancé. */
public final class Spells {
	public static final String SUMMON_TAG = "roguetower_summon";

	private Spells() {
	}

	public static boolean cast(ServerPlayerEntity p, Spell spell) {
		ServerWorld world = p.getServerWorld();
		return switch (spell) {
			case NONE -> false;
			case WHIRLWIND -> {
				for (LivingEntity e : enemiesAround(p, 4.5)) {
					hurt(p, e, 7f);
					Vec3d push = e.getPos().subtract(p.getPos()).normalize();
					e.takeKnockback(1.2, -push.x, -push.z);
				}
				particles(world, ParticleTypes.SWEEP_ATTACK, p.getPos().add(0, 1, 0), 12, 2.0);
				sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
				yield true;
			}
			case WAR_CRY -> {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 160, 1));
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 1));
				particles(world, ParticleTypes.ANGRY_VILLAGER, p.getPos().add(0, 2, 0), 8, 0.6);
				sound(p, SoundEvents.ENTITY_RAVAGER_ROAR);
				yield true;
			}
			case VOLLEY -> {
				for (int i = -3; i <= 3; i++) {
					ArrowEntity arrow = new ArrowEntity(world, p, new ItemStack(Items.ARROW), null);
					arrow.setVelocity(p, p.getPitch(), p.getYaw() + i * 10f, 0f, 3.0f, 1.0f);
					arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
					world.spawnEntity(arrow);
				}
				sound(p, SoundEvents.ENTITY_ARROW_SHOOT);
				yield true;
			}
			case LEAP -> {
				Vec3d dir = horizontalLook(p);
				p.setVelocity(dir.x * 1.6, 0.55, dir.z * 1.6);
				p.velocityModified = true;
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 0, false, false));
				particles(world, ParticleTypes.CLOUD, p.getPos(), 10, 0.4);
				sound(p, SoundEvents.ENTITY_GOAT_LONG_JUMP);
				yield true;
			}
			case FIREBOLT -> {
				Vec3d eye = p.getEyePos();
				Vec3d end = rayEnd(p, 24);
				LivingEntity target = lookTarget(p, 24);
				if (target != null) {
					end = target.getPos().add(0, target.getHeight() / 2, 0);
					hurt(p, target, 8f);
					target.setOnFireFor(5);
				}
				line(world, ParticleTypes.FLAME, eye, end);
				sound(p, SoundEvents.ENTITY_BLAZE_SHOOT);
				yield true;
			}
			case FROST_NOVA -> {
				for (LivingEntity e : enemiesAround(p, 6)) {
					hurt(p, e, 6f);
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3));
					e.setFrozenTicks(200);
				}
				particles(world, ParticleTypes.SNOWFLAKE, p.getPos().add(0, 1, 0), 80, 4.0);
				sound(p, SoundEvents.BLOCK_GLASS_BREAK);
				yield true;
			}
			case SHADOW_STEP -> {
				Vec3d dir = horizontalLook(p);
				Vec3d start = p.getPos().add(0, 0.5, 0);
				Vec3d end = start.add(dir.multiply(8));
				BlockHitResult hit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, p));
				Vec3d dest = hit.getType() == HitResult.Type.MISS ? end : hit.getPos().subtract(dir.multiply(0.6));
				particles(world, ParticleTypes.LARGE_SMOKE, p.getPos().add(0, 1, 0), 20, 0.4);
				p.requestTeleport(dest.x, p.getY(), dest.z);
				particles(world, ParticleTypes.PORTAL, dest.add(0, 1, 0), 30, 0.5);
				sound(p, SoundEvents.ENTITY_ENDERMAN_TELEPORT);
				yield true;
			}
			case SHADOW_VEIL -> {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 120, 0));
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 1));
				for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(20), m -> m.getTarget() == p)) {
					mob.setTarget(null);
				}
				particles(world, ParticleTypes.SQUID_INK, p.getPos().add(0, 1, 0), 30, 0.6);
				sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE);
				yield true;
			}
			case SPECTRAL_PACK -> {
				for (int i = 0; i < 3; i++) {
					WolfEntity wolf = EntityType.WOLF.create(world);
					if (wolf == null) {
						continue;
					}
					double angle = i * Math.PI * 2 / 3;
					wolf.refreshPositionAndAngles(p.getX() + Math.cos(angle) * 1.5, p.getY(), p.getZ() + Math.sin(angle) * 1.5, p.getYaw(), 0);
					wolf.setOwner(p);
					wolf.addCommandTag(SUMMON_TAG);
					wolf.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 800, 1));
					wolf.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 800, 0, false, false));
					world.spawnEntity(wolf);
					ClassManager.trackSummon(wolf, 800);
				}
				particles(world, ParticleTypes.SOUL, p.getPos().add(0, 1, 0), 30, 1.5);
				sound(p, SoundEvents.ENTITY_WOLF_HOWL);
				yield true;
			}
			case LIFE_DRAIN -> {
				LivingEntity target = lookTarget(p, 16);
				if (target == null) {
					p.sendMessage(Text.literal("Aucune cible"), true);
					yield false;
				}
				hurt(p, target, 6f);
				p.heal(5f);
				line(world, ParticleTypes.DAMAGE_INDICATOR, target.getPos().add(0, 1, 0), p.getPos().add(0, 1, 0));
				sound(p, SoundEvents.ENTITY_WITCH_DRINK);
				yield true;
			}
			case HEAL -> {
				p.heal(8f);
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
				particles(world, ParticleTypes.HEART, p.getPos().add(0, 1.5, 0), 10, 0.6);
				sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP);
				yield true;
			}
			case SHIELD -> {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 3));
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0));
				particles(world, ParticleTypes.ENCHANT, p.getPos().add(0, 1, 0), 60, 1.0);
				sound(p, SoundEvents.BLOCK_BEACON_ACTIVATE);
				yield true;
			}
			case LIGHTNING -> {
				LivingEntity target = lookTarget(p, 30);
				if (target == null) {
					p.sendMessage(Text.literal("Aucune cible"), true);
					yield false;
				}
				strike(p, target, 10f);
				int extra = 0;
				for (LivingEntity e : enemiesAround(target, 5)) {
					if (e != target && extra++ < 3) {
						strike(p, e, 6f);
					}
				}
				yield true;
			}
			case ARCANE_BLAST -> {
				world.createExplosion(p, p.getX(), p.getY() + 0.5, p.getZ(), 3.0f, World.ExplosionSourceType.NONE);
				particles(world, ParticleTypes.WITCH, p.getPos().add(0, 1, 0), 60, 3.0);
				yield true;
			}
			case TORNADO -> {
				for (LivingEntity e : enemiesAround(p, 8)) {
					Vec3d out = e.getPos().subtract(p.getPos()).normalize().multiply(0.6);
					e.setVelocity(out.x, 1.2, out.z);
					e.velocityModified = true;
					hurt(p, e, 4f);
				}
				particles(world, ParticleTypes.CLOUD, p.getPos().add(0, 1, 0), 120, 4.0);
				sound(p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP);
				yield true;
			}
			case METEOR -> {
				Vec3d eye = p.getEyePos();
				BlockHitResult hit = world.raycast(new RaycastContext(eye, rayEnd(p, 30), RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, p));
				Vec3d center = hit.getType() == HitResult.Type.MISS ? p.getPos().add(horizontalLook(p).multiply(10)) : hit.getPos();
				for (int i = 0; i < 7; i++) {
					double ox = (p.getRandom().nextDouble() - 0.5) * 8;
					double oz = (p.getRandom().nextDouble() - 0.5) * 8;
					Vec3d at = center.add(ox, 0, oz);
					ClassManager.schedule(world.getServer().getTicks() + 6 + i * 5, () -> {
						particles(world, ParticleTypes.LAVA, at.add(0, 3, 0), 20, 0.5);
						world.createExplosion(p, at.x, at.y + 0.5, at.z, 2.5f, World.ExplosionSourceType.NONE);
					});
				}
				sound(p, SoundEvents.ENTITY_WITHER_SHOOT);
				yield true;
			}
			case DEATH_MARK -> {
				for (LivingEntity e : enemiesAround(p, 10)) {
					e.damage(p.getDamageSources().indirectMagic(p, p), 25f);
					particles(world, ParticleTypes.SOUL_FIRE_FLAME, e.getPos().add(0, 1, 0), 20, 0.5);
				}
				sound(p, SoundEvents.ENTITY_WITHER_SPAWN);
				yield true;
			}
		};
	}

	public static boolean isEnemy(ServerPlayerEntity p, Entity e) {
		if (!(e instanceof MobEntity mob) || !mob.isAlive()) {
			return false;
		}
		if (e instanceof TameableEntity t && t.isTamed()) {
			return false;
		}
		if (e.getCommandTags().contains(SUMMON_TAG) || e instanceof MerchantEntity || e instanceof IronGolemEntity) {
			return false;
		}
		return true;
	}

	public static List<LivingEntity> enemiesAround(LivingEntity center, double radius) {
		ServerPlayerEntity owner = center instanceof ServerPlayerEntity sp ? sp : null;
		return center.getWorld().getEntitiesByClass(LivingEntity.class, center.getBoundingBox().expand(radius),
			e -> e != center && e.squaredDistanceTo(center) <= radius * radius && isEnemy(owner, e));
	}

	private static void hurt(ServerPlayerEntity p, LivingEntity target, float amount) {
		target.damage(p.getDamageSources().playerAttack(p), amount);
	}

	private static void strike(ServerPlayerEntity p, LivingEntity target, float amount) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(p.getWorld());
		if (bolt != null) {
			bolt.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
			bolt.setCosmetic(true);
			p.getWorld().spawnEntity(bolt);
		}
		target.damage(p.getDamageSources().indirectMagic(p, p), amount);
	}

	private static Vec3d horizontalLook(ServerPlayerEntity p) {
		double yaw = Math.toRadians(p.getYaw());
		return new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	private static Vec3d rayEnd(ServerPlayerEntity p, double range) {
		return p.getEyePos().add(p.getRotationVector().multiply(range));
	}

	/** Premier ennemi dans la direction visée (avec une bonne tolérance pour la vue du dessus). */
	public static LivingEntity lookTarget(ServerPlayerEntity p, double range) {
		Vec3d eye = p.getEyePos();
		Vec3d end = rayEnd(p, range);
		BlockHitResult block = p.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE, p));
		double max = block.getType() == HitResult.Type.MISS ? range * range : eye.squaredDistanceTo(block.getPos());
		LivingEntity best = null;
		double bestDist = max;
		Box area = p.getBoundingBox().stretch(p.getRotationVector().multiply(range)).expand(2.0);
		for (LivingEntity e : p.getWorld().getEntitiesByClass(LivingEntity.class, area, e -> isEnemy(p, e))) {
			Optional<Vec3d> hit = e.getBoundingBox().expand(1.0).raycast(eye, end);
			if (hit.isPresent()) {
				double d = eye.squaredDistanceTo(hit.get());
				if (d < bestDist) {
					bestDist = d;
					best = e;
				}
			}
		}
		return best;
	}

	private static void particles(ServerWorld world, ParticleEffect type, Vec3d at, int count, double spread) {
		world.spawnParticles(type, at.x, at.y, at.z, count, spread, spread * 0.5, spread, 0.02);
	}

	private static void line(ServerWorld world, ParticleEffect type, Vec3d from, Vec3d to) {
		Vec3d delta = to.subtract(from);
		int steps = (int) Math.max(1, delta.length() * 3);
		for (int i = 0; i <= steps; i++) {
			Vec3d at = from.add(delta.multiply(i / (double) steps));
			world.spawnParticles(type, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	private static void sound(ServerPlayerEntity p, SoundEvent sound) {
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

}
