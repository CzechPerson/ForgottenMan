package com.forgottenman.client.shader;

import com.forgottenman.ForgottenMan;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * Core shaders:
 * - "censor": the man's procedural SDF silhouette
 * - "room_copy": the baked room mesh for mirror copies and door portals; vertex
 *   wobble/desync, distance fade to black
 * - "portal_static": dark red static for tree room doorways
 * - "portal_overlay": scanline shimmer over the door portals
 */
@Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModShaders {
    private static ShaderInstance censorShader;
    private static ShaderInstance roomCopyShader;
    private static ShaderInstance portalStaticShader;
    private static ShaderInstance portalOverlayShader;

    @SubscribeEvent
    static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ForgottenMan.id("censor"), DefaultVertexFormat.POSITION_TEX),
                shader -> censorShader = shader);
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ForgottenMan.id("room_copy"), DefaultVertexFormat.BLOCK),
                shader -> roomCopyShader = shader);
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ForgottenMan.id("portal_static"), DefaultVertexFormat.POSITION),
                shader -> portalStaticShader = shader);
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ForgottenMan.id("portal_overlay"), DefaultVertexFormat.POSITION),
                shader -> portalOverlayShader = shader);
    }

    public static ShaderInstance getCensorShader() {
        return censorShader;
    }

    public static ShaderInstance getRoomCopyShader() {
        return roomCopyShader;
    }

    public static ShaderInstance getPortalStaticShader() {
        return portalStaticShader;
    }

    public static ShaderInstance getPortalOverlayShader() {
        return portalOverlayShader;
    }

    private ModShaders() {
    }
}
