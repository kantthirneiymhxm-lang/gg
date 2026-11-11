package com.kaiju;

import com.kaiju.entity.GhidorahEntity;
import com.kaiju.entity.GodzillaEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KaijuMod implements ModInitializer {
	public static final String ID = "kaiju_kings";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	public static Identifier id(String s) { return new Identifier(ID, s); }

	private static FabricItemSettings s(Rarity r) { return new FabricItemSettings().rarity(r); }

	public static final Item GODZILLA_CELL = new KaijuCellItem(0, s(Rarity.EPIC));
	public static final Item GHIDORAH_CELL = new KaijuCellItem(1, s(Rarity.EPIC));
	public static final Item GODZILLA_COMPASS = new KaijuCompassItem(0, s(Rarity.RARE).maxCount(1));
	public static final Item GHIDORAH_COMPASS = new KaijuCompassItem(1, s(Rarity.RARE).maxCount(1));

	public static final Item GODZILLA_HELMET = new ArmorItem(KaijuArmor.GODZILLA, ArmorItem.Type.HELMET, s(Rarity.EPIC));
	public static final Item GODZILLA_CHESTPLATE = new ArmorItem(KaijuArmor.GODZILLA, ArmorItem.Type.CHESTPLATE, s(Rarity.EPIC));
	public static final Item GODZILLA_LEGGINGS = new ArmorItem(KaijuArmor.GODZILLA, ArmorItem.Type.LEGGINGS, s(Rarity.EPIC));
	public static final Item GODZILLA_BOOTS = new ArmorItem(KaijuArmor.GODZILLA, ArmorItem.Type.BOOTS, s(Rarity.EPIC));
	public static final Item GHIDORAH_HELMET = new ArmorItem(KaijuArmor.GHIDORAH, ArmorItem.Type.HELMET, s(Rarity.EPIC));
	public static final Item GHIDORAH_CHESTPLATE = new ArmorItem(KaijuArmor.GHIDORAH, ArmorItem.Type.CHESTPLATE, s(Rarity.EPIC));
	public static final Item GHIDORAH_LEGGINGS = new ArmorItem(KaijuArmor.GHIDORAH, ArmorItem.Type.LEGGINGS, s(Rarity.EPIC));
	public static final Item GHIDORAH_BOOTS = new ArmorItem(KaijuArmor.GHIDORAH, ArmorItem.Type.BOOTS, s(Rarity.EPIC));

	public static final Item[][] SETS = {
		{GODZILLA_HELMET, GODZILLA_CHESTPLATE, GODZILLA_LEGGINGS, GODZILLA_BOOTS},
		{GHIDORAH_HELMET, GHIDORAH_CHESTPLATE, GHIDORAH_LEGGINGS, GHIDORAH_BOOTS}
	};

	public static final EntityType<GodzillaEntity> GODZILLA = FabricEntityTypeBuilder
		.create(SpawnGroup.MONSTER, GodzillaEntity::new)
		.dimensions(EntityDimensions.fixed(30f, 90f)).trackRangeBlocks(2048).trackedUpdateRate(2).fireImmune().build();
	public static final EntityType<GhidorahEntity> GHIDORAH = FabricEntityTypeBuilder
		.create(SpawnGroup.MONSTER, GhidorahEntity::new)
		.dimensions(EntityDimensions.fixed(80f, 136f)).trackRangeBlocks(2048).trackedUpdateRate(1)
		.forceTrackedVelocityUpdates(true).fireImmune().build();

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Set<UUID> FLYERS = new HashSet<>();

	public static boolean fullSet(PlayerEntity p, int type) {
		for (int i = 0; i < 4; i++) if (p.getEquippedStack(SLOTS[i]).getItem() != SETS[type][i]) return false;
		return true;
	}

	private static void reg(String name, Item item) { Registry.register(Registries.ITEM, id(name), item); }

	@Override
	public void onInitialize() {
		reg("godzilla_cell", GODZILLA_CELL);
		reg("king_ghidorah_cell", GHIDORAH_CELL);
		reg("godzilla_compass", GODZILLA_COMPASS);
		reg("king_ghidorah_compass", GHIDORAH_COMPASS);
		String[] parts = {"helmet", "chestplate", "leggings", "boots"};
		for (int i = 0; i < 4; i++) {
			reg("godzilla_" + parts[i], SETS[0][i]);
			reg("king_ghidorah_" + parts[i], SETS[1][i]);
		}
		Registry.register(Registries.ENTITY_TYPE, id("godzilla"), GODZILLA);
		Registry.register(Registries.ENTITY_TYPE, id("king_ghidorah"), GHIDORAH);
		FabricDefaultAttributeRegistry.register(GODZILLA, GodzillaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHIDORAH, GhidorahEntity.createAttributes());
		Item egg1 = new SpawnEggItem(GODZILLA, 0x2e3b33, 0x5cc8ff, new FabricItemSettings());
		Item egg2 = new SpawnEggItem(GHIDORAH, 0xd6aa28, 0xe62020, new FabricItemSettings());
		reg("godzilla_spawn_egg", egg1);
		reg("king_ghidorah_spawn_egg", egg2);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> {
			e.add(GODZILLA_CELL); e.add(GHIDORAH_CELL); e.add(GODZILLA_COMPASS); e.add(GHIDORAH_COMPASS);
			for (Item[] set : SETS) for (Item it : set) e.add(it);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> { e.add(egg1); e.add(egg2); });

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Abilities.tickJobs();
			int tk = server.getTicks();
			if (tk % 20 == 0) LairManager.tick(server);
			if (tk % 10 == 0) for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) applySetBonus(p);
		});
		LOGGER.info("Kaiju Kings loaded: Godzilla vs King Ghidorah!");
	}

	private static void applySetBonus(ServerPlayerEntity p) {
		boolean god = fullSet(p, 0), gh = fullSet(p, 1);
		if (god || gh) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 2, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 2, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
		}
		if (god) p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 1, true, false, true));
		if (gh) {
			if (!p.getAbilities().allowFlying) {
				p.getAbilities().allowFlying = true;
				p.getAbilities().setFlySpeed(0.12f);
				p.sendAbilitiesUpdate();
			}
			FLYERS.add(p.getUuid());
		} else if (FLYERS.remove(p.getUuid()) && !p.isCreative() && !p.isSpectator()) {
			p.getAbilities().allowFlying = false;
			p.getAbilities().flying = false;
			p.getAbilities().setFlySpeed(0.05f);
			p.sendAbilitiesUpdate();
		}
	}
}
