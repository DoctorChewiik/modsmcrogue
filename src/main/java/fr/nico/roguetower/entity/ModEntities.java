package fr.nico.roguetower.entity;

import fr.nico.roguetower.RogueTower;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
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

/** Les monstres inédits de la tour. */
public final class ModEntities {
	private static final List<Item> SPAWN_EGGS = new ArrayList<>();

	public static final EntityType<BruteEntity> BRUTE = register("brute",
		EntityType.Builder.create(BruteEntity::new, SpawnGroup.MONSTER).dimensions(0.95f, 3.0f).eyeHeight(2.7f).maxTrackingRange(10),
		0x4A2A22, 0xC0392B);
	public static final EntityType<CrystalSpiderEntity> CRYSTAL_SPIDER = register("crystal_spider",
		EntityType.Builder.create(CrystalSpiderEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 0.5f).eyeHeight(0.35f).maxTrackingRange(8),
		0x5BC0EB, 0xE0F7FF);
	public static final EntityType<EmberSkeletonEntity> EMBER_SKELETON = register("ember_skeleton",
		EntityType.Builder.create(EmberSkeletonEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.99f).eyeHeight(1.74f).makeFireImmune().maxTrackingRange(8),
		0x2B2B2B, 0xFF7A00);
	public static final EntityType<VoidEyeEntity> VOID_EYE = register("void_eye",
		EntityType.Builder.create(VoidEyeEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 0.9f).eyeHeight(0.45f).makeFireImmune().maxTrackingRange(10),
		0x2E0F4F, 0xF2F2F2);
	public static final EntityType<MycobombEntity> MYCOBOMB = register("mycobomb",
		EntityType.Builder.create(MycobombEntity::new, SpawnGroup.MONSTER).dimensions(0.45f, 1.2f).eyeHeight(1.0f).maxTrackingRange(8),
		0xC0392B, 0xF5F5DC);
	public static final EntityType<BoarEntity> BOAR = register("boar",
		EntityType.Builder.create(BoarEntity::new, SpawnGroup.MONSTER).dimensions(1.3965f, 1.4f).eyeHeight(1.0f).maxTrackingRange(8),
		0x5C3D2E, 0xD9C7A3);
	public static final EntityType<SpectralWolfEntity> SPECTRAL_WOLF = register("spectral_wolf",
		EntityType.Builder.create(SpectralWolfEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 0.85f).eyeHeight(0.68f).maxTrackingRange(10),
		0x9AD7FF, 0x1B3A5C);
	public static final EntityType<LichEntity> LICH = register("lich",
		EntityType.Builder.create(LichEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f).eyeHeight(1.62f).maxTrackingRange(10),
		0x1E1E2E, 0x7CFC00);
	public static final EntityType<StoneSentinelEntity> STONE_SENTINEL = register("stone_sentinel",
		EntityType.Builder.create(StoneSentinelEntity::new, SpawnGroup.MONSTER).dimensions(1.4f, 2.7f).eyeHeight(2.3f).maxTrackingRange(10),
		0x5E6B4E, 0x2F3A2A);

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
		FabricDefaultAttributeRegistry.register(BRUTE, BruteEntity.createBruteAttributes());
		FabricDefaultAttributeRegistry.register(CRYSTAL_SPIDER, CrystalSpiderEntity.createCrystalSpiderAttributes());
		FabricDefaultAttributeRegistry.register(EMBER_SKELETON, EmberSkeletonEntity.createEmberSkeletonAttributes());
		FabricDefaultAttributeRegistry.register(VOID_EYE, VoidEyeEntity.createVoidEyeAttributes());
		FabricDefaultAttributeRegistry.register(MYCOBOMB, MycobombEntity.createMycobombAttributes());
		FabricDefaultAttributeRegistry.register(BOAR, BoarEntity.createBoarAttributes());
		FabricDefaultAttributeRegistry.register(SPECTRAL_WOLF, SpectralWolfEntity.createSpectralWolfAttributes());
		FabricDefaultAttributeRegistry.register(LICH, LichEntity.createLichAttributes());
		FabricDefaultAttributeRegistry.register(STONE_SENTINEL, StoneSentinelEntity.createSentinelAttributes());
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> SPAWN_EGGS.forEach(entries::add));
	}

	public static List<EntityType<? extends Entity>> all() {
		return List.of(BRUTE, CRYSTAL_SPIDER, EMBER_SKELETON, VOID_EYE, MYCOBOMB, BOAR, SPECTRAL_WOLF, LICH, STONE_SENTINEL);
	}
}
