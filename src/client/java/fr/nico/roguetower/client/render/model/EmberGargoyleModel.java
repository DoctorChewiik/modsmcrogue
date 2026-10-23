package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.EmberGargoyleEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class EmberGargoyleModel extends SinglePartEntityModel<EmberGargoyleEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("ember_gargoyle"), "main");
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart hornR;
	private final ModelPart hornL;
	private final ModelPart wingR;
	private final ModelPart wingL;
	private final ModelPart armR;
	private final ModelPart armL;
	private final ModelPart legR;
	private final ModelPart legL;
	private final ModelPart tail;

	public EmberGargoyleModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.hornR = head.getChild("hornR");
		this.hornL = head.getChild("hornL");
		this.wingR = body.getChild("wingR");
		this.wingL = body.getChild("wingL");
		this.armR = body.getChild("armR");
		this.armL = body.getChild("armL");
		this.legR = body.getChild("legR");
		this.legL = body.getChild("legL");
		this.tail = body.getChild("tail");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-3f, -4f, -2f, 6f, 8f, 4f),
			ModelTransform.of(0f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData head = body.addChild("head", ModelPartBuilder.create().uv(20, 0).cuboid(-2.5f, -5f, -3f, 5f, 5f, 5f),
			ModelTransform.of(0f, -4f, -0.5f, 0f, 0f, 0f));
		ModelPartData hornR = head.addChild("hornR", ModelPartBuilder.create().uv(40, 0).cuboid(-0.5f, -3f, -0.5f, 1f, 3f, 1f),
			ModelTransform.of(-2f, -5f, 0f, 0f, 0f, -0.3f));
		ModelPartData hornL = head.addChild("hornL", ModelPartBuilder.create().mirrored().uv(40, 0).cuboid(-0.5f, -3f, -0.5f, 1f, 3f, 1f).mirrored(false),
			ModelTransform.of(2f, -5f, 0f, 0f, 0f, 0.3f));
		ModelPartData wingR = body.addChild("wingR", ModelPartBuilder.create().uv(0, 12).cuboid(-10f, -1f, 0f, 10f, 8f, 1f),
			ModelTransform.of(-3f, -3f, 1.5f, 0f, 0f, 0f));
		ModelPartData wingL = body.addChild("wingL", ModelPartBuilder.create().mirrored().uv(0, 12).cuboid(0f, -1f, 0f, 10f, 8f, 1f).mirrored(false),
			ModelTransform.of(3f, -3f, 1.5f, 0f, 0f, 0f));
		ModelPartData armR = body.addChild("armR", ModelPartBuilder.create().uv(44, 0).cuboid(-1f, 0f, -1f, 2f, 5f, 2f),
			ModelTransform.of(-3.5f, -3f, 0f, 0f, 0f, 0f));
		ModelPartData armL = body.addChild("armL", ModelPartBuilder.create().mirrored().uv(44, 0).cuboid(-1f, 0f, -1f, 2f, 5f, 2f).mirrored(false),
			ModelTransform.of(3.5f, -3f, 0f, 0f, 0f, 0f));
		ModelPartData legR = body.addChild("legR", ModelPartBuilder.create().uv(44, 7).cuboid(-1f, 0f, -1f, 2f, 5f, 2f),
			ModelTransform.of(-1.5f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData legL = body.addChild("legL", ModelPartBuilder.create().mirrored().uv(44, 7).cuboid(-1f, 0f, -1f, 2f, 5f, 2f).mirrored(false),
			ModelTransform.of(1.5f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData tail = body.addChild("tail", ModelPartBuilder.create().uv(22, 12).cuboid(-0.5f, 0f, 0f, 1f, 1f, 6f),
			ModelTransform.of(0f, 3f, 2f, 0.5f, 0f, 0f));
		return TexturedModelData.of(data, 64, 32);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(EmberGargoyleEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		float flap = MathHelper.sin(t * 0.6f) * 0.6f;
		float dive = entity.isStriking() ? action : 0f;
		float charge = entity.isWindingUp() ? action : 0f;
		wingR.yaw = 0.35f + flap * (1f - dive) - charge * 0.5f;
		wingL.yaw = -0.35f - flap * (1f - dive) + charge * 0.5f;
		body.pitch = 0.2f + dive * 0.9f;
		body.pivotY += MathHelper.sin(t * 0.1f) * 1.0f;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		armR.pitch = -0.4f - dive * 1.2f; armL.pitch = -0.4f - dive * 1.2f;
		legR.pitch = 0.4f; legL.pitch = 0.4f;
		tail.yaw = MathHelper.sin(t * 0.15f) * 0.4f;
	}
}
