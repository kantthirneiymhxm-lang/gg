package com.kaiju.client;

import com.kaiju.KaijuMod;
import com.kaiju.entity.GhidorahEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class GhidorahModel extends SinglePartEntityModel<GhidorahEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(KaijuMod.id("king_ghidorah"), "main");
	private final ModelPart root, wingL, wingR, tail, neckL, neckR;

	public GhidorahModel(ModelPart root) {
		this.root = root;
		wingL = root.getChild("wingL");
		wingR = root.getChild("wingR");
		tail = root.getChild("body").getChild("tail");
		neckL = root.getChild("body").getChild("neckL");
		neckR = root.getChild("body").getChild("neckR");
	}

	private static void neck(ModelPartData body, String n, float x, float yaw, float len) {
		ModelPartData neck = body.addChild("neck" + n, ModelPartBuilder.create().uv(0, 0).cuboid(-2, -2, -len, 4, 4, len),
			ModelTransform.of(x, -2, -9, -0.6f, yaw, 0));
		neck.addChild("head" + n, ModelPartBuilder.create()
			.uv(0, 0).cuboid(-3.5f, -3.5f, -7, 7, 7, 7)
			.uv(0, 0).cuboid(-2, 0, -11, 4, 2.5f, 4)
			.uv(0, 80).cuboid(-3, -5.5f, -2, 1, 3, 1)
			.uv(0, 80).cuboid(2, -5.5f, -2, 1, 3, 1)
			.uv(64, 80).cuboid(-3.7f, -2, -6, 0.4f, 1, 1.5f)
			.uv(64, 80).cuboid(3.3f, -2, -6, 0.4f, 1, 1.5f), ModelTransform.of(0, 0, -len, 0.7f, 0, 0));
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData md = new ModelData();
		ModelPartData r = md.getRoot();
		ModelPartData body = r.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-5, -5, -10, 10, 10, 20),
			ModelTransform.pivot(0, 16, 0));
		neck(body, "C", 0, 0, 16);
		neck(body, "L", 4, -0.35f, 14);
		neck(body, "R", -4, 0.35f, 14);
		ModelPartData tl = body.addChild("tail", ModelPartBuilder.create().uv(0, 0).cuboid(-2, -2, 0, 4, 4, 16),
			ModelTransform.of(0, 0, 10, -0.1f, 0, 0));
		tl.addChild("tail2", ModelPartBuilder.create().uv(0, 0).cuboid(-1.2f, -1.2f, 0, 2.4f, 2.4f, 14)
			.uv(0, 80).cuboid(-1, -2.5f, 10, 2, 2, 4), ModelTransform.pivot(0, 0, 16));
		r.addChild("wingL", ModelPartBuilder.create().uv(0, 0).cuboid(0, -1.5f, -4, 30, 2, 3).uv(0, 0).cuboid(2, -1, -1, 28, 0.6f, 17),
			ModelTransform.of(5, 12, -2, 0, 0, 0));
		r.addChild("wingR", ModelPartBuilder.create().uv(0, 0).cuboid(-30, -1.5f, -4, 30, 2, 3).uv(0, 0).cuboid(-30, -1, -1, 28, 0.6f, 17),
			ModelTransform.of(-5, 12, -2, 0, 0, 0));
		return TexturedModelData.of(md, 128, 128);
	}

	@Override
	public void setAngles(GhidorahEntity e, float limbSwing, float limbAmt, float age, float headYaw, float headPitch) {
		float f = MathHelper.sin(age * 0.3f) * 0.6f - 0.1f;
		wingL.roll = f;
		wingR.roll = -f;
		tail.yaw = MathHelper.sin(age * 0.15f) * 0.25f;
		float g = e.getGrow();
		boolean show = g > 0.02f;
		neckL.visible = show;
		neckR.visible = show;
		neckL.xScale = neckL.yScale = neckL.zScale = g;
		neckR.xScale = neckR.yScale = neckR.zScale = g;
	}

	@Override public ModelPart getPart() { return root; }
}
