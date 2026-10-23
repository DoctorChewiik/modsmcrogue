package fr.nico.roguetower.combat;

import fr.nico.roguetower.classes.ClassManager;
import fr.nico.roguetower.classes.Spells;
import fr.nico.roguetower.net.CombatAnimPayload;
import fr.nico.roguetower.net.CombatPayload;
import fr.nico.roguetower.net.CombatSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Système de combat : esquive avec invincibilité, parade avec contre-attaque,
 * combos au corps à corps (3e coup = coup final), coups dans le dos et endurance.
 */
public final class CombatManager {
	public static final float MAX_STAMINA = 100f;
	private static final float DODGE_COST = 25f;
	private static final float PARRY_COST = 15f;
	private static final int DODGE_IFRAMES = 8;
	private static final int PARRY_WINDOW = 8;
	private static final int COMBO_WINDOW = 40;

	private static final class State {
		float stamina = MAX_STAMINA;
		int lastSpend = -100;
		int dodgeUntil;
		int dodgeReadyAt;
		int parryUntil;
		int parryReadyAt;
		int combo;
		int lastHit = -100;
		boolean riposte;
		int lastSyncedStamina = -1;
		int lastSyncedCombo = -1;
		int pendingEvent;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private CombatManager() {
	}

	private static State state(ServerPlayerEntity p) {
		return STATES.computeIfAbsent(p.getUuid(), u -> new State());
	}

	private static boolean spend(State s, float cost, int now) {
		if (s.stamina < cost) {
			return false;
		}
		s.stamina -= cost;
		s.lastSpend = now;
		return true;
	}

	public static void handle(ServerPlayerEntity p, CombatPayload payload) {
		if (p.isSpectator() || !p.isAlive()) {
			return;
		}
		int now = p.getServer().getTicks();
		State s = state(p);
		ServerWorld world = p.getServerWorld();
		if (payload.action() == CombatPayload.DODGE) {
			if (now < s.dodgeReadyAt || !spend(s, DODGE_COST, now)) {
				return;
			}
			Vec3d dir = new Vec3d(payload.dirX(), 0, payload.dirZ());
			if (dir.lengthSquared() < 0.01) {
				double yaw = Math.toRadians(p.getYaw());
				dir = new Vec3d(Math.sin(yaw), 0, -Math.cos(yaw)); // en arrière
			}
			dir = dir.normalize();
			p.setVelocity(dir.x * 1.25, p.isOnGround() ? 0.18 : p.getVelocity().y, dir.z * 1.25);
			p.velocityModified = true;
			p.fallDistance = 0;
			s.dodgeUntil = now + DODGE_IFRAMES;
			s.dodgeReadyAt = now + 12;
			world.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
			sound(p, SoundEvents.ENTITY_PHANTOM_FLAP, 1.6f);
			s.pendingEvent = CombatSyncPayload.EVENT_DODGE;
			playAnim(p, CombatAnimPayload.DODGE, (float) dir.x, (float) dir.z);
		} else if (payload.action() == CombatPayload.PARRY) {
			if (now < s.parryReadyAt || !spend(s, PARRY_COST, now)) {
				return;
			}
			s.parryUntil = now + PARRY_WINDOW;
			s.parryReadyAt = now + 25;
			p.swingHand(Hand.OFF_HAND, true);
			sound(p, SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), 1.4f);
			playAnim(p, CombatAnimPayload.PARRY, 0, 0);
		}
		sync(p, s, true);
	}

