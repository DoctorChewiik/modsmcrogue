package fr.nico.roguetower.world;

import fr.nico.roguetower.classes.RogueClass;
import fr.nico.roguetower.classes.Spell;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Données sauvegardées avec le monde : classe, sort et point de retour de chaque joueur. */
public class RogueState extends PersistentState {
	public static final PersistentState.Type<RogueState> TYPE =
		new PersistentState.Type<>(RogueState::new, RogueState::fromNbt, null);

	public static final class PlayerData {
		public RogueClass rogueClass = RogueClass.NONE;
		public Spell spell = Spell.NONE;
		public String returnDimension = "";
		public double returnX;
		public double returnY;
		public double returnZ;
		public int bestFloor;
	}

	private final Map<UUID, PlayerData> players = new HashMap<>();

	public static RogueState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "roguetower");
	}

	public PlayerData get(UUID uuid) {
		return players.computeIfAbsent(uuid, u -> new PlayerData());
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		NbtCompound list = new NbtCompound();
		players.forEach((uuid, data) -> {
			NbtCompound tag = new NbtCompound();
			tag.putString("class", data.rogueClass.name());
			tag.putString("spell", data.spell.name());
			tag.putString("returnDim", data.returnDimension);
			tag.putDouble("returnX", data.returnX);
			tag.putDouble("returnY", data.returnY);
			tag.putDouble("returnZ", data.returnZ);
			tag.putInt("bestFloor", data.bestFloor);
			list.put(uuid.toString(), tag);
		});
		nbt.put("players", list);
		return nbt;
	}

	public static RogueState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		RogueState state = new RogueState();
		NbtCompound list = nbt.getCompound("players");
		for (String key : list.getKeys()) {
			try {
				NbtCompound tag = list.getCompound(key);
				PlayerData data = new PlayerData();
				data.rogueClass = RogueClass.byName(tag.getString("class"));
				data.spell = Spell.byName(tag.getString("spell"));
				data.returnDimension = tag.getString("returnDim");
				data.returnX = tag.getDouble("returnX");
				data.returnY = tag.getDouble("returnY");
				data.returnZ = tag.getDouble("returnZ");
				data.bestFloor = tag.getInt("bestFloor");
				state.players.put(UUID.fromString(key), data);
			} catch (IllegalArgumentException ignored) {
			}
		}
		return state;
	}
}
