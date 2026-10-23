package fr.nico.roguetower.classes;

import fr.nico.roguetower.loot.Rarity;

/** Tous les pouvoirs du mod. Une classe a une compétence fixe (R) et un sort remplaçable (F). */
public enum Spell {
	NONE("Aucun", "", 0, Rarity.COMMON),
	WHIRLWIND("Tourbillon", "Frappe tous les ennemis autour de toi et les repousse.", 160, Rarity.UNCOMMON),
	WAR_CRY("Cri de guerre", "Force et Résistance pendant 8 s.", 600, Rarity.UNCOMMON),
	VOLLEY("Salve de flèches", "Tire 7 flèches en éventail.", 120, Rarity.UNCOMMON),
	LEAP("Bond", "Bond rapide dans la direction visée.", 140, Rarity.COMMON),
	FIREBOLT("Trait de feu", "Rayon de feu qui brûle le premier ennemi touché.", 40, Rarity.COMMON),
	FROST_NOVA("Nova de givre", "Gèle et blesse les ennemis proches.", 300, Rarity.RARE),
	SHADOW_STEP("Pas de l'ombre", "Téléportation de 8 blocs vers l'avant.", 100, Rarity.UNCOMMON),
	SHADOW_VEIL("Voile d'ombre", "Invisibilité et vitesse pendant 6 s.", 500, Rarity.RARE),
	SPECTRAL_PACK("Meute spectrale", "Invoque 3 loups alliés pendant 40 s.", 600, Rarity.RARE),
	LIFE_DRAIN("Drain de vie", "Vole la vie de l'ennemi visé.", 160, Rarity.UNCOMMON),
	HEAL("Soin", "Rend 8 PV et donne Régénération.", 400, Rarity.UNCOMMON),
	SHIELD("Bouclier arcanique", "Absorption IV et Résistance pendant 10 s.", 700, Rarity.RARE),
	LIGHTNING("Foudre", "Frappe l'ennemi visé et ses voisins avec la foudre.", 240, Rarity.EPIC),
	ARCANE_BLAST("Explosion arcanique", "Explosion autour de toi qui ne casse pas les blocs.", 300, Rarity.EPIC),
	TORNADO("Tornade", "Projette tous les ennemis dans les airs.", 360, Rarity.EPIC),
	METEOR("Pluie de météores", "Fait pleuvoir des explosions sur la zone visée.", 600, Rarity.LEGENDARY),
	DEATH_MARK("Marque de la mort", "Inflige d'énormes dégâts à tous les ennemis proches.", 900, Rarity.LEGENDARY);

	public final String displayName;
	public final String description;
	public final int cooldown;
	public final Rarity rarity;

	Spell(String displayName, String description, int cooldown, Rarity rarity) {
		this.displayName = displayName;
		this.description = description;
		this.cooldown = cooldown;
		this.rarity = rarity;
	}

	public static Spell byId(int id) {
		Spell[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}

	public static Spell byName(String name) {
		for (Spell s : values()) {
			if (s.name().equalsIgnoreCase(name)) {
				return s;
			}
		}
		return NONE;
	}
}
