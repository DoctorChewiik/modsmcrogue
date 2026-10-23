package fr.nico.roguetower.world;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.classes.ClassManager;
import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.loot.Loot;
import fr.nico.roguetower.loot.Rarity;
import fr.nico.roguetower.mob.MobSetup;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Gère les entrées/sorties de la tour et le déroulement de chaque étage. */
public final class TowerManager {
	private static final Map<UUID, Integer> PORTAL_COOLDOWN = new HashMap<>();
	private static final Map<UUID, Runnable> PENDING_TELEPORTS = new HashMap<>();
	private static final Map<Integer, FloorState> FLOORS = new HashMap<>();

	private static final class FloorState {
		boolean active;
		boolean cleared;
		int spawnAt = -1;
		int remaining;
	}

	private TowerManager() {
	}

	public static boolean isBossFloor(int floor) {
		return floor % 5 == 0;
	}

	// ---------- Portail ----------

	public static void onPortalContact(ServerPlayerEntity player, BlockPos pos, Direction.Axis axis) {
		int now = player.getServer().getTicks();
		Integer until = PORTAL_COOLDOWN.get(player.getUuid());
		if ((until != null && until > now) || PENDING_TELEPORTS.containsKey(player.getUuid())) {
			return;
		}
		PORTAL_COOLDOWN.put(player.getUuid(), now + 100);

		if (RogueTower.isTower(player.getWorld())) {
			PENDING_TELEPORTS.put(player.getUuid(), () -> leaveTower(player));
			return;
		}

		// Point de retour : 2 blocs devant le portail, du côté où se trouve le joueur
		BlockPos bottom = pos;
		while (player.getWorld().getBlockState(bottom.down()).isOf(RogueTower.TOWER_PORTAL)) {
			bottom = bottom.down();
		}
		double side;
		Vec3d back;
		if (axis == Direction.Axis.X) {
			side = Math.signum(player.getZ() - (pos.getZ() + 0.5));
			back = new Vec3d(player.getX(), bottom.getY(), pos.getZ() + 0.5 + (side == 0 ? 1 : side) * 2);
		} else {
			side = Math.signum(player.getX() - (pos.getX() + 0.5));
			back = new Vec3d(pos.getX() + 0.5 + (side == 0 ? 1 : side) * 2, bottom.getY(), player.getZ());
		}
		RogueState.PlayerData data = ClassManager.data(player);
		data.returnDimension = player.getWorld().getRegistryKey().getValue().toString();
		data.returnX = back.x;
		data.returnY = back.y;
		data.returnZ = back.z;
		RogueState.get(player.getServer()).markDirty();

		PENDING_TELEPORTS.put(player.getUuid(), () -> enterFloor(player, 1));
	}

