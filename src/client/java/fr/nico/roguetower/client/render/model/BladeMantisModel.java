package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.BladeMantisEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class BladeMantisModel extends SinglePartEntityModel<BladeMantisEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("blade_mantis"), "main");
	private final ModelPart root;
	private final ModelPart abdomen;
	private final ModelPart thorax;
	private final ModelPart head;
	private final ModelPart armR;
	private final ModelPart bladeR;
	private final ModelPart armL;
	private final ModelPart bladeL;
	private final ModelPart legFR;
	private final ModelPart legFL;
	private final ModelPart legBR;
	private final ModelPart legBL;

	public BladeMantisModel(ModelPart root) {
		this.root = root;
		this.abdomen = root.getChild("abdomen");
		this.thorax = root.getChild("thorax");
		this.head = thorax.getChild("head");
		this.armR = thorax.getChild("armR");
		this.bladeR = armR.getChild("bladeR");
		this.armL = thorax.getChild("armL");
		this.bladeL = armL.getChild("bladeL");
		this.legFR = root.getChild("legFR");
		this.legFL = root.getChild("legFL");
		this.legBR = root.getChild("legBR");
		this.legBL = root.getChild("legBL");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData abdomen = root.addChild("abdomen", ModelPartBuilder.create().uv(0, 0).cuboid(-2.5f, -2f, 0f, 5f, 4f, 9f),
			ModelTransform.of(0f, 13f, 2f, -0.25f, 0f, 0f));
		ModelPartData thorax = root.addChild("thorax", ModelPartBuilder.create().uv(0, 13).cuboid(-1.5f, -10f, -1.5f, 3f, 10f, 3f),
			ModelTransform.of(0f, 13f, 0f, 0f, 0f, 0f));
		ModelPartData head = thorax.addChild("head", ModelPartBuilder.create().uv(28, 0).cuboid(-2.5f, -4f, -2.5f, 5f, 4f, 4f),
			ModelTransform.of(0f, -10f, 0f, 0f, 0f, 0f));
		ModelPartData armR = thorax.addChild("armR", ModelPartBuilder.create().uv(12, 13).cuboid(-1f, 0f, -1f, 2f, 7f, 2f),
			ModelTransform.of(-1.5f, -8f, -1f, 0f, 0f, 0f));
		ModelPartData bladeR = armR.addChild("bladeR", ModelPartBuilder.create().uv(28, 8).cuboid(-0.5f, -1f, -9f, 1f, 2f, 9f),
			ModelTransform.of(0f, 7f, 0f, 0f, 0f, 0f));
		ModelPartData armL = thorax.addChild("armL", ModelPartBuilder.create().mirrored().uv(12, 13).cuboid(-1f, 0f, -1f, 2f, 7f, 2f).mirrored(false),
			ModelTransform.of(1.5f, -8f, -1f, 0f, 0f, 0f));
		ModelPartData bladeL = armL.addChild("bladeL", ModelPartBuilder.create().mirrored().uv(28, 8).cuboid(-0.5f, -1f, -9f, 1f, 2f, 9f).mirrored(false),
			ModelTransform.of(0f, 7f, 0f, 0f, 0f, 0f));
		ModelPartData legFR = root.addChild("legFR", ModelPartBuilder.create().uv(48, 0).cuboid(-0.5f, 0f, -0.5f, 1f, 11f, 1f),
			ModelTransform.of(-1.5f, 13f, -1f, 0f, 0f, 0.45f));
		ModelPartData legFL = root.addChild("legFL", ModelPartBuilder.create().mirrored().uv(48, 0).cuboid(-0.5f, 0f, -0.5f, 1f, 11f, 1f).mirrored(false),
			ModelTransform.of(1.5f, 13f, -1f, 0f, 0f, -0.45f));
		ModelPartData legBR = root.addChild("legBR", ModelPartBuilder.create().uv(52, 0).cuboid(-0.5f, 0f, -0.5f, 1f, 11f, 1f),
			ModelTransform.of(-1.5f, 13f, 1.5f, 0f, 0f, 0.45f));
		ModelPartData legBL = root.addChild("legBL", ModelPartBuilder.create().mirrored().uv(52, 0).cuboid(-0.5f, 0f, -0.5f, 1f, 11f, 1f).mirrored(false),
			ModelTransform.of(1.5f, 13f, 1.5f, 0f, 0f, -0.45f));
		return TexturedModelData.of(data, 64, 32);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(BladeMantisEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		legFR.pitch = walk; legBL.pitch = walk;
		legFL.pitch = -walk; legBR.pitch = -walk;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		float idle = MathHelper.sin(t * 0.1f) * 0.1f;
		float raise = entity.isWindingUp() ? action : 0f;
		float slash = entity.isStriking() ? action : 0f;
		armR.pitch = -0.9f + idle - raise * 1.6f + slash * 1.8f;
		armL.pitch = -0.9f - idle - raise * 1.6f + slash * 1.8f;
		bladeR.pitch = 0.6f + raise * 0.6f;
		bladeL.pitch = 0.6f + raise * 0.6f;
		abdomen.yaw = MathHelper.sin(t * 0.07f) * 0.1f;
	}
}
