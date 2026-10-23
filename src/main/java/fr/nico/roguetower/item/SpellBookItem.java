package fr.nico.roguetower.item;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.classes.ClassManager;
import fr.nico.roguetower.classes.Spell;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Livre de sort : clic droit pour apprendre le sort, qui remplace le sort de la touche F. */
public class SpellBookItem extends Item {
	public SpellBookItem(Settings settings) {
		super(settings);
	}

	public static ItemStack create(Spell spell) {
		ItemStack stack = new ItemStack(RogueTower.SPELL_BOOK);
		NbtCompound nbt = new NbtCompound();
		nbt.putString("rt_spell", spell.name());
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
		stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Livre de sort : " + spell.displayName)
			.styled(s -> s.withItalic(false).withColor(spell.rarity.color)));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			Text.literal("★ " + spell.rarity.displayName).styled(s -> s.withItalic(false).withColor(spell.rarity.color)),
			Text.literal(spell.description).styled(s -> s.withItalic(false).withColor(Formatting.GRAY)),
			Text.literal("Recharge : " + spell.cooldown / 20 + " s").styled(s -> s.withItalic(false).withColor(Formatting.DARK_GRAY)),
			Text.literal("Clic droit : apprendre (remplace ton sort [F])").styled(s -> s.withItalic(true).withColor(Formatting.YELLOW))
		)));
		stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	public static Spell spellOf(ItemStack stack) {
		NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) {
			return Spell.NONE;
		}
		return Spell.byName(data.copyNbt().getString("rt_spell"));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		Spell spell = spellOf(stack);
		if (spell == Spell.NONE) {
			return TypedActionResult.pass(stack);
		}
		if (!world.isClient && user instanceof ServerPlayerEntity player) {
			ClassManager.learnSpell(player, spell);
			if (!player.getAbilities().creativeMode) {
				stack.decrement(1);
			}
		}
		return TypedActionResult.success(stack, world.isClient);
	}
}
