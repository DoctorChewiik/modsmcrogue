package fr.nico.roguetower;

import fr.nico.roguetower.block.PortalFrame;
import fr.nico.roguetower.classes.Spell;
import fr.nico.roguetower.item.SpellBookItem;
import fr.nico.roguetower.loot.Loot;
import fr.nico.roguetower.loot.Rarity;
import fr.nico.roguetower.world.TowerLayout;
import fr.nico.roguetower.world.TowerManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Test automatique lancé en intégration continue (-Droguetower.selftest=true). */
public final class SelfTest {
	private SelfTest() {
	}

	public static boolean enabled() {
		return Boolean.getBoolean("roguetower.selftest");
	}

	public static void run(MinecraftServer server) {
		try {
			ServerWorld tower = server.getWorld(RogueTower.TOWER_WORLD);
			check(tower != null, "tower dimension missing");
			for (int floor = 1; floor <= 6; floor++) {
				int cx = TowerLayout.centerX(floor);
				for (int x = cx - 48; x <= cx + 48; x += 16) {
					for (int z = -48; z <= 48; z += 16) {
						tower.getChunk(x >> 4, z >> 4);
					}
				}
				check(tower.getBlockState(new BlockPos(cx + 28, TowerLayout.FLOOR_Y, 0)).isOf(Blocks.CRYING_OBSIDIAN),
					"exit pad missing on floor " + floor);
			}
			TowerManager.spawnWave(tower, 1);
			TowerManager.spawnWave(tower, 5);
			int mobs1 = TowerManager.countMobs(tower, 1);
			int mobs5 = TowerManager.countMobs(tower, 5);
			RogueTower.LOGGER.info("[selftest] mobs floor1={} floor5={}", mobs1, mobs5);
			check(mobs1 > 0 && mobs5 > 0, "no mobs spawned");

			int x0 = TowerLayout.centerX(2);
			int i = 0;
			for (net.minecraft.entity.EntityType<?> type : fr.nico.roguetower.entity.ModEntities.all()) {
				BlockPos pos = new BlockPos(x0 - 10 + (i++ % 5) * 4, TowerLayout.FLOOR_Y + 1, -10 + (i / 5) * 6);
				check(fr.nico.roguetower.mob.MobSetup.spawn(tower, type, pos, 2, false, null) != null, "spawn " + type);
			}
			for (int t = 0; t < 40; t++) {
				tower.tick(() -> true);
			}
			RogueTower.LOGGER.info("[selftest] custom mobs alive after 40 ticks: {}", TowerManager.countMobs(tower, 2));

			TowerManager.forceClear(tower, 3);
			check(tower.getBlockEntity(TowerLayout.rewardChest(3)) instanceof ChestBlockEntity chest && !chest.isEmpty(),
				"reward chest empty");

			for (Rarity rarity : Rarity.values()) {
				for (int i = 0; i < 60; i++) {
					check(!Loot.roll(tower, tower.getRandom(), rarity, 10).isEmpty(), "empty loot " + rarity);
				}
				RogueTower.LOGGER.info("[selftest] {} chance floor1={} floor20={}", rarity, rarity.chance(1), rarity.chance(20));
			}
			for (Spell spell : Spell.values()) {
				if (spell != Spell.NONE) {
					check(SpellBookItem.spellOf(SpellBookItem.create(spell)) == spell, "spell book " + spell);
				}
			}

			ServerWorld overworld = server.getOverworld();
			BlockPos base = new BlockPos(0, 200, 0);
			for (int i = -1; i <= 4; i++) {
				for (int j = -1; j <= 4; j++) {
					boolean frame = i == -1 || i == 4 || j == -1 || j == 4;
					overworld.setBlockState(base.east(i).up(j), frame ? Blocks.COBBLESTONE.getDefaultState() : Blocks.AIR.getDefaultState());
				}
			}
			check(PortalFrame.tryLight(overworld, base.east(1).up(1), true), "portal frame not detected");
			check(overworld.getBlockState(base.east(3).up(3)).isOf(RogueTower.TOWER_PORTAL), "portal not placed");
			check(overworld.getBlockState(base.up(2)).get(net.minecraft.state.property.Properties.HORIZONTAL_AXIS) == Direction.Axis.X,
				"portal axis");
			check(!PortalFrame.tryLight(overworld, new BlockPos(100, 200, 100), false), "false positive portal");

			RogueTower.LOGGER.info("ROGUETOWER SELFTEST OK");
		} catch (Throwable t) {
			RogueTower.LOGGER.error("ROGUETOWER SELFTEST FAILED", t);
		}
		server.stop(false);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
