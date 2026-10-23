package fr.nico.roguetower.client;

import fr.nico.roguetower.RogueTower;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Caméra vue du dessus façon Diablo : la caméra est fixe (nord en haut),
 * inclinée à 60°, et le personnage regarde vers le curseur de la souris.
 */
public final class TopDownCamera {
	public enum Mode {
		AUTO("Automatique (dans la tour)"), ON("Toujours"), OFF("Désactivée");

		final String label;

		Mode(String label) {
			this.label = label;
		}
	}

	public static final float CAMERA_YAW = 180f;
	public static final float CAMERA_PITCH = 60f;
	public static double distance = 16.0;

	public static Mode mode = Mode.AUTO;
	/** Curseur virtuel en coordonnées normalisées (-1..1), y vers le haut. */
	public static double cursorX;
	public static double cursorY = 0.3;

	private static boolean wasActive;
	private static Perspective previousPerspective = Perspective.FIRST_PERSON;

	private TopDownCamera() {
	}

	public static boolean isActive() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			return false;
		}
		return switch (mode) {
			case ON -> true;
			case OFF -> false;
			case AUTO -> client.world.getRegistryKey() == RogueTower.TOWER_WORLD;
		};
	}

	public static void cycleMode() {
		mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.sendMessage(Text.literal("Vue du dessus : " + mode.label).formatted(Formatting.GOLD), true);
		}
	}

	/** Appelé à chaque tick client : force la vue 3e personne quand la caméra est active. */
	public static void tick(MinecraftClient client) {
		boolean active = isActive();
		if (active && !wasActive) {
			previousPerspective = client.options.getPerspective();
			cursorX = 0;
			cursorY = 0.3;
		}
		if (active && client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
			client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		}
		if (!active && wasActive) {
			client.options.setPerspective(previousPerspective);
		}
		wasActive = active;
	}

	public static void onMouseMove(double dx, double dy) {
		MinecraftClient client = MinecraftClient.getInstance();
		double w = Math.max(1, client.getWindow().getWidth());
		double h = Math.max(1, client.getWindow().getHeight());
		cursorX = MathHelper.clamp(cursorX + dx * 2.0 / w, -1, 1);
		cursorY = MathHelper.clamp(cursorY - dy * 2.0 / h, -1, 1);
	}

	public static Vec3d forward() {
		double yaw = Math.toRadians(CAMERA_YAW);
		double pitch = Math.toRadians(CAMERA_PITCH);
		return new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
	}

	public static Vec3d cameraPos(Entity entity, float tickDelta) {
		Vec3d focus = entity.getLerpedPos(tickDelta).add(0, 1.0, 0);
		return focus.subtract(forward().multiply(distance));
	}

	/** Oriente le joueur vers le point du sol sous le curseur. */
	public static void aimPlayer(ClientPlayerEntity player, Vec3d camPos) {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d f = forward();
		Vec3d right = new Vec3d(-Math.cos(Math.toRadians(CAMERA_YAW)), 0, -Math.sin(Math.toRadians(CAMERA_YAW)));
		Vec3d up = right.crossProduct(f);
		double fov = client.options.getFov().getValue();
		double tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
		double aspect = client.getWindow().getFramebufferWidth() / (double) Math.max(1, client.getWindow().getFramebufferHeight());
		Vec3d dir = f.add(right.multiply(cursorX * tanHalf * aspect)).add(up.multiply(cursorY * tanHalf));
		double planeY = player.getY() + 1.0;
		if (dir.y >= -1e-4) {
			return;
		}
		double t = (planeY - camPos.y) / dir.y;
		Vec3d target = camPos.add(dir.multiply(t));
		// Visée assistée : si un monstre est sous le curseur, on vise son centre
		LivingEntity best = null;
		double bestDist = 1.6 * 1.6;
		for (Entity e : client.world.getOtherEntities(player, new Box(target, target).expand(2.0, 3.0, 2.0))) {
			if (e instanceof LivingEntity living && living.isAlive() && !(e instanceof PlayerEntity)) {
				double d = new Vec3d(living.getX() - target.x, 0, living.getZ() - target.z).lengthSquared();
				if (d < bestDist && living.squaredDistanceTo(player) < 12 * 12) {
					bestDist = d;
					best = living;
				}
			}
		}
		if (best != null) {
			target = new Vec3d(best.getX(), best.getBodyY(0.5), best.getZ());
			planeY = target.y;
		}
		double dx = target.x - player.getX();
		double dz = target.z - player.getZ();
		double horiz = Math.sqrt(dx * dx + dz * dz);
		if (horiz < 0.3) {
			return;
		}
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) Math.toDegrees(Math.atan2(player.getEyeY() - planeY, horiz));
		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setPitch(MathHelper.clamp(pitch, -30f, 60f));
		player.prevYaw = yaw;
		player.prevPitch = player.getPitch();
	}
}
