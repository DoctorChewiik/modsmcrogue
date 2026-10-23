package fr.nico.roguetower.net;

import fr.nico.roguetower.RogueTower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Serveur -> clients proches : jouer une animation de combat sur un joueur. */
public record CombatAnimPayload(int entityId, int anim, float dirX, float dirZ) implements CustomPayload {
	public static final int DODGE = 0;
	public static final int PARRY = 1;
	public static final int PARRY_SUCCESS = 2;
	public static final int FINISHER = 3;
	public static final int THRUST = 4;
	public static final int CAST = 5;
	public static final Id<CombatAnimPayload> ID = new Id<>(RogueTower.id("combat_anim"));
	public static final PacketCodec<RegistryByteBuf, CombatAnimPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, CombatAnimPayload::entityId,
		PacketCodecs.VAR_INT, CombatAnimPayload::anim,
		PacketCodecs.FLOAT, CombatAnimPayload::dirX,
		PacketCodecs.FLOAT, CombatAnimPayload::dirZ,
		CombatAnimPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
