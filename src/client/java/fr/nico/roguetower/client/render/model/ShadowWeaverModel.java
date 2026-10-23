package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.ShadowWeaverEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class ShadowWeaverModel extends SinglePartEntityModel<ShadowWeaverEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("shadow_weaver"), "main");
	private final ModelPart root;
	private final ModelPart robe;
	private final ModelPart hood;
	private final ModelPart crown;
	private final ModelPart hand0;
	private final ModelPart hand1;
	private final ModelPart hand2;
	private final ModelPart hand3;

	public ShadowWeaverModel(ModelPart root) {
		this.root = root;
		this.robe = root.getChild("robe");
		this.hood = robe.getChild("hood");
		this.crown = hood.getChild("crown");
		this.hand0 = root.getChild("hand0");
		this.hand1 = root.getChild("hand1");
		this.hand2 = root.getChild("hand2");
		this.hand3 = root.getChild("hand3");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData robe = root.addChild("robe", ModelPartBuilder.create().uv(0, 0).cuboid(-5f, -14f, -4f, 10f, 14f, 8f),
			ModelTransform.of(0f, 18f, 0f, 0f, 0f, 0f));
		ModelPartData hood = robe.addChild("hood", ModelPartBuilder.create().uv(36, 0).cuboid(-5f, -10f, -5f, 10f, 10f, 10f),
			ModelTransform.of(0f, -14f, 0f, 0f, 0f, 0f));
		ModelPartData crown = hood.addChild("crown", ModelPartBuilder.create().uv(76, 0).cuboid(-4f, -3f, -4f, 8f, 3f, 8f),
			ModelTransform.of(0f, -10f, 0f, 0f, 0f, 0f));
		ModelPartData hand0 = root.addChild("hand0", ModelPartBuilder.create().uv(0, 22).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f),
			ModelTransform.of(-9f, 6f, -2f, 0f, 0f, 0f));
		ModelPartData hand1 = root.addChild("hand1", ModelPartBuilder.create().uv(12, 22).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f),
			ModelTransform.of(9f, 6f, -2f, 0f, 0f, 0f));
		ModelPartData hand2 = root.addChild("hand2", ModelPartBuilder.create().uv(24, 22).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f),
			ModelTransform.of(-8f, 12f, 3f, 0f, 0f, 0f));
		ModelPartData hand3 = root.addChild("hand3", ModelPartBuilder.create().uv(36, 22).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f),
			ModelTransform.of(8f, 12f, 3f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(ShadowWeaverEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		float bob = MathHelper.sin(t * 0.08f) * 1.5f;
		robe.pivotY += bob;
		hood.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		float gather = entity.isWindingUp() ? action : 0f;
		ModelPart[] hs = {hand0, hand1, hand2, hand3};
		for (int i = 0; i < 4; i++) {
		float a = t * 0.05f + i * MathHelper.HALF_PI;
		float r = 9f * (1f - gather * 0.6f);
		hs[i].pivotX = MathHelper.cos(a) * r;
		hs[i].pivotZ = MathHelper.sin(a) * r;
		hs[i].pivotY = 6f + (i % 2) * 6f + bob + MathHelper.sin(t * 0.2f + i) * 1.5f - gather * 6f;
		hs[i].yaw = -a;
		}
	}
}
