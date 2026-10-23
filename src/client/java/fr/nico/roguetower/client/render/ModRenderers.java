package fr.nico.roguetower.client.render;

import fr.nico.roguetower.RogueTower;
import fr.nico.roguetower.client.render.model.BladeMantisModel;
import fr.nico.roguetower.client.render.model.CrawlerModel;
import fr.nico.roguetower.client.render.model.EmberGargoyleModel;
import fr.nico.roguetower.client.render.model.GluttonModel;
import fr.nico.roguetower.client.render.model.HowlingTotemModel;
import fr.nico.roguetower.client.render.model.RuneGolemModel;
import fr.nico.roguetower.client.render.model.ShadowWeaverModel;
import fr.nico.roguetower.client.render.model.SporelingModel;
import fr.nico.roguetower.client.render.model.VoidEyeModel;
import fr.nico.roguetower.entity.ModEntities;
import fr.nico.roguetower.entity.RogueMob;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;


public final class ModRenderers {
	private ModRenderers() {
	}

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(GluttonModel.LAYER, GluttonModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(BladeMantisModel.LAYER, BladeMantisModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(HowlingTotemModel.LAYER, HowlingTotemModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(CrawlerModel.LAYER, CrawlerModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(SporelingModel.LAYER, SporelingModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(EmberGargoyleModel.LAYER, EmberGargoyleModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(VoidEyeModel.LAYER, VoidEyeModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(RuneGolemModel.LAYER, RuneGolemModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(ShadowWeaverModel.LAYER, ShadowWeaverModel::getTexturedModelData);

		EntityRendererRegistry.register(ModEntities.GLUTTON, ctx -> new Renderer<>(ctx, new GluttonModel(ctx.getPart(GluttonModel.LAYER)), "glutton", 1.4f, 0.7f, false));
		EntityRendererRegistry.register(ModEntities.BLADE_MANTIS, ctx -> new Renderer<>(ctx, new BladeMantisModel(ctx.getPart(BladeMantisModel.LAYER)), "blade_mantis", 1.0f, 0.5f, false));
		EntityRendererRegistry.register(ModEntities.HOWLING_TOTEM, ctx -> new Renderer<>(ctx, new HowlingTotemModel(ctx.getPart(HowlingTotemModel.LAYER)), "howling_totem", 1.2f, 0.6f, false));
		EntityRendererRegistry.register(ModEntities.CRAWLER, ctx -> new Renderer<>(ctx, new CrawlerModel(ctx.getPart(CrawlerModel.LAYER)), "crawler", 1.1f, 0.5f, false));
		EntityRendererRegistry.register(ModEntities.SPORELING, ctx -> new Renderer<>(ctx, new SporelingModel(ctx.getPart(SporelingModel.LAYER)), "sporeling", 1.0f, 0.5f, true));
		EntityRendererRegistry.register(ModEntities.EMBER_GARGOYLE, ctx -> new Renderer<>(ctx, new EmberGargoyleModel(ctx.getPart(EmberGargoyleModel.LAYER)), "ember_gargoyle", 1.2f, 0.5f, true));
		EntityRendererRegistry.register(ModEntities.VOID_EYE, ctx -> new Renderer<>(ctx, new VoidEyeModel(ctx.getPart(VoidEyeModel.LAYER)), "void_eye", 1.4f, 0.5f, true));
		EntityRendererRegistry.register(ModEntities.RUNE_GOLEM, ctx -> new Renderer<>(ctx, new RuneGolemModel(ctx.getPart(RuneGolemModel.LAYER)), "rune_golem", 1.7f, 1.2f, false));
		EntityRendererRegistry.register(ModEntities.SHADOW_WEAVER, ctx -> new Renderer<>(ctx, new ShadowWeaverModel(ctx.getPart(ShadowWeaverModel.LAYER)), "shadow_weaver", 1.5f, 0.8f, true));
		EntityRendererRegistry.register(ModEntities.MAGIC_BOLT, EmptyEntityRenderer::new);
	}

	/** Rendu commun : modèle original, texture du mod, taille et lueur optionnelle. */
	static class Renderer<T extends RogueMob, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
		private final Identifier texture;
		private final float scale;
		private final boolean glow;

		Renderer(EntityRendererFactory.Context ctx, M model, String name, float scale, float shadow, boolean glow) {
			super(ctx, model, shadow * scale);
			this.texture = RogueTower.id("textures/entity/" + name + ".png");
			this.scale = scale;
			this.glow = glow;
		}

		@Override
		public Identifier getTexture(T entity) {
			return texture;
		}

		@Override
		protected void scale(T entity, MatrixStack matrices, float amount) {
			matrices.scale(scale, scale, scale);
		}

		@Override
		protected int getBlockLight(T entity, BlockPos pos) {
			return glow ? 15 : super.getBlockLight(entity, pos);
		}
	}

}
