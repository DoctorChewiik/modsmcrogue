package fr.nico.roguetower.entity;

import fr.nico.roguetower.mob.MobSetup;
import fr.nico.roguetower.world.TowerLayout;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Liche : boss sorcier qui invoque des vex, des crocs et relève les morts. */
public class LichEntity extends EvokerEntity {
	public static final String RAISED_TAG = "roguetower_raised";
	private int raiseCooldown = 120;

	public LichEntity(EntityType<? extends EvokerEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 60;
	}

	public static DefaultAttributeContainer.Builder createLichAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 90.0)
			.add(EntityAttributes.GENERIC_ARMOR, 4.0);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (getWorld() instanceof ServerWorld world && getTarget() != null && --raiseCooldown <= 0) {
			raiseCooldown = 240;
			int alive = world.getEntitiesByClass(MobEntity.class, getBoundingBox().expand(24), e -> e.getCommandTags().contains(RAISED_TAG)).size();
			if (alive >= 4) {
				return;
			}
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1f, 1.4f);
			for (int i = 0; i < 2; i++) {
				BlockPos pos = getBlockPos().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
				world.spawnParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
				EntityType<?> type = random.nextBoolean() ? EntityType.SKELETON : EntityType.ZOMBIE;
				MobEntity minion = MobSetup.spawn(world, type, pos, Math.max(1, TowerLayout.floorOf(getX())), false, getTarget());
				if (minion != null) {
					minion.addCommandTag(RAISED_TAG);
				}
			}
		}
	}
}
