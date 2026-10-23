package fr.nico.roguetower.loot;

import fr.nico.roguetower.classes.Spell;
import fr.nico.roguetower.item.SpellBookItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/** Génère le contenu des coffres de récompense selon la rareté. */
public final class Loot {
	private Loot() {
	}

	public record Drop(ItemStack stack, Rarity rarity) {
	}

	public static List<Drop> rollChest(ServerWorld world, int floor, boolean bossFloor) {
		Random random = world.getRandom();
		int rolls = Math.min(9, 4 + floor / 5) + (bossFloor ? 2 : 0);
		List<Drop> drops = new ArrayList<>();
		for (int i = 0; i < rolls; i++) {
			Rarity rarity = Rarity.roll(random, floor);
			if (bossFloor && i == 0 && rarity.ordinal() < Rarity.RARE.ordinal()) {
				rarity = Rarity.RARE;
			}
			drops.add(new Drop(roll(world, random, rarity, floor), rarity));
		}
		return drops;
	}

	public static ItemStack roll(ServerWorld world, Random random, Rarity rarity, int floor) {
		double bookChance = switch (rarity) {
			case COMMON -> 0.05;
			case UNCOMMON -> 0.15;
			case RARE -> 0.25;
			case EPIC, LEGENDARY -> 0.30;
		};
		if (random.nextDouble() < bookChance) {
			return SpellBookItem.create(randomSpell(random, rarity));
		}
		return switch (rarity) {
			case COMMON -> common(world, random);
			case UNCOMMON -> uncommon(world, random);
			case RARE -> rare(world, random);
			case EPIC -> epic(world, random);
			case LEGENDARY -> legendary(world, random);
		};
	}

	private static Spell randomSpell(Random random, Rarity rarity) {
		List<Spell> pool = new ArrayList<>();
		for (Spell s : Spell.values()) {
			if (s != Spell.NONE && s.rarity == rarity) {
				pool.add(s);
			}
		}
		if (pool.isEmpty()) {
			for (Spell s : Spell.values()) {
				if (s != Spell.NONE && s.rarity.ordinal() <= rarity.ordinal()) {
					pool.add(s);
				}
			}
		}
		return pool.get(random.nextInt(pool.size()));
	}

	private static ItemStack common(ServerWorld world, Random random) {
		return switch (random.nextInt(8)) {
			case 0 -> new ItemStack(Items.BREAD, 4 + random.nextInt(5));
			case 1 -> new ItemStack(Items.COOKED_PORKCHOP, 3 + random.nextInt(3));
			case 2 -> new ItemStack(Items.ARROW, 12 + random.nextInt(13));
			case 3 -> new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(4));
			case 4 -> new ItemStack(Items.TORCH, 16);
			case 5 -> new ItemStack(Items.STONE_SWORD);
			case 6 -> pick(random, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
			default -> new ItemStack(Items.COOKED_BEEF, 3 + random.nextInt(3));
		};
	}

