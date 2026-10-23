package fr.nico.roguetower.client;

import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.classes.Spell;

/** État reçu du serveur. */
public final class ClientData {
	public static RogueClass rogueClass = RogueClass.NONE;
	public static Spell spell = Spell.NONE;
	/** Tick client auquel la compétence / le sort seront prêts. */
	public static long abilityReady;
	public static long spellReady;
	public static long clientTicks;
	public static int stamina = 100;
	public static int combo;
	public static int lastEvent;
	public static long lastEventTick = -100;

	private ClientData() {
	}
}
