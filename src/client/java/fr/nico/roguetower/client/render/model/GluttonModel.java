package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.GluttonEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class GluttonModel extends SinglePartEntityModel<GluttonEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("glutton"), "main");
	private final ModelPart root;
	private final ModelPart jaw;
	private final ModelPart upper;
	private final ModelPart teeth;
	private final ModelPart eyeStalkR;
	private final ModelPart eyeR;
	private final ModelPart eyeStalkL;
	private final ModelPart eyeL;

	public GluttonModel(ModelPart root) {
		this.root = root;
		this.jaw = root.getChild("jaw");
		this.upper = root.getChild("upper");
		this.teeth = upper.getChild("teeth");
		this.eyeStalkR = upper.getChild("eyeStalkR");
		this.eyeR = eyeStalkR.getChild("eyeR");
		this.eyeStalkL = upper.getChild("eyeStalkL");
		this.eyeL = eyeStalkL.getChild("eyeL");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData jaw = root.addChild("jaw", ModelPartBuilder.create().uv(0, 19).cuboid(-6f, -5f, -6f, 12f, 5f, 12f),
			ModelTransform.of(0f, 24f, 0f, 0f, 0f, 0f));
		ModelPartData upper = root.addChild("upper", ModelPartBuilder.create().uv(0, 0).cuboid(-6f, -7f, -12f, 12f, 7f, 12f),
			ModelTransform.of(0f, 19f, 6f, 0f, 0f, 0f));
		ModelPartData teeth = upper.addChild("teeth", ModelPartBuilder.create().uv(0, 36).cuboid(-5.5f, 0f, -11.5f, 11f, 1f, 1f),
			ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData eyeStalkR = upper.addChild("eyeStalkR", ModelPartBuilder.create().uv(48, 2).cuboid(-1f, -4f, -1f, 2f, 4f, 2f),
			ModelTransform.of(-3f, -7f, -7f, 0f, 0f, 0f));
		ModelPartData eyeR = eyeStalkR.addChild("eyeR", ModelPartBuilder.create().uv(48, 8).cuboid(-1.5f, -3f, -1.5f, 3f, 3f, 3f),
			ModelTransform.of(0f, -4f, 0f, 0f, 0f, 0f));
		ModelPartData eyeStalkL = upper.addChild("eyeStalkL", ModelPartBuilder.create().mirrored().uv(48, 2).cuboid(-1f, -4f, -1f, 2f, 4f, 2f).mirrored(false),
			ModelTransform.of(3f, -7f, -7f, 0f, 0f, 0f));
		ModelPartData eyeL = eyeStalkL.addChild("eyeL", ModelPartBuilder.create().mirrored().uv(48, 8).cuboid(-1.5f, -3f, -1.5f, 3f, 3f, 3f).mirrored(false),
			ModelTransform.of(0f, -4f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(GluttonEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		float hop = Math.abs(MathHelper.sin(limbAngle * 0.5f)) * limbDistance * 3f;
		jaw.pivotY -= hop;
		upper.pivotY -= hop;
		upper.pitch = -0.15f - Math.abs(MathHelper.sin(t * 0.08f)) * 0.15f - action * 0.9f;
		eyeStalkR.roll = MathHelper.sin(t * 0.13f) * 0.3f - 0.2f;
		eyeStalkL.roll = -MathHelper.sin(t * 0.11f) * 0.3f + 0.2f;
		eyeStalkR.pitch = headPitch * 0.01f;
		upper.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.3f;
	}
}
