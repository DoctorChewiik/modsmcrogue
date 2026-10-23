package fr.nico.roguetower.loot;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/** Effets spéciaux des armes magiques (épiques) et légendaires. */
public final class WeaponEffects {
	private WeaponEffects() {
	}

	public static void onHit(ServerPlayerEntity player, LivingEntity target) {
		ItemStack stack = player.getMainHandStack();
		NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null || player.getAttackCooldownProgress(0.5f) < 0.8f) {
			return;
		}
		NbtCompound nbt = data.copyNbt();
		String effect = nbt.getString("rt_effect");
		int power = Math.max(1, nbt.getInt("rt_power"));
		switch (effect) {
			case "fire" -> target.setOnFireFor(3 * power);
			case "frost" -> {
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, power));
				target.setFrozenTicks(140 + 40 * power);
			}
			case "poison" -> target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, power - 1));
			case "wither" -> target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, power - 1));
			case "lifesteal" -> player.heal(1.5f * power);
			case "lightning" -> {
				if (player.getRandom().nextFloat() < 0.15f * power) {
					LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(player.getWorld());
					if (bolt != null) {
						bolt.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
						bolt.setCosmetic(true);
						player.getWorld().spawnEntity(bolt);
					}
					target.damage(player.getDamageSources().indirectMagic(player, player), 4f + 2f * power);
				}
			}
			default -> {
			}
		}
	}
}
