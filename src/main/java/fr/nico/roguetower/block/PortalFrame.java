package fr.nico.roguetower.block;

import fr.nico.roguetower.RogueTower;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Détecte un cadre de cobblestone dont l'ouverture fait 4x4 blocs. */
public final class PortalFrame {
	public static final int SIZE = 4;

	private PortalFrame() {
	}

	/**
	 * Cherche un cadre contenant {@code inside}. Si trouvé et que {@code place}
	 * est vrai, remplit l'ouverture avec le bloc de portail.
	 */
	public static boolean tryLight(World world, BlockPos inside, boolean place) {
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
			for (int ox = 0; ox < SIZE; ox++) {
				for (int oy = 0; oy < SIZE; oy++) {
					BlockPos origin = inside.offset(right, -ox).down(oy);
					if (isValid(world, origin, right)) {
						if (place) {
							BlockState portal = RogueTower.TOWER_PORTAL.getDefaultState().with(TowerPortalBlock.AXIS, axis);
							for (int i = 0; i < SIZE; i++) {
								for (int j = 0; j < SIZE; j++) {
									world.setBlockState(origin.offset(right, i).up(j), portal, 2 | 16);
								}
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isValid(World world, BlockPos origin, Direction right) {
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				BlockState state = world.getBlockState(origin.offset(right, i).up(j));
				if (!state.isAir() && !state.isOf(Blocks.FIRE)) {
					return false;
				}
			}
		}
		for (int i = 0; i < SIZE; i++) {
			if (!isFrame(world, origin.offset(right, i).down())
				|| !isFrame(world, origin.offset(right, i).up(SIZE))
				|| !isFrame(world, origin.offset(right, -1).up(i))
				|| !isFrame(world, origin.offset(right, SIZE).up(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isFrame(World world, BlockPos pos) {
		return world.getBlockState(pos).isOf(Blocks.COBBLESTONE);
	}
}
