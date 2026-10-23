package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.RuneGolemEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class RuneGolemModel extends SinglePartEntityModel<RuneGolemEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("rune_golem"), "main");
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart armR;
	private final ModelPart armL;
	private final ModelPart legR;
	private final ModelPart legL;
	private final ModelPart core;

	public RuneGolemModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.armR = body.getChild("armR");
		this.armL = body.getChild("armL");
		this.legR = root.getChild("legR");
		this.legL = root.getChild("legL");
		this.core = body.getChild("core");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-9f, -14f, -6f, 18f, 14f, 12f),
			ModelTransform.of(0f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData head = body.addChild("head", ModelPartBuilder.create().uv(60, 0).cuboid(-4f, -7f, -4f, 8f, 7f, 8f),
			ModelTransform.of(0f, -14f, -2f, 0f, 0f, 0f));
		ModelPartData armR = body.addChild("armR", ModelPartBuilder.create().uv(64, 15).cuboid(-6f, -2f, -3f, 6f, 18f, 6f),
			ModelTransform.of(-9f, -12f, 0f, 0f, 0f, 0f));
		ModelPartData armL = body.addChild("armL", ModelPartBuilder.create().mirrored().uv(64, 15).cuboid(0f, -2f, -3f, 6f, 18f, 6f).mirrored(false),
			ModelTransform.of(9f, -12f, 0f, 0f, 0f, 0f));
		ModelPartData legR = root.addChild("legR", ModelPartBuilder.create().uv(0, 26).cuboid(-3f, 0f, -3f, 6f, 10f, 6f),
			ModelTransform.of(-5f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData legL = root.addChild("legL", ModelPartBuilder.create().mirrored().uv(0, 26).cuboid(-3f, 0f, -3f, 6f, 10f, 6f).mirrored(false),
			ModelTransform.of(5f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData core = body.addChild("core", ModelPartBuilder.create().uv(24, 26).cuboid(-2f, -2f, -1f, 4f, 4f, 1f),
			ModelTransform.of(0f, -8f, -6f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(RuneGolemEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		legR.pitch = walk * 0.6f;
		legL.pitch = -walk * 0.6f;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		float raise = entity.isWindingUp() ? action : 0f;
		float slam = entity.isStriking() ? action : 0f;
		armR.pitch = -walk * 0.6f - raise * 2.6f + slam * 2.6f;
		armL.pitch = walk * 0.6f - raise * 2.6f + slam * 2.6f;
		core.xScale = 1f + MathHelper.sin(t * 0.3f) * 0.15f + raise * 0.5f;
		core.yScale = core.xScale;
		body.pitch = slam * 0.3f;
	}
}
