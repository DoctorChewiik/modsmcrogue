package fr.nico.roguetower.classes;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.net.ClassSyncPayload;
import fr.nico.roguetower.world.RogueState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClassManager {
	private static final Identifier HEALTH_ID = RogueTower.id("class_health");
	private static final Identifier ARMOR_ID = RogueTower.id("class_armor");
	private static final Identifier SPEED_ID = RogueTower.id("class_speed");
	private static final Identifier DAMAGE_ID = RogueTower.id("class_damage");
	private static final Identifier REACH_ID = RogueTower.id("combat_reach");

	/** Tick serveur auquel la compétence (0) et le sort (1) seront prêts. */
	private static final Map<UUID, int[]> READY_AT = new HashMap<>();
	private static final Map<Entity, Integer> SUMMONS = new HashMap<>();
	private static final List<Task> TASKS = new ArrayList<>();

	private record Task(int tick, Runnable action) {
	}

	private ClassManager() {
	}

	public static RogueState.PlayerData data(ServerPlayerEntity p) {
		return RogueState.get(p.getServer()).get(p.getUuid());
	}

	public static void selectClass(ServerPlayerEntity p, RogueClass cls) {
		if (cls == RogueClass.NONE) {
			return;
		}
		RogueState.PlayerData data = data(p);
		if (data.rogueClass == cls) {
			p.sendMessage(Text.literal("Tu es déjà " + cls.displayName + ".").formatted(Formatting.GRAY), false);
			return;
		}
		if (RogueTower.isTower(p.getWorld()) && data.rogueClass != RogueClass.NONE) {
			p.sendMessage(Text.literal("Impossible de changer de classe dans la tour.").formatted(Formatting.RED), false);
			return;
		}
		data.rogueClass = cls;
		data.spell = cls.startingSpell;
		RogueState.get(p.getServer()).markDirty();
		applyPassives(p);
		giveKit(p, cls);
		p.setHealth(p.getMaxHealth());
		READY_AT.remove(p.getUuid());
		p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.6f, 1f);
		p.sendMessage(Text.literal("Tu es maintenant " + cls.displayName + " ! ").formatted(Formatting.GOLD)
			.append(Text.literal("[R] " + cls.ability.displayName + "  [F] " + data.spell.displayName).formatted(Formatting.YELLOW)), false);
		sync(p, false);
	}

	public static void learnSpell(ServerPlayerEntity p, Spell spell) {
		RogueState.PlayerData data = data(p);
		Spell old = data.spell;
		data.spell = spell;
		RogueState.get(p.getServer()).markDirty();
		ready(p)[1] = 0;
		p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1f);
		p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 1f);
		p.sendMessage(Text.literal("Nouveau sort [F] : ").formatted(Formatting.LIGHT_PURPLE)
			.append(Text.literal(spell.displayName).formatted(spell.rarity.color))
			.append(Text.literal(old == Spell.NONE ? "" : " (remplace " + old.displayName + ")").formatted(Formatting.GRAY)), false);
		sync(p, false);
	}

	private static int[] ready(ServerPlayerEntity p) {
		return READY_AT.computeIfAbsent(p.getUuid(), u -> new int[2]);
	}

	public static void useSlot(ServerPlayerEntity p, int slot) {
		if (slot != 0 && slot != 1 || p.isSpectator()) {
			return;
		}
		RogueState.PlayerData data = data(p);
		if (data.rogueClass == RogueClass.NONE) {
			sync(p, true);
			return;
		}
		Spell spell = slot == 0 ? data.rogueClass.ability : data.spell;
		if (spell == Spell.NONE) {
			p.sendMessage(Text.literal("Aucun sort. Trouve un livre de sort !").formatted(Formatting.GRAY), true);
			return;
		}
		int now = p.getServer().getTicks();
		int[] ready = ready(p);
		if (ready[slot] > now) {
			p.sendMessage(Text.literal(spell.displayName + " : recharge " + ((ready[slot] - now) / 20 + 1) + " s")
				.formatted(Formatting.RED), true);
			return;
		}
		if (Spells.cast(p, spell)) {
			fr.nico.roguetower.combat.CombatManager.playAnim(p, fr.nico.roguetower.net.CombatAnimPayload.CAST, 0, 0);
			int cooldown = spell.cooldown;
			if (data.rogueClass == RogueClass.MAGE) {
				cooldown = cooldown * 7 / 10;
			}
			ready[slot] = now + cooldown;
			sync(p, false);
		}
	}

	public static void sync(ServerPlayerEntity p, boolean openScreen) {
		RogueState.PlayerData data = data(p);
		int now = p.getServer().getTicks();
		int[] ready = ready(p);
		ServerPlayNetworking.send(p, new ClassSyncPayload(data.rogueClass.ordinal(), data.spell.ordinal(),
			Math.max(0, ready[0] - now), Math.max(0, ready[1] - now), openScreen));
	}

	public static void applyPassives(ServerPlayerEntity p) {
		RogueClass cls = data(p).rogueClass;
		setModifier(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, switch (cls) {
			case WARRIOR -> 8;
			case NECROMANCER -> 4;
			default -> 0;
		}, EntityAttributeModifier.Operation.ADD_VALUE);
		setModifier(p, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, cls == RogueClass.WARRIOR ? 2 : 0,
			EntityAttributeModifier.Operation.ADD_VALUE);
		setModifier(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, switch (cls) {
			case ARCHER -> 0.15;
			case ASSASSIN -> 0.10;
			default -> 0;
		}, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		setModifier(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_ID, cls == RogueClass.ASSASSIN ? 2 : 0,
			EntityAttributeModifier.Operation.ADD_VALUE);
		// Allonge un peu la portée au corps à corps, plus confortable en vue du dessus
		setModifier(p, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, REACH_ID, 1.0, EntityAttributeModifier.Operation.ADD_VALUE);
		if (p.getHealth() > p.getMaxHealth()) {
			p.setHealth(p.getMaxHealth());
		}
	}

	private static void setModifier(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attribute, Identifier id,
									double value, EntityAttributeModifier.Operation op) {
		EntityAttributeInstance instance = p.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(id);
		if (value != 0) {
			instance.addPersistentModifier(new EntityAttributeModifier(id, value, op));
		}
	}

	private static void giveKit(ServerPlayerEntity p, RogueClass cls) {
		List<ItemStack> kit = new ArrayList<>();
		switch (cls) {
			case WARRIOR -> {
				kit.add(new ItemStack(Items.IRON_SWORD));
				kit.add(new ItemStack(Items.SHIELD));
				kit.add(new ItemStack(Items.IRON_HELMET));
				kit.add(new ItemStack(Items.IRON_CHESTPLATE));
				kit.add(new ItemStack(Items.COOKED_BEEF, 8));
			}
			case ARCHER -> {
				kit.add(new ItemStack(Items.BOW));
				kit.add(new ItemStack(Items.ARROW, 64));
				kit.add(new ItemStack(Items.STONE_SWORD));
				kit.add(new ItemStack(Items.LEATHER_CHESTPLATE));
				kit.add(new ItemStack(Items.LEATHER_BOOTS));
				kit.add(new ItemStack(Items.BREAD, 8));
			}
			case MAGE -> {
				kit.add(new ItemStack(Items.STONE_SWORD));
				kit.add(new ItemStack(Items.LEATHER_HELMET));
				kit.add(PotionContentsComponent.createStack(Items.POTION, Potions.HEALING));
				kit.add(PotionContentsComponent.createStack(Items.POTION, Potions.HEALING));
				kit.add(PotionContentsComponent.createStack(Items.POTION, Potions.HEALING));
				kit.add(new ItemStack(Items.BREAD, 8));
			}
			case ASSASSIN -> {
				kit.add(new ItemStack(Items.IRON_SWORD));
				kit.add(new ItemStack(Items.LEATHER_CHESTPLATE));
				kit.add(new ItemStack(Items.ENDER_PEARL, 4));
				kit.add(new ItemStack(Items.BREAD, 8));
			}
			case NECROMANCER -> {
				kit.add(new ItemStack(Items.STONE_AXE));
				kit.add(new ItemStack(Items.LEATHER_CHESTPLATE));
				kit.add(new ItemStack(Items.BONE, 16));
				kit.add(PotionContentsComponent.createStack(Items.POTION, Potions.HEALING));
				kit.add(new ItemStack(Items.BREAD, 8));
			}
			default -> {
			}
		}
		for (ItemStack stack : kit) {
			p.getInventory().offerOrDrop(stack);
		}
	}

	public static void trackSummon(Entity entity, int lifetime) {
		SUMMONS.put(entity, entity.getServer().getTicks() + lifetime);
	}

	public static void schedule(int tick, Runnable action) {
		TASKS.add(new Task(tick, action));
	}

	public static void tick(MinecraftServer server) {
		int now = server.getTicks();
		Iterator<Map.Entry<Entity, Integer>> it = SUMMONS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Entity, Integer> entry = it.next();
			Entity e = entry.getKey();
			if (e.isRemoved() || entry.getValue() <= now) {
				if (!e.isRemoved() && e.getWorld() instanceof ServerWorld sw) {
					sw.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.3, 0.3, 0.01);
					e.discard();
				}
				it.remove();
			}
		}
		if (!TASKS.isEmpty()) {
			List<Task> due = new ArrayList<>();
			TASKS.removeIf(t -> {
				if (t.tick() <= now) {
					due.add(t);
					return true;
				}
				return false;
			});
			due.forEach(t -> t.action().run());
		}
	}
}
