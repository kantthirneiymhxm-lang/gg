package com.kaiju;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.util.HashSet;
import java.util.Set;

public class KaijuState extends PersistentState {
	public final Set<String> consumed = new HashSet<>();

	public static KaijuState get(ServerWorld overworld) {
		return overworld.getPersistentStateManager().getOrCreate(KaijuState::fromNbt, KaijuState::new, "kaiju_kings_lairs");
	}

	public static KaijuState fromNbt(NbtCompound n) {
		KaijuState s = new KaijuState();
		NbtList l = n.getList("c", NbtElement.STRING_TYPE);
		for (int i = 0; i < l.size(); i++) s.consumed.add(l.getString(i));
		return s;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound n) {
		NbtList l = new NbtList();
		for (String c : consumed) l.add(NbtString.of(c));
		n.put("c", l);
		return n;
	}
}
