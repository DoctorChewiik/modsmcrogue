package fr.nico.roguetower.client.render.model;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.entity.CrawlerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Modèle original généré (formes et texture créées pour Rogue Tower). */
public class CrawlerModel extends SinglePartEntityModel<CrawlerEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id("crawler"), "main");
	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart seg0;
	private final ModelPart seg1;
	private final ModelPart seg2;
	private final ModelPart seg3;

	public CrawlerModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("head");
		this.jaw = head.getChild("jaw");
		this.seg0 = root.getChild("seg0");
		this.seg1 = root.getChild("seg1");
		this.seg2 = root.getChild("seg2");
		this.seg3 = root.getChild("seg3");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-3f, -3f, -5f, 6f, 5f, 5f),
			ModelTransform.of(0f, 21f, -3f, 0f, 0f, 0f));
		ModelPartData jaw = head.addChild("jaw", ModelPartBuilder.create().uv(22, 0).cuboid(-2.5f, 0f, -2f, 5f, 1f, 2f),
			ModelTransform.of(0f, 1.5f, -5f, 0f, 0f, 0f));
		ModelPartData seg0 = root.addChild("seg0", ModelPartBuilder.create().uv(0, 10).cuboid(-2.5f, -2.5f, 0f, 5f, 5f, 4f).uv(18, 10).cuboid(-0.5f, -4.5f, 1f, 1f, 2f, 1f).uv(22, 10).cuboid(-3.5f, 1.5f, 1.5f, 7f, 1f, 1f),
			ModelTransform.of(0f, 21f, -3f, 0f, 0f, 0f));
		ModelPartData seg1 = root.addChild("seg1", ModelPartBuilder.create().uv(0, 10).cuboid(-2.5f, -2.5f, 0f, 5f, 5f, 4f).uv(18, 10).cuboid(-0.5f, -4.5f, 1f, 1f, 2f, 1f).uv(22, 10).cuboid(-3.5f, 1.5f, 1.5f, 7f, 1f, 1f),
			ModelTransform.of(0f, 21f, 1f, 0f, 0f, 0f));
		ModelPartData seg2 = root.addChild("seg2", ModelPartBuilder.create().uv(0, 10).cuboid(-2.5f, -2.5f, 0f, 5f, 5f, 4f).uv(18, 10).cuboid(-0.5f, -4.5f, 1f, 1f, 2f, 1f).uv(22, 10).cuboid(-3.5f, 1.5f, 1.5f, 7f, 1f, 1f),
			ModelTransform.of(0f, 21f, 5f, 0f, 0f, 0f));
		ModelPartData seg3 = root.addChild("seg3", ModelPartBuilder.create().uv(0, 10).cuboid(-2.5f, -2.5f, 0f, 5f, 5f, 4f).uv(18, 10).cuboid(-0.5f, -4.5f, 1f, 1f, 2f, 1f).uv(22, 10).cuboid(-3.5f, 1.5f, 1.5f, 7f, 1f, 1f),
			ModelTransform.of(0f, 21f, 9f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 32);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(CrawlerEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		root.traverse().forEach(ModelPart::resetTransform);
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
		float t = animationProgress;
		float action = entity.getActionProgress();
		for (int i = 0; i < 4; i++) {
		ModelPart s = switch (i) { case 0 -> seg0; case 1 -> seg1; case 2 -> seg2; default -> seg3; };
		s.pivotX = MathHelper.sin(limbAngle * 0.8f - i * 0.9f) * 1.6f * Math.min(1f, limbDistance * 2f + 0.15f);
		s.yaw = MathHelper.cos(limbAngle * 0.8f - i * 0.9f) * 0.25f;
		}
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.5f;
		head.pitch = entity.isWindingUp() ? -action * 0.6f : 0f;
		jaw.pitch = 0.2f + MathHelper.sin(t * 0.4f) * 0.15f + (entity.isWindingUp() ? action * 0.6f : 0f);
	}
}
