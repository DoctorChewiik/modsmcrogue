package fr.nico.roguetower.client;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.SelfTest;
import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.classes.Spell;
import fr.nico.roguetower.net.AbilityPayload;
import fr.nico.roguetower.net.ClassSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class RogueTowerClient implements ClientModInitializer {
	private static final String CATEGORY = "category.roguetower";
	private static KeyBinding abilityKey;
	private static KeyBinding spellKey;
	private static KeyBinding classKey;
	private static KeyBinding cameraKey;
	private static boolean checkedConflicts;
	private static int selfTestTicks;

	@Override
	public void onInitializeClient() {
		abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
		spellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.spell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY));
		classKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.class", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
		cameraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.camera", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

		BlockRenderLayerMap.INSTANCE.putBlock(RogueTower.TOWER_PORTAL, RenderLayer.getTranslucent());
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> 0xFFB43C, RogueTower.TOWER_PORTAL);

		ClientPlayNetworking.registerGlobalReceiver(ClassSyncPayload.ID, (payload, context) -> {
			ClientData.rogueClass = RogueClass.byId(payload.classId());
			ClientData.spell = Spell.byId(payload.spellId());
			ClientData.abilityReady = ClientData.clientTicks + payload.abilityCooldown();
			ClientData.spellReady = ClientData.clientTicks + payload.spellCooldown();
			if (payload.openScreen()) {
				context.client().setScreen(new ClassSelectScreen());
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientData.clientTicks++;
			if (SelfTest.enabled() && client.currentScreen instanceof TitleScreen && ++selfTestTicks == 100) {
				RogueTower.LOGGER.info("ROGUETOWER CLIENT SELFTEST OK");
				client.scheduleStop();
			}
			if (client.player == null) {
				return;
			}
			if (!checkedConflicts) {
				checkedConflicts = true;
				resolveSwapHandsConflict(client);
			}
			TopDownCamera.tick(client);
			while (abilityKey.wasPressed()) {
				ClientPlayNetworking.send(new AbilityPayload(0));
			}
			while (spellKey.wasPressed()) {
				ClientPlayNetworking.send(new AbilityPayload(1));
			}
			while (classKey.wasPressed()) {
				client.setScreen(new ClassSelectScreen());
			}
			while (cameraKey.wasPressed()) {
				TopDownCamera.cycleMode();
			}
		});

		HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
	}

	/** La touche F sert au sort : on déplace « échanger les mains » sur G si besoin. */
	private static void resolveSwapHandsConflict(MinecraftClient client) {
		KeyBinding swap = client.options.swapHandsKey;
		if (swap.getBoundKeyTranslationKey().equals(spellKey.getBoundKeyTranslationKey())) {
			client.options.setKeyCode(swap, InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_G));
			KeyBinding.updateKeysByCode();
			client.options.write();
			client.player.sendMessage(Text.literal("Rogue Tower : la touche F lance ton sort. « Échanger les mains » est maintenant sur G.")
				.formatted(Formatting.YELLOW), false);
		}
	}

	private static void renderHud(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}
		int w = context.getScaledWindowWidth();
		int h = context.getScaledWindowHeight();

		if (TopDownCamera.isActive() && client.currentScreen == null) {
			int cx = (int) ((TopDownCamera.cursorX + 1) / 2 * w);
			int cy = (int) ((1 - TopDownCamera.cursorY) / 2 * h);
			int color = 0xFFFFD040;
			context.fill(cx - 5, cy, cx - 1, cy + 1, color);
			context.fill(cx + 2, cy, cx + 6, cy + 1, color);
			context.fill(cx, cy - 5, cx + 1, cy - 1, color);
			context.fill(cx, cy + 2, cx + 1, cy + 6, color);
			context.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
		}

		RogueClass cls = ClientData.rogueClass;
		int x = 6;
		int y = h - 46;
		if (cls == RogueClass.NONE) {
			context.drawTextWithShadow(client.textRenderer, Text.literal("Appuie sur [" + classKey.getBoundKeyLocalizedText().getString()
				+ "] pour choisir ta classe").formatted(Formatting.YELLOW), x, y + 20, 0xFFFFFF);
			return;
		}
		context.drawTextWithShadow(client.textRenderer, Text.literal(cls.displayName).formatted(Formatting.GOLD), x, y, 0xFFFFFF);
		drawSlot(context, client, x, y + 11, abilityKey, cls.ability, ClientData.abilityReady);
		drawSlot(context, client, x, y + 22, spellKey, ClientData.spell, ClientData.spellReady);
	}

	private static void drawSlot(DrawContext context, MinecraftClient client, int x, int y, KeyBinding key, Spell spell, long readyAt) {
		long remaining = readyAt - ClientData.clientTicks;
		String keyName = key.getBoundKeyLocalizedText().getString().toUpperCase();
		Text status = remaining > 0
			? Text.literal(" " + (remaining / 20 + 1) + "s").formatted(Formatting.RED)
			: Text.literal(" prêt").formatted(Formatting.GREEN);
		context.drawTextWithShadow(client.textRenderer, Text.literal("[" + keyName + "] ").formatted(Formatting.GRAY)
			.append(Text.literal(spell.displayName).formatted(spell.rarity.color)).append(status), x, y, 0xFFFFFF);
	}
}
