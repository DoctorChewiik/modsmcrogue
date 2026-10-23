package fr.nico.roguetower.net;

import fr.nico.roguetower.RogueTower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client -> serveur : le joueur choisit une classe. */
public record SelectClassPayload(int classId) implements CustomPayload {
	public static final Id<SelectClassPayload> ID = new Id<>(RogueTower.id("select_class"));
	public static final PacketCodec<RegistryByteBuf, SelectClassPayload> CODEC =
		PacketCodec.tuple(PacketCodecs.VAR_INT, SelectClassPayload::classId, SelectClassPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
