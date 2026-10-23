package fr.nico.roguetower.net;

import fr.nico.roguetower.RogueTower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Serveur -> client : classe, sort actuel, recharges restantes (en ticks) et
 * s'il faut ouvrir l'écran de choix de classe.
 */
public record ClassSyncPayload(int classId, int spellId, int abilityCooldown, int spellCooldown, boolean openScreen)
	implements CustomPayload {
	public static final Id<ClassSyncPayload> ID = new Id<>(RogueTower.id("class_sync"));
	public static final PacketCodec<RegistryByteBuf, ClassSyncPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.VAR_INT, ClassSyncPayload::classId,
		PacketCodecs.VAR_INT, ClassSyncPayload::spellId,
		PacketCodecs.VAR_INT, ClassSyncPayload::abilityCooldown,
		PacketCodecs.VAR_INT, ClassSyncPayload::spellCooldown,
		PacketCodecs.BOOL, ClassSyncPayload::openScreen,
		ClassSyncPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
