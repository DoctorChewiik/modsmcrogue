package fr.nico.roguetower.client.render;

import fr.nico.roguetower.RogueTower;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Crée les textures des monstres inédits à partir des formes vanilla, recolorées
 * au lancement du jeu (aucune texture Mojang n'est redistribuée dans le mod).
 */
public final class RecoloredTextures {
	@FunctionalInterface
	public interface Recolor {
		/** Reçoit r, g, b, a (0-255) et la position du pixel ; renvoie la couleur ARGB. */
		int apply(int r, int g, int b, int a, int x, int y);
	}

	private static final Map<Identifier, Identifier> CACHE = new HashMap<>();

	private RecoloredTextures() {
	}

	public static Identifier get(String name, Identifier source, Recolor recolor) {
		Identifier target = RogueTower.id("dynamic/" + name);
		Identifier cached = CACHE.get(target);
		if (cached != null) {
			return cached;
		}
		Identifier result = source;
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			Optional<Resource> resource = client.getResourceManager().getResource(source);
			if (resource.isPresent()) {
				try (InputStream in = resource.get().getInputStream()) {
					NativeImage image = NativeImage.read(in);
					for (int y = 0; y < image.getHeight(); y++) {
						for (int x = 0; x < image.getWidth(); x++) {
							int abgr = image.getColor(x, y);
							int a = (abgr >>> 24) & 0xFF;
							if (a == 0) {
								continue;
							}
							int b = (abgr >>> 16) & 0xFF;
							int g = (abgr >>> 8) & 0xFF;
							int r = abgr & 0xFF;
							int argb = recolor.apply(r, g, b, a, x, y);
							int na = (argb >>> 24) & 0xFF;
							int nr = (argb >>> 16) & 0xFF;
							int ng = (argb >>> 8) & 0xFF;
							int nb = argb & 0xFF;
							image.setColor(x, y, (na << 24) | (nb << 16) | (ng << 8) | nr);
						}
					}
					client.getTextureManager().registerTexture(target, new NativeImageBackedTexture(image));
					result = target;
				}
			}
		} catch (Exception e) {
			RogueTower.LOGGER.warn("Texture {} non générée", name, e);
		}
		CACHE.put(target, result);
		return result;
	}

	// ---------- Outils de couleur ----------

	public static int lum(int r, int g, int b) {
		return (r * 30 + g * 59 + b * 11) / 100;
	}

	public static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, v));
	}

	public static int argb(int a, double r, double g, double b) {
		return (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	/** Teinte : garde la luminosité, applique une couleur. */
	public static int tint(int r, int g, int b, int a, double tr, double tg, double tb, double add) {
		int l = lum(r, g, b);
		return argb(a, l * tr + add, l * tg + add, l * tb + add);
	}

	public static int noise(int x, int y, int salt) {
		int h = x * 374761393 + y * 668265263 + salt * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		return (h ^ (h >>> 16)) & 0x7FFFFFFF;
	}
}
