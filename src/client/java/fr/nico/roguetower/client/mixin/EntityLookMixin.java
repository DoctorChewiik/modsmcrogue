package fr.nico.roguetower.client.mixin;

import fr.nico.roguetower.client.TopDownCamera;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En vue du dessus, la souris déplace un curseur au lieu de tourner la tête. */
@Mixin(Entity.class)
public abstract class EntityLookMixin {
	@Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
	private void roguetower$moveCursor(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
		if ((Object) this instanceof ClientPlayerEntity && TopDownCamera.isActive()) {
			TopDownCamera.onMouseMove(cursorDeltaX, cursorDeltaY);
			ci.cancel();
		}
	}
}
