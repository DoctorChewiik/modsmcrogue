package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.VoidEyeEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class VoidEyeModel extends SinglePartEntityModel<VoidEyeEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("void_eye"), "main");
	private final ModelPart root;
	private final ModelPart eye;
	private final ModelPart lid;
	private final ModelPart t0;
	private final ModelPart t1;
	private final ModelPart t2;
	private final ModelPart t3;

	public VoidEyeModel(ModelPart root) {
		this.root = root;
		this.eye = root.getChild("eye");
		this.lid = eye.getChild("lid");
		this.t0 = root.getChild("t0");
		this.t1 = root.getChild("t1");
		this.t2 = root.getChild("t2");
		this.t3 = root.getChild("t3");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData eye = root.addChild("eye", ModelPartBuilder.create().uv(0, 0).cuboid(-5f, -5f, -5f, 10f, 10f, 10f),
			ModelTransform.of(0f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData lid = eye.addChild("lid", ModelPartBuilder.create().uv(0, 33).cuboid(-5.5f, -5.5f, -5.5f, 11f, 2f, 11f),
			ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData t0 = root.addChild("t0", ModelPartBuilder.create().uv(40, 0).cuboid(-1f, 0f, -1f, 2f, 8f, 2f),
			ModelTransform.of(-3f, 16f, -3f, 0f, 0f, 0f));
		ModelPartData t1 = root.addChild("t1", ModelPartBuilder.create().uv(48, 0).cuboid(-1f, 0f, -1f, 2f, 8f, 2f),
			ModelTransform.of(3f, 16f, -3f, 0f, 0f, 0f));
		ModelPartData t2 = root.addChild("t2", ModelPartBuilder.create().uv(56, 0).cuboid(-1f, 0f, -1f, 2f, 8f, 2f),
			ModelTransform.of(-3f, 16f, 3f, 0f, 0f, 0f));
		ModelPartData t3 = root.addChild("t3", ModelPartBuilder.create().uv(40, 23).cuboid(-1f, 0f, -1f, 2f, 7f, 2f),
			ModelTransform.of(3f, 16f, 3f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(VoidEyeEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		float bob = MathHelper.sin(t * 0.1f) * 1.2f;
		eye.pivotY += bob;
		eye.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		eye.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		float charge = entity.isWindingUp() ? action : 0f;
		float size = 1f - charge * 0.35f + MathHelper.sin(t * 1.5f) * 0.05f * charge;
		eye.xScale = size; eye.yScale = size; eye.zScale = size;
		lid.pivotY = -charge * 0.5f;
		ModelPart[] ts = {t0, t1, t2, t3};
		for (int i = 0; i < 4; i++) {
		ts[i].pivotY += bob;
		ts[i].pitch = MathHelper.sin(t * 0.2f + i * 1.7f) * 0.35f;
		ts[i].roll = MathHelper.cos(t * 0.17f + i) * 0.25f;
		}
	}
}
