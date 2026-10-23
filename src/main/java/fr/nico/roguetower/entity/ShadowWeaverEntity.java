package fr.nico.roguetower.entity;

import fr.nico.roguetower.mob.MobSetup;
import fr.nico.roguetower.world.TowerLayout;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Tisseuse d'Ombres (boss) : rassemble ses quatre mains puis lance une salve d'ombres, invoque, se téléporte. */
public class ShadowWeaverEntity extends FlyingRogueMob {
	public static final String SUMMON_TAG = "roguetower_weaver_minion";
	private int pattern;

	public ShadowWeaverEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 60;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
			.add(EntityAttributes.GENERIC_ARMOR, 4.0);
	}

	@Override
	protected Vec3d hoverPosition(LivingEntity target) {
		double a = age * 0.02;
		return target.getPos().add(Math.cos(a) * 6, 2.0, Math.sin(a) * 6);
	}

	@Override
	protected void tickSpecial(ServerWorld world, LivingEntity target) {
		pattern = (pattern + 1) % 3;
		specialCooldown = 50;
		startAction(ACTION_WINDUP, pattern == 1 ? 26 : 20);
		playSound(pattern == 1 ? SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON : SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.2f, 0.6f);
	}

	@Override
	protected void onWindupDone(ServerWorld world) {
		LivingEntity target = getTarget();
		if (target == null) {
			return;
		}
		switch (pattern) {
			case 1 -> {
				int alive = world.getEntitiesByClass(MobEntity.class, getBoundingBox().expand(30), e -> e.getCommandTags().contains(SUMMON_TAG)).size();
				for (int i = alive; i < Math.min(alive + 2, 4); i++) {
					BlockPos pos = target.getBlockPos().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
					world.spawnParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
					EntityType<?> type = random.nextBoolean() ? ModEntities.CRAWLER : ModEntities.SPORELING;
					MobEntity minion = MobSetup.spawn(world, type, pos, Math.max(1, TowerLayout.floorOf(getX())), false,
						target instanceof PlayerEntity p ? p : null);
					if (minion != null) {
						minion.addCommandTag(SUMMON_TAG);
					}
				}
				playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.2f, 0.5f);
			}
			case 2 -> {
				world.spawnParticles(ParticleTypes.SQUID_INK, getX(), getBodyY(0.5), getZ(), 30, 0.5, 0.8, 0.5, 0.05);
				double a = random.nextDouble() * Math.PI * 2;
				teleport(target.getX() + Math.cos(a) * 5, target.getY() + 2, target.getZ() + Math.sin(a) * 5, true);
				for (int i = -3; i <= 3; i++) {
					shootBolt(world, target, MagicBoltEntity.SHADOW, 4f, 0.45, i * 9f);
				}
			}
			default -> {
				for (int i = -2; i <= 2; i++) {
					shootBolt(world, target, MagicBoltEntity.SHADOW, 5f, 0.55, i * 10f);
				}
				playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 0.6f);
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VEX_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_VEX_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return 0.5f;
	}
}
