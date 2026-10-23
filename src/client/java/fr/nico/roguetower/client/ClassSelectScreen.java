package fr.nico.roguetower.client;

import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.net.SelectClassPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClassSelectScreen extends Screen {
	public ClassSelectScreen() {
		super(Text.literal("Crée ton personnage : choisis ta classe").formatted(Formatting.GOLD));
	}

	@Override
	protected void init() {
		RogueClass[] classes = RogueClass.values();
		int count = classes.length - 1;
		int buttonWidth = 200;
		int y = height / 2 - count * 13;
		for (RogueClass cls : classes) {
			if (cls == RogueClass.NONE) {
				continue;
			}
			Text tooltip = Text.literal(cls.description + "\n\n[R] " + cls.ability.displayName + " : " + cls.ability.description
				+ "\n[F] " + cls.startingSpell.displayName + " : " + cls.startingSpell.description
				+ "\n\nLe sort [F] peut être remplacé par un livre de sort.");
			Text label = cls == ClientData.rogueClass
				? Text.literal(cls.displayName + " (actuelle)").formatted(Formatting.GREEN)
				: Text.literal(cls.displayName);
			addDrawableChild(ButtonWidget.builder(label, b -> {
					ClientPlayNetworking.send(new SelectClassPayload(cls.ordinal()));
					close();
				})
				.dimensions(width / 2 - buttonWidth / 2, y, buttonWidth, 20)
				.tooltip(Tooltip.of(tooltip))
				.build());
			y += 26;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - (RogueClass.values().length - 1) * 13 - 30, 0xFFFFFF);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal("Survole une classe pour voir ses pouvoirs").formatted(Formatting.GRAY),
			width / 2, height / 2 - (RogueClass.values().length - 1) * 13 - 16, 0xFFFFFF);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
