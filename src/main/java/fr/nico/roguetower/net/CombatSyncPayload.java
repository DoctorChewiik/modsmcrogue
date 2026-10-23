package fr.nico.roguetower.net;

import fr.nico.roguetower.RogueTower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Serveur -> client : endurance (0-100), combo actuel et dernier événement de combat à afficher. */
public record CombatSyncPayload(int stamina, int combo, int event) implements CustomPayload {
	public static final int EVENT_NONE = 0;
	public static final int EVENT_DODGE = 1;
	public static final int EVENT_PARRY = 2;
	public static final int EVENT_FINISHER = 3;
	public static final int EVENT_BACKSTAB = 4;
	public static final int EVENT_RIPOSTE = 5;
	public static final Id<CombatSyncPayload> ID = new Id<>(RogueTower.id("combat_sync"));
	public static final PacketCodec<RegistryByteBuf, CombatSyncPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, CombatSyncPayload::stamina,
		PacketCodecs.VAR_INT, CombatSyncPayload::combo,
		PacketCodecs.VAR_INT, CombatSyncPayload::event,
		CombatSyncPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
