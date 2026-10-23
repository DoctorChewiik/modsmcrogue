package fr.nico.roguetower;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.nico.roguetower.block.PortalFrame;
import fr.nico.roguetower.block.TowerPortalBlock;
import fr.nico.roguetower.classes.ClassManager;
import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.classes.Spell;
import fr.nico.roguetower.classes.Spells;
import fr.nico.roguetower.item.SpellBookItem;
import fr.nico.roguetower.loot.Loot;
import fr.nico.roguetower.loot.Rarity;
import fr.nico.roguetower.loot.WeaponEffects;
import fr.nico.roguetower.mob.MobSetup;
import fr.nico.roguetower.combat.CombatManager;
import fr.nico.roguetower.entity.ModEntities;
import fr.nico.roguetower.net.AbilityPayload;
import fr.nico.roguetower.net.CombatAnimPayload;
import fr.nico.roguetower.net.CombatPayload;
import fr.nico.roguetower.net.CombatSyncPayload;
import fr.nico.roguetower.net.ClassSyncPayload;
import fr.nico.roguetower.net.SelectClassPayload;
import fr.nico.roguetower.world.TowerChunkGenerator;
import fr.nico.roguetower.world.TowerLayout;
import fr.nico.roguetower.world.TowerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RogueTower implements ModInitializer {
	public static final String MOD_ID = "roguetower";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final RegistryKey<World> TOWER_WORLD = RegistryKey.of(RegistryKeys.WORLD, id("tower"));

	public static final Block TOWER_PORTAL = Registry.register(Registries.BLOCK, id("tower_portal"),
		new TowerPortalBlock(AbstractBlock.Settings.create()
			.noCollision()
			.strength(-1.0f)
			.luminance(state -> 12)
			.sounds(BlockSoundGroup.GLASS)
			.dropsNothing()
			.pistonBehavior(PistonBehavior.BLOCK)));

	public static final Item SPELL_BOOK = Registry.register(Registries.ITEM, id("spell_book"),
		new SpellBookItem(new Item.Settings().maxCount(1)));

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public static boolean isTower(World world) {
		return world.getRegistryKey() == TOWER_WORLD;
	}

	@Override
	public void onInitialize() {
		Registry.register(Registries.CHUNK_GENERATOR, id("tower"), TowerChunkGenerator.CODEC);

		PayloadTypeRegistry.playC2S().register(AbilityPayload.ID, AbilityPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SelectClassPayload.ID, SelectClassPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ClassSyncPayload.ID, ClassSyncPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CombatPayload.ID, CombatPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CombatSyncPayload.ID, CombatSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CombatAnimPayload.ID, CombatAnimPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CombatPayload.ID,
			(payload, context) -> CombatManager.handle(context.player(), payload));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatManager::allowDamage);
		ModEntities.init();

		ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.ID,
			(payload, context) -> ClassManager.useSlot(context.player(), payload.slot()));
		ServerPlayNetworking.registerGlobalReceiver(SelectClassPayload.ID,
			(payload, context) -> ClassManager.selectClass(context.player(), RogueClass.byId(payload.classId())));

		// Allumer le portail au briquet
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			ItemStack stack = player.getStackInHand(hand);
			if (!stack.isOf(Items.FLINT_AND_STEEL)) {
				return ActionResult.PASS;
			}
			BlockPos inside = hit.getBlockPos().offset(hit.getSide());
			if (!PortalFrame.tryLight(world, inside, !world.isClient)) {
				return ActionResult.PASS;
			}
			if (!world.isClient) {
				world.playSound(null, inside, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1f, 1f);
				world.playSound(null, inside, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.6f, 1.4f);
				if (!player.isCreative()) {
					stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
				}
			}
			return ActionResult.SUCCESS;
		});

		// Effets des armes magiques / légendaires
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof LivingEntity target) {
				CombatManager.onMeleeHit(sp, target);
				WeaponEffects.onHit(sp, target);
			}
			return ActionResult.PASS;
		});

		// Les animaux deviennent agressifs dans la tour (aussi après rechargement du monde)
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity.getCommandTags().contains(Spells.SUMMON_TAG) && !entity.getCommandTags().contains("roguetower_tracked")) {
				entity.addCommandTag("roguetower_tracked");
				ClassManager.trackSummon(entity, 600);
				return;
			}
			if (isTower(world) && entity instanceof MobEntity mob) {
				if (entity.getCommandTags().contains(MobSetup.AGGRO_TAG) && entity.age > 0) {
					MobSetup.makeAggressive(mob, TowerLayout.floorOf(entity.getX()));
				}
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TowerManager.tick(server);
			ClassManager.tick(server);
			CombatManager.tick(server);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			ClassManager.applyPassives(player);
			ClassManager.sync(player, false);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			ClassManager.applyPassives(newPlayer);
			newPlayer.setHealth(newPlayer.getMaxHealth());
			ClassManager.sync(newPlayer, false);
			if (!alive && isTower(oldPlayer.getWorld())) {
				newPlayer.sendMessage(Text.literal("Tu es mort dans la tour. Fin de la partie !").formatted(Formatting.DARK_RED), false);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			CommandManager.literal("roguetower")
				.then(CommandManager.literal("etage")
					.requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("numero", IntegerArgumentType.integer(1, 100000))
						.executes(ctx -> {
							TowerManager.enterFloor(ctx.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(ctx, "numero"));
							return 1;
						})))
				.then(CommandManager.literal("quitter")
					.executes(ctx -> {
						ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
						if (isTower(p.getWorld())) {
							TowerManager.leaveTower(p);
						}
						return 1;
					}))
				.then(CommandManager.literal("classe")
					.executes(ctx -> {
						ClassManager.sync(ctx.getSource().getPlayerOrThrow(), true);
						return 1;
					}))
				.then(CommandManager.literal("chances")
					.executes(ctx -> {
						ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
						int floor = isTower(p.getWorld()) ? Math.max(1, TowerLayout.floorOf(p.getX())) : 1;
						ctx.getSource().sendFeedback(() -> TowerManager.chancesText(floor), false);
						return 1;
					}))
				.then(CommandManager.literal("livre")
					.requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("sort", StringArgumentType.word())
						.executes(ctx -> {
							Spell spell = Spell.byName(StringArgumentType.getString(ctx, "sort"));
							if (spell != Spell.NONE) {
								ctx.getSource().getPlayerOrThrow().getInventory().offerOrDrop(SpellBookItem.create(spell));
							}
							return 1;
						})))
				.then(CommandManager.literal("butin")
					.requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("rarete", StringArgumentType.word())
						.executes(ctx -> {
							ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
							Rarity rarity = Rarity.valueOf(StringArgumentType.getString(ctx, "rarete").toUpperCase());
							p.getInventory().offerOrDrop(Loot.roll(p.getServerWorld(), p.getRandom(), rarity, 10));
							return 1;
						})))
		));

		if (SelfTest.enabled()) {
			ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
		}

		LOGGER.info("Rogue Tower chargé !");
	}
}
