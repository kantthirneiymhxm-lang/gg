package com.kaiju;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import java.util.function.Supplier;

public enum KaijuArmor implements ArmorMaterial {
	GODZILLA("kaiju_godzilla", () -> Ingredient.ofItems(KaijuMod.GODZILLA_CELL)),
	GHIDORAH("kaiju_ghidorah", () -> Ingredient.ofItems(KaijuMod.GHIDORAH_CELL));

	private static final int[] BASE = {13, 15, 16, 11};
	private static final int[] PROT = {4, 8, 10, 5};
	private final String name;
	private final Supplier<Ingredient> repair;

	KaijuArmor(String name, Supplier<Ingredient> repair) { this.name = name; this.repair = repair; }

	@Override public int getDurability(ArmorItem.Type type) { return BASE[type.getEquipmentSlot().getEntitySlotId()] * 60; }
	@Override public int getProtection(ArmorItem.Type type) { return PROT[type.getEquipmentSlot().getEntitySlotId()]; }
	@Override public int getEnchantability() { return 20; }
	@Override public SoundEvent getEquipSound() { return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE; }
	@Override public Ingredient getRepairIngredient() { return repair.get(); }
	@Override public String getName() { return name; }
	@Override public float getToughness() { return 5f; }
	@Override public float getKnockbackResistance() { return 0.2f; }
}
