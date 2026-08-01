package com.forgottenman.client;

import com.forgottenman.ForgottenMan;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Sky effects for the tree room
 */
public class TreeRoomSpecialEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = ForgottenMan.id("tree_room");

    public TreeRoomSpecialEffects() {
        // forceBrightLightmap keeps the room readable against the void and matches
        // the fullbright-baked mirror copies
        super(Float.NaN, false, SkyType.NONE, true, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO; // Pure #000000, always
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    // Suppressing the sky, clouds and weather is done by overriding these on NeoForge.
    // Vanilla has no such hooks, so on Fabric empty sky/cloud/weather renderers are
    // registered against the dimension in ForgottenManClient instead.
}
