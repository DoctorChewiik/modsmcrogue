package fr.nico.roguetower.loot;

import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

public enum Rarity {
	COMMON("Commun", Formatting.WHITE, 50.0, 0.0),
	UNCOMMON("Peu commun", Formatting.GREEN, 27.0, 0.5),
	RARE("Rare", Formatting.AQUA, 14.0, 0.6),
	EPIC("Épique", Formatting.LIGHT_PURPLE, 7.0, 0.35),
	LEGENDARY("Légendaire", Formatting.GOLD, 2.0, 0.2);

	public final String displayName;
	public final Formatting color;
	/** Poids de base (en %) à l'étage 1. */
	public final double baseWeight;
	/** Points de % gagnés par étage (plafonnés), les raretés montent avec l'étage. */
	public final double perFloor;

	Rarity(String displayName, Formatting color, double baseWeight, double perFloor) {
		this.displayName = displayName;
		this.color = color;
		this.baseWeight = baseWeight;
		this.perFloor = perFloor;
	}

	public double weight(int floor) {
		double bonus = Math.min(floor - 1, 40) * perFloor;
		if (this == COMMON) {
			return Math.max(10.0, baseWeight - Math.min(floor - 1, 40) * 1.6);
		}
		return baseWeight + bonus;
	}

	/** Pourcentage de chance réel à cet étage (affiché au joueur). */
	public double chance(int floor) {
		double total = 0;
		for (Rarity r : values()) {
			total += r.weight(floor);
		}
		return weight(floor) * 100.0 / total;
	}

	public static Rarity roll(Random random, int floor) {
		double total = 0;
		for (Rarity r : values()) {
			total += r.weight(floor);
		}
		double pick = random.nextDouble() * total;
		for (Rarity r : values()) {
			pick -= r.weight(floor);
			if (pick <= 0) {
				return r;
			}
		}
		return COMMON;
	}
}
