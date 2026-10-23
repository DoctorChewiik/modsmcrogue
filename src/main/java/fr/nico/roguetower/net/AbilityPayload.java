package fr.nico.roguetower.net;

import fr.nico.roguetower.RogueTower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client -> serveur : le joueur active la compétence 1 ou 2. */
public record AbilityPayload(int slot) implements CustomPayload {
	public static final Id<AbilityPayload> ID = new Id<>(RogueTower.id("ability"));
	public static final PacketCodec<RegistryByteBuf, AbilityPayload> CODEC =
		PacketCodec.tuple(PacketCodecs.VAR_INT, AbilityPayload::slot, AbilityPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
