package fr.nico.roguetower.client.mixin;

import fr.nico.roguetower.client.TopDownCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void setPos(double x, double y, double z);

	@Inject(method = "update", at = @At("TAIL"))
	private void roguetower$topDown(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
									float tickDelta, CallbackInfo ci) {
		if (focusedEntity == null || !TopDownCamera.isActive()) {
			return;
		}
		Vec3d pos = TopDownCamera.cameraPos(focusedEntity, tickDelta);
		if (focusedEntity instanceof ClientPlayerEntity player && MinecraftClient.getInstance().currentScreen == null) {
			TopDownCamera.aimPlayer(player, pos);
		}
		setRotation(TopDownCamera.CAMERA_YAW, TopDownCamera.CAMERA_PITCH);
		setPos(pos.x, pos.y, pos.z);
	}
}
