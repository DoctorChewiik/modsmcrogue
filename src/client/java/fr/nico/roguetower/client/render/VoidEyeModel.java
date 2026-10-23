package fr.nico.roguetower.client.render;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.VoidEyeEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle 100 % original : un œil géant avec quatre tentacules. */
public class VoidEyeModel extends SinglePartEntityModel<VoidEyeEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("void_eye"), "main");
	private final ModelPart root;
	private final ModelPart eye;
	private final ModelPart[] tentacles = new ModelPart[4];

	public VoidEyeModel(ModelPart root) {
		this.root = root;
		this.eye = root.getChild("eye");
		for (int i = 0; i < 4; i++) {
			tentacles[i] = root.getChild("tentacle" + i);
		}
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("eye", ModelPartBuilder.create().uv(0, 0).cuboid(-5f, -5f, -5f, 10f, 10f, 10f),
			ModelTransform.pivot(0f, 12f, 0f));
		float[][] pos = {{-3f, -3f}, {3f, -3f}, {-3f, 3f}, {3f, 3f}};
		for (int i = 0; i < 4; i++) {
			root.addChild("tentacle" + i, ModelPartBuilder.create().uv(40, 0).cuboid(-1f, 0f, -1f, 2f, 8f, 2f),
				ModelTransform.pivot(pos[i][0], 16f, pos[i][1]));
		}
		return TexturedModelData.of(data, 64, 32);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(VoidEyeEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		float bob = MathHelper.sin(animationProgress * 0.1f) * 1.2f;
		eye.pivotY = 12f + bob;
		eye.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		eye.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		// l'œil se contracte avant de se téléporter
		float size = entity.getCharge() > 0 ? 0.6f + 0.25f * MathHelper.sin(animationProgress * 1.5f) : 1f;
		eye.xScale = size;
		eye.yScale = size;
		eye.zScale = size;
		for (int i = 0; i < 4; i++) {
			tentacles[i].pivotY = 16f + bob;
			tentacles[i].pitch = MathHelper.sin(animationProgress * 0.2f + i * 1.7f) * 0.35f;
			tentacles[i].roll = MathHelper.cos(animationProgress * 0.17f + i) * 0.25f;
		}
	}
}
