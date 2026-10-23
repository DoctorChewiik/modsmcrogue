package fr.nico.roguetower.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TowerChunkGenerator extends ChunkGenerator {
	public static final MapCodec<TowerChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
	).apply(instance, TowerChunkGenerator::new));

	public TowerChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
					  StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
	}

	@Override
	public void populateEntities(ChunkRegion region) {
	}

	@Override
	public int getWorldHeight() {
		return 256;
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
												  StructureAccessor structureAccessor, Chunk chunk) {
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		Heightmap oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
		Heightmap surface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				int x = startX + lx;
				int z = startZ + lz;
				if (!TowerLayout.inTower(x, z)) {
					continue;
				}
				for (int y = TowerLayout.FLOOR_Y - 4; y <= TowerLayout.WALL_TOP + 1; y++) {
					BlockState state = TowerLayout.blockAt(x, y, z);
					if (state != null) {
						chunk.setBlockState(pos.set(x, y, z), state, false);
						oceanFloor.trackUpdate(lx, y, lz, state);
						surface.trackUpdate(lx, y, lz, state);
					}
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinimumY() {
		return 0;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		for (int y = TowerLayout.WALL_TOP + 1; y >= TowerLayout.FLOOR_Y - 4; y--) {
			BlockState state = TowerLayout.blockAt(x, y, z);
			if (state != null && heightmap.getBlockPredicate().test(state)) {
				return y + 1;
			}
		}
		return world.getBottomY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		int bottom = world.getBottomY();
		BlockState[] states = new BlockState[world.getHeight()];
		for (int i = 0; i < states.length; i++) {
			BlockState state = TowerLayout.blockAt(x, bottom + i, z);
			states[i] = state == null ? Blocks.AIR.getDefaultState() : state;
		}
		return new VerticalBlockSample(bottom, states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		text.add("Rogue Tower - étage " + TowerLayout.floorOf(pos.getX()));
	}
}