	public static void enterFloor(ServerPlayerEntity player, int floor) {
		ServerWorld tower = player.getServer().getWorld(RogueTower.TOWER_WORLD);
		if (tower == null) {
			player.sendMessage(Text.literal("La dimension de la tour est introuvable.").formatted(Formatting.RED), false);
			return;
		}
		Vec3d entry = TowerLayout.entry(floor);
		player.teleport(tower, entry.x, entry.y, entry.z, -90f, 0f);
		player.fallDistance = 0;
		PORTAL_COOLDOWN.put(player.getUuid(), player.getServer().getTicks() + 60);

		RogueState.PlayerData data = ClassManager.data(player);
		if (floor > data.bestFloor) {
			data.bestFloor = floor;
			RogueState.get(player.getServer()).markDirty();
		}

		player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Étage " + floor)
			.formatted(isBossFloor(floor) ? Formatting.DARK_RED : Formatting.GOLD)));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(isBossFloor(floor)
			? "Un gardien vous attend..." : "Éliminez tous les ennemis").formatted(Formatting.GRAY)));
		tower.playSound(null, entry.x, entry.y, entry.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.5f, 1.2f);

		FloorState state = FLOORS.computeIfAbsent(floor, f -> new FloorState());
		boolean someoneElseHere = tower.getPlayers().stream()
			.anyMatch(p -> p != player && TowerLayout.floorOf(p.getX()) == floor);
		if (!someoneElseHere && countMobs(tower, floor) == 0) {
			// Étage vide : on le (ré)arme pour une nouvelle manche
			state.active = false;
			state.cleared = false;
			state.spawnAt = player.getServer().getTicks() + 40;
			clearRewardChest(tower, floor);
		}

		if (ClassManager.data(player).rogueClass == RogueClass.NONE) {
			ClassManager.sync(player, true);
		}
	}

	public static void leaveTower(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		RogueState.PlayerData data = ClassManager.data(player);
		ServerWorld target = null;
		if (!data.returnDimension.isEmpty()) {
			Identifier id = Identifier.tryParse(data.returnDimension);
			if (id != null) {
				target = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
			}
		}
		if (target == null) {
			target = server.getOverworld();
			BlockPos spawn = target.getSpawnPos();
			player.teleport(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYaw(), 0f);
		} else {
			player.teleport(target, data.returnX, data.returnY, data.returnZ, player.getYaw(), 0f);
		}
		player.fallDistance = 0;
		PORTAL_COOLDOWN.put(player.getUuid(), server.getTicks() + 100);
		player.sendMessage(Text.literal("Tu quittes la tour. Record : étage " + data.bestFloor).formatted(Formatting.GOLD), false);
	}

	// ---------- Boucle de jeu ----------

	public static void tick(MinecraftServer server) {
		if (!PENDING_TELEPORTS.isEmpty()) {
			Map<UUID, Runnable> copy = new HashMap<>(PENDING_TELEPORTS);
			PENDING_TELEPORTS.clear();
			copy.values().forEach(Runnable::run);
		}

		ServerWorld tower = server.getWorld(RogueTower.TOWER_WORLD);
		if (tower == null) {
			return;
		}
		int now = server.getTicks();

		for (Map.Entry<Integer, FloorState> entry : FLOORS.entrySet()) {
			int floor = entry.getKey();
			FloorState state = entry.getValue();
			if (state.spawnAt >= 0 && now >= state.spawnAt) {
				state.spawnAt = -1;
				spawnWave(tower, floor);
				state.active = true;
			}
		}

		if (now % 10 != 0) {
			return;
		}

		for (ServerPlayerEntity player : List.copyOf(tower.getPlayers())) {
			if (player.isSpectator()) {
				continue;
			}
			int floor = TowerLayout.floorOf(player.getX());
			if (floor < 1) {
				continue;
			}
			// Chute dans le vide : retour au début de l'étage
			if (player.getY() < TowerLayout.FLOOR_Y - 14) {
				Vec3d entry = TowerLayout.entry(floor);
				player.teleport(tower, entry.x, entry.y, entry.z, -90f, 0f);
				player.fallDistance = 0;
				player.damage(player.getDamageSources().outOfWorld(), 4f);
				continue;
			}
			FloorState state = FLOORS.computeIfAbsent(floor, f -> new FloorState());
			if (state.active && !state.cleared) {
				state.remaining = countMobs(tower, floor);
				if (state.remaining == 0) {
					clearFloor(tower, floor, state);
				}
			}

			// Plateforme de sortie (obsidienne pleureuse)
			BlockState below = tower.getBlockState(player.getBlockPos().down());
			int dx = player.getBlockX() - TowerLayout.centerX(floor);
			if (below.isOf(Blocks.CRYING_OBSIDIAN) && TowerLayout.isExitPad(dx, player.getBlockZ())) {
				if (player.isSneaking()) {
					PENDING_TELEPORTS.put(player.getUuid(), () -> leaveTower(player));
				} else if (state.cleared) {
					PENDING_TELEPORTS.put(player.getUuid(), () -> enterFloor(player, floor + 1));
				} else {
					player.sendMessage(Text.literal("Élimine tous les ennemis d'abord (" + state.remaining
						+ " restants) — accroupis-toi pour quitter la tour").formatted(Formatting.RED), true);
				}
				continue;
			}

			if (now % 20 == 0) {
				MutableText hud = Text.literal("Étage " + floor).formatted(Formatting.GOLD);
				if (state.cleared) {
					hud.append(Text.literal("  ✔ terminé — va sur la plateforme violette").formatted(Formatting.GREEN));
				} else if (state.active) {
					hud.append(Text.literal("  ennemis restants : " + state.remaining).formatted(Formatting.RED));
				}
				player.sendMessage(hud, true);
			}
		}
	}

	private static Box floorBox(int floor) {
		int cx = TowerLayout.centerX(floor);
		return new Box(cx - 60, 0, -60, cx + 60, 256, 60);
	}

	public static int countMobs(ServerWorld world, int floor) {
		return world.getEntitiesByClass(MobEntity.class, floorBox(floor),
			e -> e.isAlive() && e.getCommandTags().contains(MobSetup.MOB_TAG)).size();
	}

	private static void spawnWave(ServerWorld world, int floor) {
		Random random = world.getRandom();
		int count = Math.min(40, 6 + floor * 2);
		ServerPlayerEntity target = world.getPlayers().stream()
			.filter(p -> TowerLayout.floorOf(p.getX()) == floor).findFirst().orElse(null);
		int cx = TowerLayout.centerX(floor);
		int spawned = 0;
		for (int attempt = 0; attempt < count * 6 && spawned < count; attempt++) {
			BlockPos pos = randomSpot(world, random, cx);
			if (pos == null) {
				continue;
			}
			EntityType<?> type = MobSetup.randomMob(floor, random);
			if (MobSetup.spawn(world, type, pos, floor, false, target) != null) {
				spawned++;
			}
		}
		if (isBossFloor(floor)) {
			BlockPos bossPos = new BlockPos(cx + 10, TowerLayout.FLOOR_Y + 1, 0);
			MobSetup.spawn(world, MobSetup.randomBoss(random), bossPos, floor, true, target);
		}
	}

	private static BlockPos randomSpot(ServerWorld world, Random random, int cx) {
		double angle = random.nextDouble() * Math.PI * 2;
		double dist = 4 + random.nextDouble() * (TowerLayout.RADIUS - 8);
		int x = cx + (int) Math.round(Math.cos(angle) * dist);
		int z = (int) Math.round(Math.sin(angle) * dist);
		if (x - cx < -14) {
			return null; // pas trop près de l'entrée
		}
		BlockPos pos = new BlockPos(x, TowerLayout.FLOOR_Y + 1, z);
		BlockState floorBlock = world.getBlockState(pos.down());
		if (!floorBlock.isSolidBlock(world, pos.down()) || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
			return null;
		}
		return pos;
	}

	private static void clearFloor(ServerWorld world, int floor, FloorState state) {
		state.cleared = true;
		boolean boss = isBossFloor(floor);
		BlockPos chestPos = TowerLayout.rewardChest(floor);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
		Rarity best = Rarity.COMMON;
		BlockEntity be = world.getBlockEntity(chestPos);
		if (be instanceof ChestBlockEntity chest) {
			List<Loot.Drop> drops = Loot.rollChest(world, floor, boss);
			int slot = 0;
			for (Loot.Drop drop : drops) {
				chest.setStack(slot, drop.stack());
				slot += 2 + world.getRandom().nextInt(2);
				if (drop.rarity().ordinal() > best.ordinal()) {
					best = drop.rarity();
				}
			}
		}
		ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(chestPos), 10 + floor * 3);

		for (ServerPlayerEntity p : world.getPlayers()) {
			if (TowerLayout.floorOf(p.getX()) != floor) {
				continue;
			}
			p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Étage terminé !").formatted(Formatting.GREEN)));
			p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Meilleur butin : ")
				.formatted(Formatting.GRAY).append(Text.literal(best.displayName).formatted(best.color))));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
			world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.7f, 1f);
			p.sendMessage(chancesText(floor + 1), false);
		}
	}

	public static Text chancesText(int floor) {
		MutableText text = Text.literal("Chances de butin à l'étage " + floor + " : ").formatted(Formatting.GRAY);
		for (Rarity r : Rarity.values()) {
			text.append(Text.literal(String.format("%s %.1f%%  ", r.displayName, r.chance(floor))).formatted(r.color));
		}
		return text;
	}

	private static void clearRewardChest(ServerWorld world, int floor) {
		BlockPos chestPos = TowerLayout.rewardChest(floor);
		if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			chest.clear();
		}
		if (!world.getBlockState(chestPos).isAir()) {
			world.setBlockState(chestPos, Blocks.AIR.getDefaultState());
		}
	}

}
