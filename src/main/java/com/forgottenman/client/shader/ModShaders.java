package com.forgottenman.client.shader;

import com.forgottenman.ForgottenMan;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Core shaders:
 * - "censor": the man's procedural SDF silhouette
 * - "room_copy": the baked room mesh for mirror copies and door portals; vertex
 *   wobble/desync, distance fade to black
 * - "portal_static": dark red static for tree room doorways
 * - "portal_overlay": scanline shimmer over the door portals
 */
public final class ModShaders {
    private static ShaderInstance censorShader;
    private static ShaderInstance roomCopyShader;
    private static ShaderInstance portalStaticShader;
    private static ShaderInstance portalOverlayShader;

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(ForgottenMan.id("censor"), DefaultVertexFormat.POSITION_TEX,
                    shader -> censorShader = shader);
            context.register(ForgottenMan.id("room_copy"), DefaultVertexFormat.BLOCK,
                    shader -> roomCopyShader = shader);
            context.register(ForgottenMan.id("portal_static"), DefaultVertexFormat.POSITION,
                    shader -> portalStaticShader = shader);
            context.register(ForgottenMan.id("portal_overlay"), DefaultVertexFormat.POSITION,
                    shader -> portalOverlayShader = shader);
        });
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