	/** Appelé avant chaque dégât subi par une entité : renvoie faux pour annuler. */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayerEntity p)) {
			return true;
		}
		State s = state(p);
		int now = p.getServer().getTicks();
		Entity attacker = source.getAttacker();
		boolean physical = attacker != null || source.getSource() instanceof ProjectileEntity;
		if (!physical) {
			return true;
		}
		ServerWorld world = p.getServerWorld();
		if (now <= s.dodgeUntil) {
			world.spawnParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
			return false;
		}
		if (now <= s.parryUntil) {
			s.parryUntil = 0;
			s.parryReadyAt = now;
			s.stamina = Math.min(MAX_STAMINA, s.stamina + PARRY_COST + 10);
			s.riposte = true;
			world.spawnParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(), 25, 0.4, 0.4, 0.4, 0.4);
			sound(p, SoundEvents.ITEM_SHIELD_BLOCK, 1.0f);
			sound(p, SoundEvents.BLOCK_ANVIL_LAND, 2.0f);
			if (source.getSource() instanceof ProjectileEntity projectile) {
				projectile.discard();
			}
			if (attacker instanceof LivingEntity living && living.squaredDistanceTo(p) < 36) {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 4), p);
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 50, 1), p);
				Vec3d push = living.getPos().subtract(p.getPos()).normalize();
				living.takeKnockback(0.9, -push.x, -push.z);
				living.damage(p.getDamageSources().playerAttack(p), 3f);
			}
			s.pendingEvent = CombatSyncPayload.EVENT_PARRY;
			playAnim(p, CombatAnimPayload.PARRY_SUCCESS, 0, 0);
			sync(p, s, true);
			return false;
		}
		return true;
	}

	/** Coup au corps à corps donné par un joueur (avant les dégâts vanilla). */
	public static void onMeleeHit(ServerPlayerEntity p, LivingEntity target) {
		if (p.getAttackCooldownProgress(0.5f) < 0.75f || !Spells.isEnemy(p, target)) {
			return;
		}
		int now = p.getServer().getTicks();
		State s = state(p);
		s.combo = now - s.lastHit <= COMBO_WINDOW ? s.combo + 1 : 1;
		s.lastHit = now;

		float base = (float) p.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
		float bonus = 0f;
		int event = CombatSyncPayload.EVENT_NONE;
		ServerWorld world = p.getServerWorld();

		// Coup dans le dos
		Vec3d facing = target.getRotationVector().multiply(1, 0, 1).normalize();
		Vec3d toPlayer = p.getPos().subtract(target.getPos()).multiply(1, 0, 1).normalize();
		if (facing.dotProduct(toPlayer) < -0.5) {
			bonus += base * 0.5f;
			event = CombatSyncPayload.EVENT_BACKSTAB;
			world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.6), target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
		}
		// Riposte après une parade réussie
		if (s.riposte) {
			s.riposte = false;
			bonus += base;
			event = CombatSyncPayload.EVENT_RIPOSTE;
		}
		// Coup final du combo
		if (s.combo % 3 == 0) {
			bonus += base * 0.6f;
			event = CombatSyncPayload.EVENT_FINISHER;
			Vec3d push = target.getPos().subtract(p.getPos()).normalize();
			target.takeKnockback(1.1, -push.x, -push.z);
			world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 3, 0.6, 0.2, 0.6, 0.0);
			sound(p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.8f);
			for (LivingEntity other : Spells.enemiesAround(target, 2.5)) {
				if (other != target) {
					other.damage(p.getDamageSources().playerAttack(p), base * 0.4f);
				}
			}
		}
		if (bonus > 0) {
			float extra = bonus;
			ClassManager.schedule(now + 1, () -> {
				if (target.isAlive()) {
					target.timeUntilRegen = 0;
					target.damage(p.getDamageSources().playerAttack(p), extra);
				}
			});
		}
		s.pendingEvent = event;
		if (event == CombatSyncPayload.EVENT_FINISHER) {
			playAnim(p, CombatAnimPayload.FINISHER, 0, 0);
		} else if (event == CombatSyncPayload.EVENT_BACKSTAB || event == CombatSyncPayload.EVENT_RIPOSTE) {
			playAnim(p, CombatAnimPayload.THRUST, 0, 0);
		}
		sync(p, s, true);
	}

	/** Envoie l'animation au joueur et à tous ceux qui le voient. */
	public static void playAnim(ServerPlayerEntity p, int anim, float dirX, float dirZ) {
		CombatAnimPayload payload = new CombatAnimPayload(p.getId(), anim, dirX, dirZ);
		ServerPlayNetworking.send(p, payload);
		for (ServerPlayerEntity other : PlayerLookup.tracking(p)) {
			if (other != p) {
				ServerPlayNetworking.send(other, payload);
			}
		}
	}

	public static void tick(MinecraftServer server) {
		int now = server.getTicks();
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			State s = state(p);
			if (now - s.lastSpend > 15 && s.stamina < MAX_STAMINA) {
				s.stamina = Math.min(MAX_STAMINA, s.stamina + 1.2f);
			}
			if (s.combo > 0 && now - s.lastHit > COMBO_WINDOW) {
				s.combo = 0;
			}
			if (now % 4 == 0) {
				sync(p, s, false);
			}
		}
	}

	private static void sync(ServerPlayerEntity p, State s, boolean force) {
		int stamina = Math.round(s.stamina);
		if (!force && stamina == s.lastSyncedStamina && s.combo == s.lastSyncedCombo) {
			return;
		}
		s.lastSyncedStamina = stamina;
		s.lastSyncedCombo = s.combo;
		ServerPlayNetworking.send(p, new CombatSyncPayload(stamina, s.combo, s.pendingEvent));
		s.pendingEvent = CombatSyncPayload.EVENT_NONE;
	}

	private static void sound(ServerPlayerEntity p, SoundEvent sound, float pitch) {
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundCategory.PLAYERS, 1.0f, pitch);
	}
}
