package com.kaiju.client;

import com.kaiju.KaijuMod;
import com.kaiju.entity.GodzillaEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class GodzillaModel extends SinglePartEntityModel<GodzillaEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(KaijuMod.id("godzilla"), "main");
	private final ModelPart root, head, jaw, tail1, tail2, tail3, legL, legR, armL, armR;

	public GodzillaModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		head = body.getChild("head");
		jaw = head.getChild("jaw");
		tail1 = body.getChild("tail1");
		tail2 = tail1.getChild("tail2");
		tail3 = tail2.getChild("tail3");
		armL = body.getChild("armL");
		armR = body.getChild("armR");
		legL = root.getChild("legL");
		legR = root.getChild("legR");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData md = new ModelData();
		ModelPartData r = md.getRoot();
		ModelPartData body = r.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-6, -20, -5, 12, 20, 10),
			ModelTransform.of(0, 10, 0, 0.35f, 0, 0));
		ModelPartData head = body.addChild("head", ModelPartBuilder.create()
			.uv(0, 0).cuboid(-4, -7, -6, 8, 7, 8)
			.uv(0, 0).cuboid(-3, -4, -13, 6, 3, 7), ModelTransform.of(0, -19, -3, -0.25f, 0, 0));
		head.addChild("jaw", ModelPartBuilder.create()
			.uv(0, 0).cuboid(-2.5f, 0, -10, 5, 2, 8)
			.uv(64, 96).cuboid(-2.5f, -0.8f, -9.5f, 0.8f, 1, 0.8f)
			.uv(64, 96).cuboid(1.7f, -0.8f, -9.5f, 0.8f, 1, 0.8f)
			.uv(64, 96).cuboid(-2.5f, -0.8f, -6.5f, 0.8f, 1, 0.8f)
			.uv(64, 96).cuboid(1.7f, -0.8f, -6.5f, 0.8f, 1, 0.8f), ModelTransform.pivot(0, -1.5f, -3));
		head.addChild("eyes", ModelPartBuilder.create()
			.uv(64, 80).cuboid(-4.2f, -5, -5.5f, 0.4f, 1.2f, 2)
			.uv(64, 80).cuboid(3.8f, -5, -5.5f, 0.4f, 1.2f, 2), ModelTransform.NONE);
		for (int i = 0; i < 5; i++) {
			body.addChild("spike" + i, ModelPartBuilder.create().uv(0, 80).cuboid(-1, -2.5f, 0, 2, 5, 4),
				ModelTransform.of(0, -17 + i * 3.5f, 5, 0.3f, 0, 0));
		}
		body.addChild("armL", ModelPartBuilder.create().uv(0, 0).cuboid(0, 0, -1.5f, 3, 9, 3), ModelTransform.of(6, -17, -3, -0.6f, 0, 0));
		body.addChild("armR", ModelPartBuilder.create().uv(0, 0).cuboid(-3, 0, -1.5f, 3, 9, 3), ModelTransform.of(-6, -17, -3, -0.6f, 0, 0));
		ModelPartData t1 = body.addChild("tail1", ModelPartBuilder.create().uv(0, 0).cuboid(-3.5f, -3.5f, 0, 7, 7, 10),
			ModelTransform.of(0, -3, 4, -0.6f, 0, 0));
		ModelPartData t2 = t1.addChild("tail2", ModelPartBuilder.create().uv(0, 0).cuboid(-2.5f, -2.5f, 0, 5, 5, 10),
			ModelTransform.of(0, 0, 10, -0.15f, 0, 0));
		t2.addChild("tail3", ModelPartBuilder.create().uv(0, 0).cuboid(-1.5f, -1.5f, 0, 3, 3, 10),
			ModelTransform.of(0, 0, 10, -0.1f, 0, 0));
		r.addChild("legR", ModelPartBuilder.create().uv(0, 0).cuboid(-3.5f, 0, -3.5f, 7, 14, 7).uv(0, 0).cuboid(-3.5f, 12, -7.5f, 7, 2, 4),
			ModelTransform.pivot(-4, 10, 0));
		r.addChild("legL", ModelPartBuilder.create().uv(0, 0).cuboid(-3.5f, 0, -3.5f, 7, 14, 7).uv(0, 0).cuboid(-3.5f, 12, -7.5f, 7, 2, 4),
			ModelTransform.pivot(4, 10, 0));
		return TexturedModelData.of(md, 128, 128);
	}

	@Override
	public void setAngles(GodzillaEntity e, float limbSwing, float limbAmt, float age, float headYaw, float headPitch) {
		float c = MathHelper.cos(limbSwing * 0.5f);
		legR.pitch = c * 1.0f * limbAmt;
		legL.pitch = -c * 1.0f * limbAmt;
		armR.pitch = -0.6f - c * 0.5f * limbAmt;
		armL.pitch = -0.6f + c * 0.5f * limbAmt;
		head.yaw = headYaw * 0.0105f;
		head.pitch = -0.25f + headPitch * 0.0175f;
		jaw.pitch = e.isBreathing() ? 0.9f : 0.05f + MathHelper.sin(age * 0.08f) * 0.03f;
		float sway = MathHelper.sin(age * 0.1f) * 0.12f + MathHelper.sin(limbSwing * 0.25f) * 0.3f * limbAmt;
		tail1.yaw = sway;
		tail2.yaw = sway * 1.3f;
		tail3.yaw = sway * 1.6f;
	}

	@Override public ModelPart getPart() { return root; }
}
