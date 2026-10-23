package fr.nico.roguetower.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreeperEntity.class)
public interface CreeperEntityAccessor {
	@Accessor("fuseTime")
	void roguetower$setFuseTime(int fuseTime);

	@Accessor("explosionRadius")
	void roguetower$setExplosionRadius(int radius);
}
