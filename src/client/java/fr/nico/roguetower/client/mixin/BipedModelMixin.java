package fr.nico.roguetower.client.mixin;

import fr.nico.roguetower.client.CombatAnimations;
import fr.nico.roguetower.net.CombatAnimPayload;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Poses des mouvements de combat des joueurs. */
@Mixin(BipedEntityModel.class)
public abstract class BipedModelMixin<T extends LivingEntity> {
	@Shadow @Final public ModelPart body;
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart rightLeg;
	@Shadow @Final public ModelPart leftLeg;

	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void roguetower$combatPose(T entity, float limbAngle, float limbDistance, float animationProgress,
									   float headYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof PlayerEntity)) {
			return;
		}
		CombatAnimations.Anim anim = CombatAnimations.get(entity);
		if (anim == null) {
			return;
		}
		float t = CombatAnimations.progress(anim, animationProgress);
		float w = MathHelper.sin(t * MathHelper.PI); // entrée/sortie en douceur
		switch (anim.type()) {
			case CombatAnimPayload.DODGE -> {
				rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -1.1f);
				leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -1.1f);
				rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, -1.3f);
				leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, -1.3f);
			}
			case CombatAnimPayload.PARRY -> {
				float g = Math.min(1f, t * 4f) * (t > 0.8f ? (1f - t) * 5f : 1f);
				rightArm.pitch = MathHelper.lerp(g, rightArm.pitch, -1.5f);
				rightArm.yaw = MathHelper.lerp(g, rightArm.yaw, -0.6f);
				leftArm.pitch = MathHelper.lerp(g, leftArm.pitch, -1.5f);
				leftArm.yaw = MathHelper.lerp(g, leftArm.yaw, 0.6f);
			}
			case CombatAnimPayload.PARRY_SUCCESS -> {
				rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -1.0f);
				rightArm.roll = MathHelper.lerp(w, rightArm.roll, 1.3f);
				leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -1.0f);
				leftArm.roll = MathHelper.lerp(w, leftArm.roll, -1.3f);
			}
			case CombatAnimPayload.FINISHER -> {
				float swing = MathHelper.lerp(t, -3.0f, 0.5f);
				rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, swing);
				leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, swing + 0.3f);
				body.yaw = MathHelper.lerp(w, body.yaw, MathHelper.lerp(t, 0.5f, -0.5f));
			}
			case CombatAnimPayload.THRUST -> {
				rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -1.57f);
				rightArm.yaw = MathHelper.lerp(w, rightArm.yaw, 0f);
				body.yaw = MathHelper.lerp(w, body.yaw, -0.35f);
			}
			case CombatAnimPayload.CAST -> {
				rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -2.8f);
				leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -2.8f);
				rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.35f);
				leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.35f);
			}
			default -> {
			}
		}
	}
}
