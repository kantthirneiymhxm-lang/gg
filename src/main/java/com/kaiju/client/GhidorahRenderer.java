package com.kaiju.client;

import com.kaiju.KaijuMod;
import com.kaiju.entity.GhidorahEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GhidorahRenderer extends MobEntityRenderer<GhidorahEntity, GhidorahModel> {
	private static final Identifier TEX = KaijuMod.id("textures/entity/king_ghidorah.png");

	public GhidorahRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new GhidorahModel(ctx.getPart(GhidorahModel.LAYER)), 32f);
	}

	@Override protected void scale(GhidorahEntity e, MatrixStack m, float t) { m.scale(100f, 100f, 100f); }
	@Override public Identifier getTexture(GhidorahEntity e) { return TEX; }

	@Override public boolean shouldRender(GhidorahEntity e, Frustum f, double x, double y, double z) { return true; }
}
