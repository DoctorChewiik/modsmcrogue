package fr.nico.roguetower.entity;

import fr.nico.roguetower.RogueTower;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

/** Les monstres inédits de la tour (modèles et textures 100 % originaux). */
public final class ModEntities {
	private static final List<Item> SPAWN_EGGS = new ArrayList<>();

	public static final EntityType<GluttonEntity> GLUTTON = register("glutton",
		EntityType.Builder.create(GluttonEntity::new, SpawnGroup.MONSTER).dimensions(1.1f, 1.1f).eyeHeight(0.8f).maxTrackingRange(8),
		0x962896, 0xFAE63C);
	public static final EntityType<BladeMantisEntity> BLADE_MANTIS = register("blade_mantis",
		EntityType.Builder.create(BladeMantisEntity::new, SpawnGroup.MONSTER).dimensions(0.8f, 1.6f).eyeHeight(1.45f).maxTrackingRange(8),
		0x46AA3C, 0xE1E6EB);
	public static final EntityType<HowlingTotemEntity> HOWLING_TOTEM = register("howling_totem",
		EntityType.Builder.create(HowlingTotemEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 1.95f).eyeHeight(1.6f).maxTrackingRange(10),
		0x785032, 0x50E6FF);
	public static final EntityType<CrawlerEntity> CRAWLER = register("crawler",
		EntityType.Builder.create(CrawlerEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 0.55f).eyeHeight(0.4f).maxTrackingRange(8),
		0x96461E, 0xF0C828);
	public static final EntityType<SporelingEntity> SPORELING = register("sporeling",
		EntityType.Builder.create(SporelingEntity::new, SpawnGroup.MONSTER).dimensions(0.8f, 0.9f).eyeHeight(0.5f).maxTrackingRange(8),
		0x287896, 0x96FFE6);
	public static final EntityType<EmberGargoyleEntity> EMBER_GARGOYLE = register("ember_gargoyle",
		EntityType.Builder.create(EmberGargoyleEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 1.4f).eyeHeight(1.2f).makeFireImmune().maxTrackingRange(10),
		0x2D2A30, 0xFF7814);
	public static final EntityType<VoidEyeEntity> VOID_EYE = register("void_eye",
		EntityType.Builder.create(VoidEyeEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 0.9f).eyeHeight(0.45f).maxTrackingRange(10),
		0x2E0F4F, 0xF2F2F2);
	public static final EntityType<RuneGolemEntity> RUNE_GOLEM = register("rune_golem",
		EntityType.Builder.create(RuneGolemEntity::new, SpawnGroup.MONSTER).dimensions(1.8f, 3.3f).eyeHeight(2.9f).maxTrackingRange(10),
		0x3C404B, 0x46E6FF);
	public static final EntityType<ShadowWeaverEntity> SHADOW_WEAVER = register("shadow_weaver",
		EntityType.Builder.create(ShadowWeaverEntity::new, SpawnGroup.MONSTER).dimensions(1.2f, 2.8f).eyeHeight(2.4f).maxTrackingRange(10),
		0x231432, 0xD278FF);

	public static final EntityType<MagicBoltEntity> MAGIC_BOLT = Registry.register(Registries.ENTITY_TYPE, RogueTower.id("magic_bolt"),
		EntityType.Builder.<MagicBoltEntity>create(MagicBoltEntity::new, SpawnGroup.MISC).dimensions(0.4f, 0.4f)
			.maxTrackingRange(6).trackingTickInterval(2).build(RogueTower.id("magic_bolt").toString()));

	private ModEntities() {
	}

	private static <T extends MobEntity> EntityType<T> register(String name, EntityType.Builder<T> builder, int primary, int secondary) {
		EntityType<T> type = Registry.register(Registries.ENTITY_TYPE, RogueTower.id(name), builder.build(RogueTower.id(name).toString()));
		Item egg = Registry.register(Registries.ITEM, RogueTower.id(name + "_spawn_egg"),
			new SpawnEggItem(type, primary, secondary, new Item.Settings()));
		SPAWN_EGGS.add(egg);
		return type;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(GLUTTON, GluttonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BLADE_MANTIS, BladeMantisEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HOWLING_TOTEM, HowlingTotemEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CRAWLER, CrawlerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPORELING, SporelingEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(EMBER_GARGOYLE, EmberGargoyleEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_EYE, VoidEyeEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(RUNE_GOLEM, RuneGolemEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SHADOW_WEAVER, ShadowWeaverEntity.createAttributes());
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> SPAWN_EGGS.forEach(entries::add));
	}

	public static List<EntityType<? extends MobEntity>> all() {
		return List.of(GLUTTON, BLADE_MANTIS, HOWLING_TOTEM, CRAWLER, SPORELING, EMBER_GARGOYLE, VOID_EYE, RUNE_GOLEM, SHADOW_WEAVER);
	}
}
