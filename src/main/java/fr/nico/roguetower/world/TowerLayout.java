package fr.nico.roguetower.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Géométrie de la tour : chaque étage est une grande salle circulaire.
 * Les étages sont alignés sur l'axe X (un étage tous les 512 blocs) pour
 * pouvoir en générer une infinité sans limite de hauteur.
 */
public final class TowerLayout {
	public static final int SPACING = 512;
	public static final int RADIUS = 34;
	public static final int FLOOR_Y = 64;
	public static final int WALL_TOP = 68;

	private TowerLayout() {
	}

	public static int floorOf(double x) {
		return Math.floorDiv((int) Math.floor(x) + SPACING / 2, SPACING);
	}

	public static int centerX(int floor) {
		return floor * SPACING;
	}

	public static Vec3d entry(int floor) {
		return new Vec3d(centerX(floor) - 28 + 0.5, FLOOR_Y + 1, 0.5);
	}

	public static BlockPos rewardChest(int floor) {
		return new BlockPos(centerX(floor) + 24, FLOOR_Y + 1, 0);
	}

	public static boolean isExitPad(int dx, int dz) {
		return dx >= 27 && dx <= 29 && Math.abs(dz) <= 1;
	}

	private record Theme(BlockState floor, BlockState floorAlt, BlockState wall, BlockState accent, BlockState light) {
	}

	private static final Theme[] THEMES = {
		new Theme(Blocks.STONE_BRICKS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
			Blocks.STONE_BRICKS.getDefaultState(), Blocks.CHISELED_STONE_BRICKS.getDefaultState(), Blocks.GLOWSTONE.getDefaultState()),
		new Theme(Blocks.DEEPSLATE_BRICKS.getDefaultState(), Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),
			Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.CHISELED_DEEPSLATE.getDefaultState(), Blocks.SEA_LANTERN.getDefaultState()),
		new Theme(Blocks.NETHER_BRICKS.getDefaultState(), Blocks.RED_NETHER_BRICKS.getDefaultState(),
			Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Blocks.GILDED_BLACKSTONE.getDefaultState(), Blocks.SHROOMLIGHT.getDefaultState()),
		new Theme(Blocks.END_STONE_BRICKS.getDefaultState(), Blocks.PURPUR_BLOCK.getDefaultState(),
			Blocks.PURPUR_PILLAR.getDefaultState(), Blocks.PURPUR_BLOCK.getDefaultState(), Blocks.PEARLESCENT_FROGLIGHT.getDefaultState()),
		new Theme(Blocks.PRISMARINE_BRICKS.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState(),
			Blocks.PRISMARINE.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState(), Blocks.SEA_LANTERN.getDefaultState()),
	};

	private static Theme theme(int floor) {
		return THEMES[Math.floorMod((floor - 1) / 5, THEMES.length)];
	}

	public static long hash(long a, long b, long c) {
		long h = a * 341873128712L + b * 132897987541L + c * 42317861L + 0x9E3779B97F4A7C15L;
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h & Long.MAX_VALUE;
	}

	/** Vrai si la colonne (x, z) peut contenir des blocs de la tour. */
	public static boolean inTower(int x, int z) {
		int floor = floorOf(x);
		if (floor < 1) {
			return false;
		}
		int dx = x - centerX(floor);
		return dx * dx + z * z <= (RADIUS + 2) * (RADIUS + 2);
	}

	/** Bloc à la position donnée, ou null pour de l'air. */
	public static BlockState blockAt(int x, int y, int z) {
		int floor = floorOf(x);
		if (floor < 1 || y < FLOOR_Y - 4 || y > WALL_TOP + 1) {
			return null;
		}
		int dx = x - centerX(floor);
		int dz = z;
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist > RADIUS + 1) {
			return null;
		}
		Theme t = theme(floor);

		// Mur d'enceinte
		if (dist > RADIUS - 1) {
			if (y <= WALL_TOP) {
				double angle = Math.atan2(dz, dx);
				int seg = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * 48);
				if (y == WALL_TOP - 1 && seg % 4 == 0) {
					return t.light();
				}
				return t.wall();
			}
			double angle = Math.atan2(dz, dx);
			int seg = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * 96);
			return seg % 2 == 0 ? t.wall() : null;
		}

		// Sol
		if (y < FLOOR_Y) {
			return t.wall();
		}
		if (y == FLOOR_Y) {
			if (isExitPad(dx, dz)) {
				return Blocks.CRYING_OBSIDIAN.getDefaultState();
			}
			if (dx >= 26 && dx <= 30 && Math.abs(dz) <= 2) {
				return Blocks.GOLD_BLOCK.getDefaultState();
			}
			if (dx >= -29 && dx <= -27 && Math.abs(dz) <= 1) {
				return t.accent();
			}
		}

		// Obstacles générés aléatoirement par cellule de 7x7
		int cellX = Math.floorDiv(dx + 3, 7);
		int cellZ = Math.floorDiv(dz + 3, 7);
		int lx = dx - cellX * 7;
		int lz = dz - cellZ * 7;
		boolean protectedCell = cellX <= -3 || (cellX >= 3 && Math.abs(cellZ) <= 1) || dist > RADIUS - 4;
		long h = hash(floor, cellX, cellZ);
		int type = protectedCell ? -1 : (int) (h % 12);
		int ax = Math.abs(lx);
		int az = Math.abs(lz);

		if (y == FLOOR_Y) {
			if (type == 3 && floor >= 3 && ax <= 1 && az <= 1) {
				return Blocks.LAVA.getDefaultState();
			}
			if (Math.floorMod(dx, 9) == 0 && Math.floorMod(dz, 9) == 0) {
				return t.light();
			}
			return hash(x, y, z) % 5 == 0 ? t.floorAlt() : t.floor();
		}

		int above = y - FLOOR_Y; // 1..5
		switch (type) {
			case 0, 1, 2 -> { // pilier
				if (ax <= 1 && az <= 1) {
					if (above <= 3) {
						return (ax == 0 && az == 0 && above == 2) ? t.accent() : t.wall();
					}
					if (above == 4 && ax == 0 && az == 0) {
						return t.light();
					}
				}
			}
			case 4 -> { // croix de murets
				if (above <= 2 && ((lx == 0 && az <= 2) || (lz == 0 && ax <= 2)) && !(ax == 0 && az == 0)) {
					return t.wall();
				}
			}
			case 5 -> { // ruines
				if (above <= 2 && ax <= 2 && az <= 2 && hash(x, y * 7, z) % 3 == 0) {
					return above == 1 ? t.floorAlt() : t.wall();
				}
			}
			case 6 -> { // toiles d'araignée
				if (floor >= 2 && above == 1 && ax == 2 && az == 2) {
					return Blocks.COBWEB.getDefaultState();
				}
			}
			default -> {
			}
		}
		return null;
	}
}
