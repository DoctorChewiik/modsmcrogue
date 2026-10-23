package fr.nico.roguetower.classes;

public enum RogueClass {
	NONE("Aucune", "", Spell.NONE, Spell.NONE),
	WARRIOR("Guerrier", "Robuste au corps à corps. +8 PV, +2 armure.", Spell.WHIRLWIND, Spell.WAR_CRY),
	ARCHER("Archer", "Rapide et mortel à distance. +15% vitesse.", Spell.VOLLEY, Spell.LEAP),
	MAGE("Mage", "Fragile mais dévastateur. Sorts rechargés 30% plus vite.", Spell.FROST_NOVA, Spell.FIREBOLT),
	ASSASSIN("Assassin", "Frappe vite et disparaît. +2 dégâts, +10% vitesse.", Spell.SHADOW_STEP, Spell.SHADOW_VEIL),
	NECROMANCER("Nécromancien", "Invoque des alliés et vole la vie. +4 PV.", Spell.LIFE_DRAIN, Spell.SPECTRAL_PACK);

	public final String displayName;
	public final String description;
	/** Compétence de classe (touche R), ne change jamais. */
	public final Spell ability;
	/** Sort de départ (touche F), remplaçable par un livre de sort. */
	public final Spell startingSpell;

	RogueClass(String displayName, String description, Spell ability, Spell startingSpell) {
		this.displayName = displayName;
		this.description = description;
		this.ability = ability;
		this.startingSpell = startingSpell;
	}

	public static RogueClass byId(int id) {
		RogueClass[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}

	public static RogueClass byName(String name) {
		for (RogueClass c : values()) {
			if (c.name().equalsIgnoreCase(name)) {
				return c;
			}
		}
		return NONE;
	}
}
