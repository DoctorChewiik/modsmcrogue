package fr.nico.roguetower.net;

import fr.nico.roguetower.RogueTower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client -> serveur : mouvement de combat (0 = esquive, 1 = parade) et direction voulue. */
public record CombatPayload(int action, float dirX, float dirZ) implements CustomPayload {
	public static final int DODGE = 0;
	public static final int PARRY = 1;
	public static final Id<CombatPayload> ID = new Id<>(RogueTower.id("combat"));
	public static final PacketCodec<RegistryByteBuf, CombatPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, CombatPayload::action,
		PacketCodecs.FLOAT, CombatPayload::dirX,
		PacketCodecs.FLOAT, CombatPayload::dirZ,
		CombatPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
