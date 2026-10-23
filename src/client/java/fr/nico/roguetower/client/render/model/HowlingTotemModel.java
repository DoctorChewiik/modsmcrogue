package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.HowlingTotemEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class HowlingTotemModel extends SinglePartEntityModel<HowlingTotemEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("howling_totem"), "main");
	private final ModelPart root;
	private final ModelPart base;
	private final ModelPart middle;
	private final ModelPart top;
	private final ModelPart wingR;
	private final ModelPart wingL;

	public HowlingTotemModel(ModelPart root) {
		this.root = root;
		this.base = root.getChild("base");
		this.middle = root.getChild("middle");
		this.top = root.getChild("top");
		this.wingR = top.getChild("wingR");
		this.wingL = top.getChild("wingL");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData base = root.addChild("base", ModelPartBuilder.create().uv(0, 0).cuboid(-5f, -8f, -5f, 10f, 8f, 10f),
			ModelTransform.of(0f, 24f, 0f, 0f, 0f, 0f));
		ModelPartData middle = root.addChild("middle", ModelPartBuilder.create().uv(0, 18).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
			ModelTransform.of(0f, 16f, 0f, 0f, 0f, 0f));
		ModelPartData top = root.addChild("top", ModelPartBuilder.create().uv(0, 34).cuboid(-4.5f, -9f, -4.5f, 9f, 9f, 9f),
			ModelTransform.of(0f, 8f, 0f, 0f, 0f, 0f));
		ModelPartData wingR = top.addChild("wingR", ModelPartBuilder.create().uv(40, 0).cuboid(-7f, -3f, -0.5f, 7f, 6f, 1f),
			ModelTransform.of(-4.5f, -6f, 0f, 0f, 0f, 0f));
		ModelPartData wingL = top.addChild("wingL", ModelPartBuilder.create().mirrored().uv(40, 0).cuboid(0f, -3f, -0.5f, 7f, 6f, 1f).mirrored(false),
			ModelTransform.of(4.5f, -6f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(HowlingTotemEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		float charge = entity.isWindingUp() ? action : 0f;
		top.pivotY -= charge * 3f;
		top.yaw = MathHelper.sin(t * 0.05f) * 0.15f + MathHelper.sin(t * 1.3f) * 0.12f * charge;
		middle.yaw = -MathHelper.sin(t * 0.05f) * 0.1f;
		wingR.roll = -0.2f - MathHelper.sin(t * 0.2f) * 0.1f - charge * 0.6f;
		wingL.roll = 0.2f + MathHelper.sin(t * 0.2f) * 0.1f + charge * 0.6f;
	}
}
