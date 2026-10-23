package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.SporelingEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class SporelingModel extends SinglePartEntityModel<SporelingEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("sporeling"), "main");
	private final ModelPart root;
	private final ModelPart cap;
	private final ModelPart stem;
	private final ModelPart legR;
	private final ModelPart legL;

	public SporelingModel(ModelPart root) {
		this.root = root;
		this.cap = root.getChild("cap");
		this.stem = root.getChild("stem");
		this.legR = root.getChild("legR");
		this.legL = root.getChild("legL");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData cap = root.addChild("cap", ModelPartBuilder.create().uv(0, 0).cuboid(-6f, -5f, -6f, 12f, 5f, 12f),
			ModelTransform.of(0f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData stem = root.addChild("stem", ModelPartBuilder.create().uv(0, 17).cuboid(-3f, 0f, -3f, 6f, 6f, 6f),
			ModelTransform.of(0f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData legR = root.addChild("legR", ModelPartBuilder.create().uv(24, 17).cuboid(-1f, 0f, -1f, 2f, 4f, 2f),
			ModelTransform.of(-1.5f, 20f, 0f, 0f, 0f, 0f));
		ModelPartData legL = root.addChild("legL", ModelPartBuilder.create().mirrored().uv(24, 17).cuboid(-1f, 0f, -1f, 2f, 4f, 2f).mirrored(false),
			ModelTransform.of(1.5f, 20f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 32);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(SporelingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		legR.pitch = walk * 1.4f;
		legL.pitch = -walk * 1.4f;
		float wobble = MathHelper.sin(limbAngle * 0.66f) * limbDistance * 0.3f;
		cap.roll = wobble;
		stem.roll = wobble * 0.5f;
		float swell = entity.isWindingUp() ? 1f + action * 0.6f + MathHelper.sin(t * 2f) * 0.05f : 1f;
		cap.xScale = swell; cap.yScale = swell; cap.zScale = swell;
	}
}
