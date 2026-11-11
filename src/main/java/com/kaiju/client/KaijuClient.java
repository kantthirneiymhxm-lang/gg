package com.kaiju.client;

import com.kaiju.KaijuMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

public class KaijuClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(GodzillaModel.LAYER, GodzillaModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(GhidorahModel.LAYER, GhidorahModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(HeadGear.GODZILLA, HeadGear::godzilla);
		EntityModelLayerRegistry.registerModelLayer(HeadGear.GHIDORAH, HeadGear::ghidorah);
		EntityRendererRegistry.register(KaijuMod.GODZILLA, GodzillaRenderer::new);
		EntityRendererRegistry.register(KaijuMod.GHIDORAH, GhidorahRenderer::new);

		compass(KaijuMod.GODZILLA_COMPASS);
		compass(KaijuMod.GHIDORAH_COMPASS);

		head(KaijuMod.GODZILLA_HELMET, HeadGear.GODZILLA, KaijuMod.id("textures/entity/godzilla.png"));
		head(KaijuMod.GHIDORAH_HELMET, HeadGear.GHIDORAH, KaijuMod.id("textures/entity/king_ghidorah.png"));
	}

	private static void compass(Item item) {
		ModelPredicateProviderRegistry.register(item, new Identifier("angle"), new CompassAnglePredicateProvider((world, stack, entity) -> {
			NbtCompound n = stack.getNbt();
			if (n == null || !n.getBoolean("Has")) return null;
			return GlobalPos.create(world.getRegistryKey(), new BlockPos(n.getInt("TX"), 64, n.getInt("TZ")));
		}));
	}

	private static void head(Item helmet, EntityModelLayer layer, Identifier tex) {
		ArmorRenderer.register((matrices, vcp, stack, entity, slot, light, model) -> {
			ModelPart part = MinecraftClient.getInstance().getEntityModelLoader().getModelPart(layer);
			matrices.push();
			model.head.rotate(matrices);
			VertexConsumer vc = ItemRenderer.getArmorGlintConsumer(vcp, RenderLayer.getArmorCutoutNoCull(tex), false, stack.hasGlint());
			part.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
			matrices.pop();
		}, helmet);
	}
}
