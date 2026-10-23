package fr.nico.roguetower.mob;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.classes.Spells;
import fr.nico.roguetower.entity.ModEntities;
import fr.nico.roguetower.mixin.MobEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public final class MobSetup {
	public static final String MOB_TAG = "roguetower_mob";
	public static final String AGGRO_TAG = "roguetower_aggro";
	private static final Identifier HEALTH_ID = RogueTower.id("floor_health");
	private static final Identifier DAMAGE_ID = RogueTower.id("floor_damage");

	private MobSetup() {
	}

	private record Entry(EntityType<?> type, int minFloor, int weight) {
	}

	/** Uniquement des monstres inédits (plus aucun monstre ni animal vanilla dans la tour). */
	private static final List<Entry> MONSTERS = List.of(
		new Entry(ModEntities.CRAWLER, 1, 10),
		new Entry(ModEntities.SPORELING, 1, 8),
		new Entry(ModEntities.GLUTTON, 1, 8),
		new Entry(ModEntities.BLADE_MANTIS, 2, 8),
		new Entry(ModEntities.HOWLING_TOTEM, 2, 5),
		new Entry(ModEntities.EMBER_GARGOYLE, 4, 5),
		new Entry(ModEntities.VOID_EYE, 5, 5)
	);

	private static final List<EntityType<?>> BOSSES = List.of(
		ModEntities.RUNE_GOLEM, ModEntities.SHADOW_WEAVER
	);

	private static EntityType<?> pick(List<Entry> list, int floor, Random random) {
		List<Entry> pool = new ArrayList<>();
		int total = 0;
		for (Entry e : list) {
			if (floor >= e.minFloor()) {
				pool.add(e);
				total += e.weight();
			}
		}
		int r = random.nextInt(total);
		for (Entry e : pool) {
			r -= e.weight();
			if (r < 0) {
				return e.type();
			}
		}
		return pool.get(0).type();
	}

	public static EntityType<?> randomMob(int floor, Random random) {
		return pick(MONSTERS, floor, random);
	}

	public static EntityType<?> randomBoss(Random random) {
		return BOSSES.get(random.nextInt(BOSSES.size()));
	}

	/** Fait apparaître un monstre d'étage, renforcé selon l'étage. */
	public static MobEntity spawn(ServerWorld world, EntityType<?> type, BlockPos pos, int floor, boolean boss, PlayerEntity target) {
		Entity entity = type.create(world);
		if (!(entity instanceof MobEntity mob)) {
			if (entity != null) {
				entity.discard();
			}
			return null;
		}
		mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
		mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
		mob.addCommandTag(MOB_TAG);
		mob.addCommandTag(AGGRO_TAG);
		mob.setPersistent();

		double healthBonus = 0.15 * (floor - 1) + (boss ? 1.0 : 0.0);
		if (mob instanceof AnimalEntity) {
			healthBonus += 0.5;
		}
		modify(mob, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, healthBonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modify(mob, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_ID, 0.5 * (floor - 1) + (boss ? 2 : 0), EntityAttributeModifier.Operation.ADD_VALUE);
		mob.setHealth(mob.getMaxHealth());

		if (boss) {
			mob.setCustomName(Text.literal("Gardien de l'étage " + floor).formatted(Formatting.DARK_RED, Formatting.BOLD));
			mob.setCustomNameVisible(true);
			mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 0, false, false));
			mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1, 0, false, false));
		}
		world.spawnEntity(mob);
		makeAggressive(mob, floor);
		if (target != null) {
			mob.setTarget(target);
		}
		return mob;
	}

	private static void modify(MobEntity mob, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
							   Identifier id, double value, EntityAttributeModifier.Operation op) {
		EntityAttributeInstance instance = mob.getAttributeInstance(attribute);
		if (instance != null && value != 0) {
			instance.removeModifier(id);
			instance.addPersistentModifier(new EntityAttributeModifier(id, value, op));
		}
	}

	/** Ajoute le comportement agressif aux animaux et golems (appelé aussi au rechargement du monde). */
	public static void makeAggressive(MobEntity mob, int floor) {
		if (!(mob instanceof PathAwareEntity pathAware) || mob instanceof TameableEntity t && t.isTamed()) {
			return;
		}
		if (mob.getCommandTags().contains(Spells.SUMMON_TAG)) {
			return;
		}
		boolean needsGoal = mob instanceof AnimalEntity || mob.getType() == EntityType.IRON_GOLEM;
		if (!needsGoal) {
			return;
		}
		float damage = baseBite(mob.getType()) + 0.4f * Math.max(0, floor - 1);
		if (mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
			damage = Math.max(damage, (float) mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
		}
		((MobEntityAccessor) mob).roguetower$getGoalSelector().add(0, new AnimalAggroGoal(pathAware, damage, 1.6));
	}

	private static float baseBite(EntityType<?> type) {
		if (type == EntityType.CHICKEN || type == EntityType.RABBIT) {
			return 2f;
		}
		if (type == EntityType.PIG || type == EntityType.SHEEP || type == EntityType.FOX) {
			return 3f;
		}
		if (type == EntityType.COW || type == EntityType.MOOSHROOM || type == EntityType.LLAMA || type == EntityType.WOLF) {
			return 4f;
		}
		if (type == EntityType.GOAT || type == EntityType.HORSE || type == EntityType.PANDA) {
			return 5f;
		}
		return 6f;
	}
}