	private static ItemStack uncommon(ServerWorld world, Random random) {
		return switch (random.nextInt(6)) {
			case 0, 1 -> {
				ItemStack s = pick(random, Items.IRON_SWORD, Items.IRON_AXE, Items.BOW, Items.CROSSBOW);
				weaponEnchants(world, s, 1 + random.nextInt(2));
				yield named(s, nameOf(s) + " renforcé", Rarity.UNCOMMON, null);
			}
			case 2, 3 -> {
				ItemStack s = pick(random, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
				ench(world, s, Enchantments.PROTECTION, 1 + random.nextInt(2));
				yield named(s, nameOf(s) + " renforcé", Rarity.UNCOMMON, null);
			}
			case 4 -> PotionContentsComponent.createStack(Items.POTION, Potions.HEALING);
			default -> new ItemStack(Items.GOLDEN_CARROT, 4 + random.nextInt(4));
		};
	}

	private static ItemStack rare(ServerWorld world, Random random) {
		return switch (random.nextInt(7)) {
			case 0, 1 -> {
				ItemStack s = pick(random, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.BOW, Items.CROSSBOW);
				weaponEnchants(world, s, 2 + random.nextInt(2));
				ench(world, s, Enchantments.UNBREAKING, 2);
				yield named(s, nameOf(s) + " du chevalier", Rarity.RARE, null);
			}
			case 2, 3 -> {
				ItemStack s = pick(random, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
				ench(world, s, Enchantments.PROTECTION, 2 + random.nextInt(2));
				yield named(s, nameOf(s) + " du chevalier", Rarity.RARE, null);
			}
			case 4 -> new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(2));
			case 5 -> PotionContentsComponent.createStack(Items.POTION, Potions.STRONG_HEALING);
			default -> new ItemStack(Items.ENDER_PEARL, 3 + random.nextInt(3));
		};
	}

	private static final String[][] MAGIC_EFFECTS = {
		{"fire", "enflammée", "Embrase la cible"},
		{"frost", "de givre", "Ralentit et gèle la cible"},
		{"poison", "venimeuse", "Empoisonne la cible"},
		{"lifesteal", "vampirique", "Vole de la vie à chaque coup"},
		{"lightning", "de l'orage", "Chance d'invoquer la foudre"},
		{"wither", "maudite", "Inflige le Wither"},
	};

	private static ItemStack epic(ServerWorld world, Random random) {
		int r = random.nextInt(10);
		if (r <= 5) {
			ItemStack s = pick(random, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.NETHERITE_SWORD);
			weaponEnchants(world, s, 3 + random.nextInt(2));
			ench(world, s, Enchantments.UNBREAKING, 3);
			String[] fx = MAGIC_EFFECTS[random.nextInt(MAGIC_EFFECTS.length)];
			return named(s, "Lame " + fx[1], Rarity.EPIC, effect(fx[0], 1, fx[2] + " (I)"));
		}
		if (r <= 7) {
			ItemStack s = pick(random, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
			ench(world, s, Enchantments.PROTECTION, 4);
			ench(world, s, Enchantments.UNBREAKING, 3);
			ench(world, s, Enchantments.THORNS, 2);
			return named(s, nameOf(s) + " runique", Rarity.EPIC, null);
		}
		if (r == 8) {
			return named(new ItemStack(Items.TOTEM_OF_UNDYING), "Totem de résurrection", Rarity.EPIC, null);
		}
		return new ItemStack(Items.GOLDEN_APPLE, 3);
	}

	private static ItemStack legendary(ServerWorld world, Random random) {
		ItemStack s;
		switch (random.nextInt(9)) {
			case 0 -> {
				s = new ItemStack(Items.NETHERITE_SWORD);
				ench(world, s, Enchantments.SHARPNESS, 6);
				ench(world, s, Enchantments.UNBREAKING, 3);
				return named(s, "Excalibur", Rarity.LEGENDARY, effect("lightning", 2, "La foudre frappe souvent tes ennemis"));
			}
			case 1 -> {
				s = new ItemStack(Items.NETHERITE_SWORD);
				ench(world, s, Enchantments.SHARPNESS, 5);
				ench(world, s, Enchantments.FIRE_ASPECT, 2);
				return named(s, "Lame du Phénix", Rarity.LEGENDARY, effect("lifesteal", 2, "Brûle et vole beaucoup de vie"));
			}
			case 2 -> {
				s = new ItemStack(Items.NETHERITE_AXE);
				ench(world, s, Enchantments.SHARPNESS, 6);
				ench(world, s, Enchantments.UNBREAKING, 3);
				return named(s, "Hache du Berserker", Rarity.LEGENDARY, effect("lifesteal", 3, "Chaque coup te soigne énormément"));
			}
			case 3 -> {
				s = new ItemStack(Items.NETHERITE_SWORD);
				ench(world, s, Enchantments.SHARPNESS, 5);
				ench(world, s, Enchantments.SWEEPING_EDGE, 3);
				return named(s, "Croc de Givre", Rarity.LEGENDARY, effect("frost", 3, "Gèle quasiment sur place"));
			}
			case 4 -> {
				s = new ItemStack(Items.BOW);
				ench(world, s, Enchantments.POWER, 6);
				ench(world, s, Enchantments.PUNCH, 2);
				ench(world, s, Enchantments.FLAME, 1);
				ench(world, s, Enchantments.INFINITY, 1);
				return named(s, "Arc de l'Ouragan", Rarity.LEGENDARY, effect("none", 0, "Flèches infinies et enflammées"));
			}
			case 5 -> {
				s = new ItemStack(Items.TRIDENT);
				ench(world, s, Enchantments.IMPALING, 5);
				ench(world, s, Enchantments.LOYALTY, 3);
				ench(world, s, Enchantments.CHANNELING, 1);
				return named(s, "Trident de Poséidon", Rarity.LEGENDARY, effect("lightning", 2, "Invoque la foudre"));
			}
			case 6 -> {
				s = new ItemStack(Items.MACE);
				ench(world, s, Enchantments.DENSITY, 5);
				ench(world, s, Enchantments.WIND_BURST, 2);
				return named(s, "Marteau de Thor", Rarity.LEGENDARY, effect("lightning", 3, "Chaque coup appelle la foudre"));
			}
			case 7 -> {
				s = new ItemStack(Items.NETHERITE_CHESTPLATE);
				ench(world, s, Enchantments.PROTECTION, 5);
				ench(world, s, Enchantments.THORNS, 3);
				ench(world, s, Enchantments.UNBREAKING, 3);
				return named(s, "Armure du Roi-Liche", Rarity.LEGENDARY, null);
			}
			default -> {
				return named(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2), "Pomme des Dieux", Rarity.LEGENDARY, null);
			}
		}
	}

	private static NbtCompound effect(String id, int power, String description) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("rt_effect", id);
		nbt.putInt("rt_power", power);
		nbt.putString("rt_desc", description);
		return nbt;
	}

	private static void weaponEnchants(ServerWorld world, ItemStack s, int level) {
		if (s.isOf(Items.BOW)) {
			ench(world, s, Enchantments.POWER, level);
		} else if (s.isOf(Items.CROSSBOW)) {
			ench(world, s, Enchantments.QUICK_CHARGE, Math.min(3, level));
		} else {
			ench(world, s, Enchantments.SHARPNESS, level);
		}
	}

	public static void ench(ServerWorld world, ItemStack stack, RegistryKey<Enchantment> key, int level) {
		world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key)
			.ifPresent(entry -> stack.addEnchantment(entry, level));
	}

	private static ItemStack pick(Random random, Item... items) {
		return new ItemStack(items[random.nextInt(items.length)]);
	}

	private static String nameOf(ItemStack s) {
		return s.getName().getString();
	}

	public static ItemStack named(ItemStack s, String name, Rarity rarity, NbtCompound effect) {
		s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).styled(st -> st.withItalic(false).withColor(rarity.color)));
		List<Text> lore = new ArrayList<>();
		lore.add(Text.literal("★ " + rarity.displayName).styled(st -> st.withItalic(false).withColor(rarity.color)));
		if (effect != null) {
			NbtCompound data = effect.copy();
			s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
			lore.add(Text.literal(effect.getString("rt_desc")).styled(st -> st.withItalic(true).withColor(Formatting.GRAY)));
		}
		s.set(DataComponentTypes.LORE, new LoreComponent(lore));
		if (rarity.ordinal() >= Rarity.EPIC.ordinal()) {
			s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return s;
	}
}
