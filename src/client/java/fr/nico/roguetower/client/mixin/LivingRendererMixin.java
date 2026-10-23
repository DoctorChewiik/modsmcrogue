package fr.nico.roguetower.client.mixin;

import fr.nico.roguetower.client.CombatAnimations;
import fr.nico.roguetower.net.CombatAnimPayload;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Roulade d'esquive : le modèle du joueur fait un tour complet dans la direction de l'esquive. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin {
	@Inject(method = "setupTransforms", at = @At("TAIL"))
	private void roguetower$dodgeRoll(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw,
									  float tickDelta, float scale, CallbackInfo ci) {
		if (!(entity instanceof PlayerEntity)) {
			return;
		}
		CombatAnimations.Anim anim = CombatAnimations.get(entity);
		if (anim == null || anim.type() != CombatAnimPayload.DODGE) {
			return;
		}
		float t = CombatAnimations.progress(anim, animationProgress);
		float angle = t * MathHelper.TAU;
		// direction de l'esquive exprimée dans le repère du corps
		double yaw = Math.toRadians(bodyYaw);
		float forward = (float) (anim.dirX() * -Math.sin(yaw) + anim.dirZ() * Math.cos(yaw));
		float right = (float) (anim.dirX() * -Math.cos(yaw) + anim.dirZ() * -Math.sin(yaw));
		float len = MathHelper.sqrt(forward * forward + right * right);
		if (len < 1e-3f) {
			forward = -1f;
			right = 0f;
			len = 1f;
		}
		float half = entity.getHeight() * 0.5f;
		matrices.translate(0f, half, 0f);
		matrices.multiply(new Quaternionf().rotateAxis(angle, -forward / len, 0f, right / len));
		matrices.translate(0f, -half, 0f);
	}
}
