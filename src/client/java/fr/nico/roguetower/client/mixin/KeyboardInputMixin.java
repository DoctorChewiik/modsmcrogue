package fr.nico.roguetower.client.mixin;

import fr.nico.roguetower.client.TopDownCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En vue du dessus, ZQSD/WASD déplacent par rapport à l'écran (haut = nord). */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void roguetower$screenRelative(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null || !TopDownCamera.isActive()) {
			return;
		}
		Input input = (Input) (Object) this;
		double f = input.movementForward;
		double s = input.movementSideways;
		double c = Math.toRadians(TopDownCamera.CAMERA_YAW);
		double wx = s * Math.cos(c) - f * Math.sin(c);
		double wz = f * Math.cos(c) + s * Math.sin(c);
		double y = Math.toRadians(player.getYaw());
		input.movementForward = (float) (-wx * Math.sin(y) + wz * Math.cos(y));
		input.movementSideways = (float) (wx * Math.cos(y) + wz * Math.sin(y));
	}
}
