package fr.nico.roguetower.entity;

import fr.nico.roguetower.mixin.CreeperEntityAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

/** Champibombe : petit champignon très rapide à la mèche courte. */
public class MycobombEntity extends CreeperEntity {
	public MycobombEntity(EntityType<? extends CreeperEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 6;
		((CreeperEntityAccessor) this).roguetower$setFuseTime(18);
		((CreeperEntityAccessor) this).roguetower$setExplosionRadius(2);
	}

	public static DefaultAttributeContainer.Builder createMycobombAttributes() {
		return CreeperEntity.createCreeperAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		// avance en sautillant
		if (!getWorld().isClient && isOnGround() && getTarget() != null && random.nextInt(12) == 0) {
			jump();
		}
		if (getWorld().isClient && random.nextInt(3) == 0) {
			getWorld().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, getParticleX(0.6), getRandomBodyY(), getParticleZ(0.6), 0, 0, 0);
		}
	}
}
