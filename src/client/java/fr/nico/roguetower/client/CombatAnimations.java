package fr.nico.roguetower.client;

import fr.nico.roguetower.net.CombatAnimPayload;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/** Animations de combat en cours, par entité (jouées côté client uniquement, purement visuelles). */
public final class CombatAnimations {
	public record Anim(int type, float startAge, float dirX, float dirZ) {
		public float duration() {
			return switch (type) {
				case CombatAnimPayload.DODGE -> 8f;
				case CombatAnimPayload.PARRY -> 9f;
				case CombatAnimPayload.PARRY_SUCCESS -> 7f;
				case CombatAnimPayload.FINISHER -> 8f;
				case CombatAnimPayload.THRUST -> 6f;
				case CombatAnimPayload.CAST -> 10f;
				default -> 6f;
			};
		}
	}

	private static final Map<Integer, Anim> ANIMS = new HashMap<>();

	private CombatAnimations() {
	}

	public static void start(Entity entity, CombatAnimPayload payload) {
		ANIMS.put(entity.getId(), new Anim(payload.anim(), entity.age, payload.dirX(), payload.dirZ()));
	}

	/** Animation active et sa progression (0..1), ou null. */
	public static Anim get(Entity entity) {
		Anim anim = ANIMS.get(entity.getId());
		if (anim == null) {
			return null;
		}
		if (entity.age - anim.startAge() > anim.duration() + 1 || entity.age < anim.startAge()) {
			ANIMS.remove(entity.getId());
			return null;
		}
		return anim;
	}

	public static float progress(Anim anim, float animationProgress) {
		return Math.max(0f, Math.min(1f, (animationProgress - anim.startAge()) / anim.duration()));
	}

	public static void clear() {
		ANIMS.clear();
	}
}
