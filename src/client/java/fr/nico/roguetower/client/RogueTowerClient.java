package fr.nico.roguetower.client;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.SelfTest;
import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.classes.Spell;
import fr.nico.roguetower.net.AbilityPayload;
import fr.nico.roguetower.client.render.ModRenderers;
import fr.nico.roguetower.net.ClassSyncPayload;
import fr.nico.roguetower.net.CombatAnimPayload;
import fr.nico.roguetower.net.CombatPayload;
import fr.nico.roguetower.net.CombatSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class RogueTowerClient implements ClientModInitializer {
	private static final String CATEGORY = "category.roguetower";
	private static KeyBinding abilityKey;
	private static KeyBinding spellKey;
	private static KeyBinding classKey;
	private static KeyBinding cameraKey;
	private static KeyBinding dodgeKey;
	private static KeyBinding parryKey;
	private static boolean checkedConflicts;
	private static int selfTestTicks;

	@Override
	public void onInitializeClient() {
		abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
		spellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.spell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY));
		classKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.class", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
		cameraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.camera", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
		dodgeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.dodge", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
		parryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.roguetower.parry", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

		ModRenderers.register();

		ClientPlayNetworking.registerGlobalReceiver(CombatSyncPayload.ID, (payload, context) -> {
			ClientData.stamina = payload.stamina();
			ClientData.combo = payload.combo();
			if (payload.event() != CombatSyncPayload.EVENT_NONE) {
				ClientData.lastEvent = payload.event();
				ClientData.lastEventTick = ClientData.clientTicks;
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(CombatAnimPayload.ID, (payload, context) -> {
			if (context.client().world != null) {
				Entity entity = context.client().world.getEntityById(payload.entityId());
				if (entity != null) {
					CombatAnimations.start(entity, payload);
				}
			}
		});

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
			if (SelfTest.enabled() && client.currentScreen != null && client.getOverlay() == null && ++selfTestTicks == 100) {
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
			while (dodgeKey.wasPressed()) {
				// direction voulue = touches de déplacement, exprimées dans le monde
				float f = client.player.input.movementForward;
				float sd = client.player.input.movementSideways;
				double yaw = Math.toRadians(client.player.getYaw());
				float wx = (float) (sd * Math.cos(yaw) - f * Math.sin(yaw));
				float wz = (float) (f * Math.cos(yaw) + sd * Math.sin(yaw));
				ClientPlayNetworking.send(new CombatPayload(CombatPayload.DODGE, wx, wz));
			}
			while (parryKey.wasPressed()) {
				ClientPlayNetworking.send(new CombatPayload(CombatPayload.PARRY, 0, 0));
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

		renderCombat(context, client, w, h);

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

	private static void renderCombat(DrawContext context, MinecraftClient client, int w, int h) {
		// Barre d'endurance (esquive / parade)
		int x = 6;
		int y = h - 58;
		int width = 82;
		context.fill(x - 1, y - 1, x + width + 1, y + 5, 0xAA000000);
		int filled = width * ClientData.stamina / 100;
		context.fill(x, y, x + filled, y + 4, ClientData.stamina >= 25 ? 0xFF4CD964 : 0xFFE0A030);
		context.drawTextWithShadow(client.textRenderer, Text.literal("[" + keyName(dodgeKey) + "] Esquive  [" + keyName(parryKey) + "] Parade")
			.formatted(Formatting.GRAY), x, y - 11, 0xFFFFFF);

		if (ClientData.combo >= 2) {
			Text combo = Text.literal("COMBO x" + ClientData.combo).formatted(ClientData.combo % 3 == 2 ? Formatting.GOLD : Formatting.YELLOW, Formatting.BOLD);
			context.drawCenteredTextWithShadow(client.textRenderer, combo, w / 2, h / 2 + 28, 0xFFFFFF);
		}
		long age = ClientData.clientTicks - ClientData.lastEventTick;
		if (age < 20) {
			String label = switch (ClientData.lastEvent) {
				case CombatSyncPayload.EVENT_PARRY -> "PARADE !";
				case CombatSyncPayload.EVENT_FINISHER -> "COUP FINAL !";
				case CombatSyncPayload.EVENT_BACKSTAB -> "DANS LE DOS !";
				case CombatSyncPayload.EVENT_RIPOSTE -> "RIPOSTE !";
				default -> "";
			};
			if (!label.isEmpty()) {
				context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(label).formatted(Formatting.GOLD, Formatting.BOLD),
					w / 2, h / 2 - 40, 0xFFFFFF);
			}
		}
	}

	private static String keyName(KeyBinding key) {
		return key.getBoundKeyLocalizedText().getString().toUpperCase();
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
