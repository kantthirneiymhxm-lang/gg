package com.kaiju.client;

import com.kaiju.KaijuMod;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/** The Godzilla / King Ghidorah head worn on the player's head. */
public final class HeadGear {
	public static final EntityModelLayer GODZILLA = new EntityModelLayer(KaijuMod.id("godzilla_head"), "main");
	public static final EntityModelLayer GHIDORAH = new EntityModelLayer(KaijuMod.id("ghidorah_head"), "main");

	public static TexturedModelData godzilla() {
		ModelData md = new ModelData();
		ModelPartBuilder b = ModelPartBuilder.create()
			.uv(0, 0).cuboid(-4.6f, -9, -4.6f, 9.2f, 9, 9.2f)
			.uv(0, 0).cuboid(-3.5f, -5, -12.5f, 7, 3.5f, 8)
			.uv(0, 0).cuboid(-3f, -1.5f, -12, 6, 1.5f, 8)
			.uv(64, 80).cuboid(-5f, -6, -3.5f, 0.6f, 1.4f, 2.5f)
			.uv(64, 80).cuboid(4.4f, -6, -3.5f, 0.6f, 1.4f, 2.5f)
			.uv(0, 80).cuboid(-0.8f, -12.5f, -3, 1.6f, 4, 2.5f)
			.uv(0, 80).cuboid(-0.8f, -13.5f, 0, 1.6f, 5, 2.5f)
			.uv(0, 80).cuboid(-0.8f, -12f, 3, 1.6f, 4, 2.5f);
		for (int i = 0; i < 4; i++) {
			float z = -12f + i * 2f;
			b.uv(64, 96).cuboid(-2.8f, -2.4f, z, 0.6f, 1, 0.6f);
			b.uv(64, 96).cuboid(2.2f, -2.4f, z, 0.6f, 1, 0.6f);
		}
		md.getRoot().addChild("head", b, ModelTransform.NONE);
		return TexturedModelData.of(md, 128, 128);
	}

	public static TexturedModelData ghidorah() {
		ModelData md = new ModelData();
		ModelPartBuilder b = ModelPartBuilder.create()
			.uv(0, 0).cuboid(-4.6f, -9, -4.6f, 9.2f, 9, 9.2f)
			.uv(0, 0).cuboid(-2.5f, -4.5f, -11, 5, 3, 6.5f)
			.uv(0, 80).cuboid(-4.5f, -13, -1, 1, 5, 1)
			.uv(0, 80).cuboid(3.5f, -13, -1, 1, 5, 1)
			.uv(0, 80).cuboid(-0.5f, -12, 0, 1, 3, 3)
			.uv(64, 80).cuboid(-3.4f, -6, -5.2f, 1.6f, 1.2f, 0.8f)
			.uv(64, 80).cuboid(1.8f, -6, -5.2f, 1.6f, 1.2f, 0.8f);
		// two extra heads on long necks
		for (int s = -1; s <= 1; s += 2) {
			float x = s * 10f;
			b.uv(0, 0).cuboid(x - 1.5f, -20, -1.5f, 3, 14, 3);
			b.uv(0, 0).cuboid(x - 2.5f, -24, -6, 5, 5, 7);
			b.uv(0, 0).cuboid(x - 1.5f, -22, -9.5f, 3, 2, 4);
			b.uv(0, 80).cuboid(x - 2.2f, -27, -2, 0.8f, 3.5f, 0.8f);
			b.uv(0, 80).cuboid(x + 1.4f, -27, -2, 0.8f, 3.5f, 0.8f);
			b.uv(64, 80).cuboid(x - 2.7f, -23, -5, 0.4f, 1, 1.4f);
			b.uv(64, 80).cuboid(x + 2.3f, -23, -5, 0.4f, 1, 1.4f);
		}
		md.getRoot().addChild("head", b, ModelTransform.NONE);
		return TexturedModelData.of(md, 128, 128);
	}
}
