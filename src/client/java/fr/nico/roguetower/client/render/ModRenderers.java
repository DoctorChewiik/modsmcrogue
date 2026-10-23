package fr.nico.roguetower.client.render;

import fr.nico.roguetower.entity.CrystalSpiderEntity;
import fr.nico.roguetower.entity.LichEntity;
import fr.nico.roguetower.entity.ModEntities;
import fr.nico.roguetower.entity.VoidEyeEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EvokerEntityRenderer;
import net.minecraft.client.render.entity.HoglinEntityRenderer;
import net.minecraft.client.render.entity.IronGolemEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.SkeletonEntityRenderer;
import net.minecraft.client.render.entity.SpiderEntityRenderer;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.Identifier;

import static fr.nico.roguetower.client.render.RecoloredTextures.argb;
import static fr.nico.roguetower.client.render.RecoloredTextures.get;
import static fr.nico.roguetower.client.render.RecoloredTextures.lum;
import static fr.nico.roguetower.client.render.RecoloredTextures.noise;
import static fr.nico.roguetower.client.render.RecoloredTextures.tint;

public final class ModRenderers {
	private ModRenderers() {
	}

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(VoidEyeModel.LAYER, VoidEyeModel::getTexturedModelData);

		EntityRendererRegistry.register(ModEntities.BRUTE, (EntityRendererFactory<ZombieEntity>) BruteRenderer::new);
		EntityRendererRegistry.register(ModEntities.CRYSTAL_SPIDER, CrystalSpiderRenderer::new);
		EntityRendererRegistry.register(ModEntities.EMBER_SKELETON, (EntityRendererFactory<AbstractSkeletonEntity>) EmberSkeletonRenderer::new);
		EntityRendererRegistry.register(ModEntities.VOID_EYE, VoidEyeRenderer::new);
		EntityRendererRegistry.register(ModEntities.MYCOBOMB, (EntityRendererFactory<CreeperEntity>) MycobombRenderer::new);
		EntityRendererRegistry.register(ModEntities.BOAR, (EntityRendererFactory<HoglinEntity>) BoarRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPECTRAL_WOLF, (EntityRendererFactory<WolfEntity>) SpectralWolfRenderer::new);
		EntityRendererRegistry.register(ModEntities.LICH, LichRenderer::new);
		EntityRendererRegistry.register(ModEntities.STONE_SENTINEL, (EntityRendererFactory<IronGolemEntity>) SentinelRenderer::new);
	}

	static class BruteRenderer extends ZombieEntityRenderer {
		BruteRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(ZombieEntity entity) {
			return get("brute", Identifier.ofVanilla("textures/entity/zombie/zombie.png"),
				(r, g, b, a, x, y) -> tint(r, g, b, a, 0.95, 0.42, 0.34, 10));
		}

		@Override
		protected void scale(ZombieEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.55f, 1.55f, 1.55f);
		}
	}

	static class CrystalSpiderRenderer extends SpiderEntityRenderer<CrystalSpiderEntity> {
		CrystalSpiderRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
			this.shadowRadius = 0.6f;
		}

		@Override
		public Identifier getTexture(CrystalSpiderEntity entity) {
			return get("crystal_spider", Identifier.ofVanilla("textures/entity/spider/spider.png"),
				(r, g, b, a, x, y) -> noise(x, y, 3) % 9 == 0
					? argb(a, 230, 250, 255)
					: tint(r, g, b, a, 0.55, 0.95, 1.25, 45));
		}

		@Override
		protected void scale(CrystalSpiderEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(0.75f, 0.75f, 0.75f);
		}
	}

	static class EmberSkeletonRenderer extends SkeletonEntityRenderer {
		EmberSkeletonRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(AbstractSkeletonEntity entity) {
			return get("ember_skeleton", Identifier.ofVanilla("textures/entity/skeleton/skeleton.png"),
				(r, g, b, a, x, y) -> {
					int l = lum(r, g, b);
					if (l > 150 && noise(x, y, 7) % 5 == 0) {
						return argb(a, 255, 140 + noise(x, y, 8) % 80, 20);
					}
					return argb(a, l * 0.28, l * 0.22, l * 0.2);
				});
		}
	}

	static class VoidEyeRenderer extends MobEntityRenderer<VoidEyeEntity, VoidEyeModel> {
		private static final Identifier TEXTURE = fr.nico.roguetower.RogueTower.id("textures/entity/void_eye.png");

		VoidEyeRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new VoidEyeModel(ctx.getPart(VoidEyeModel.LAYER)), 0.5f);
		}

		@Override
		public Identifier getTexture(VoidEyeEntity entity) {
			return TEXTURE;
		}
	}

	static class MycobombRenderer extends CreeperEntityRenderer {
		MycobombRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(CreeperEntity entity) {
			return get("mycobomb", Identifier.ofVanilla("textures/entity/creeper/creeper.png"),
				(r, g, b, a, x, y) -> {
					if (y < 16 && noise(x / 2, y / 2, 11) % 5 == 0) {
						return argb(a, 245, 240, 225);
					}
					int l = lum(r, g, b);
					return y < 16 ? argb(a, 120 + l * 0.6, l * 0.15, l * 0.12) : argb(a, 200 + l * 0.2, 190 + l * 0.2, 160 + l * 0.2);
				});
		}

		@Override
		protected void scale(CreeperEntity entity, MatrixStack matrices, float amount) {
			super.scale(entity, matrices, amount);
			matrices.scale(0.65f, 0.65f, 0.65f);
		}
	}

	static class BoarRenderer extends HoglinEntityRenderer {
		BoarRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(HoglinEntity entity) {
			return get("boar", Identifier.ofVanilla("textures/entity/hoglin/hoglin.png"),
				(r, g, b, a, x, y) -> tint(r, g, b, a, 0.62, 0.48, 0.38, 0));
		}
	}

	static class SpectralWolfRenderer extends WolfEntityRenderer {
		SpectralWolfRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(WolfEntity entity) {
			return get("spectral_wolf", Identifier.ofVanilla("textures/entity/wolf/wolf_angry.png"),
				(r, g, b, a, x, y) -> tint(r, g, b, a, 0.55, 0.85, 1.1, 70));
		}
	}

	static class LichRenderer extends EvokerEntityRenderer<LichEntity> {
		LichRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(LichEntity entity) {
			return get("lich", Identifier.ofVanilla("textures/entity/illager/evoker.png"),
				(r, g, b, a, x, y) -> {
					int l = lum(r, g, b);
					if (y < 16 && x >= 8 && x < 16 && y >= 10 && y < 13 && l > 60) {
						return argb(a, 120, 255, 60); // yeux verts
					}
					return argb(a, l * 0.38, l * 0.3, l * 0.55);
				});
		}

		@Override
		protected void scale(LichEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.15f, 1.15f, 1.15f);
		}
	}

	static class SentinelRenderer extends IronGolemEntityRenderer {
		SentinelRenderer(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public Identifier getTexture(IronGolemEntity entity) {
			return get("stone_sentinel", Identifier.ofVanilla("textures/entity/iron_golem/iron_golem.png"),
				(r, g, b, a, x, y) -> noise(x, y, 5) % 6 == 0
					? argb(a, 70, 110 + noise(x, y, 6) % 40, 50)
					: tint(r, g, b, a, 0.5, 0.56, 0.46, 0));
		}
	}
}
