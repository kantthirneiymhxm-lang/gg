package com.kaiju.client;

import com.kaiju.KaijuMod;
import com.kaiju.entity.GodzillaEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GodzillaRenderer extends MobEntityRenderer<GodzillaEntity, GodzillaModel> {
	private static final Identifier TEX = KaijuMod.id("textures/entity/godzilla.png");

	public GodzillaRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new GodzillaModel(ctx.getPart(GodzillaModel.LAYER)), 30f);
	}

	@Override protected void scale(GodzillaEntity e, MatrixStack m, float t) { m.scale(36f, 36f, 36f); }
	@Override public Identifier getTexture(GodzillaEntity e) { return TEX; }

	@Override public boolean shouldRender(GodzillaEntity e, Frustum f, double x, double y, double z) { return true; }
}
