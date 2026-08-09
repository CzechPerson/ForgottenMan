package com.forgottenman.compat;

import com.forgottenman.config.ClientConfig;
import com.forgottenman.config.EffectsMode;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Decides whether the full effects can run, or whether to fall back.
 *
 * Two signals, answering different questions:
 *
 * The GL probe asks whether the framebuffer about to be drawn into actually has a
 * stencil attachment. That is the authoritative answer for the door portal, and it is
 * mod-agnostic -- it catches Iris, Oculus, OptiFine, Fabulous graphics (which binds
 * separate layer targets), and anything else that swaps the render target.
 *
 * Shaderpack detection asks whether the mod's own core shaders will be honoured. GL
 * cannot answer that: censor/room_copy/portal_static all compile and run under a pack,
 * they just write one colour into a gbuffer that the pack then relights from garbage.
 */
public final class ShaderCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Re-probe when any of these change, plus a coarse tick TTL in case a pack is
    // toggled without a reload. FBO ids get recycled, so the id alone is not a key.
    private static final int PROBE_TTL_TICKS = 200;
    // Explicit sentinel: (tick - Integer.MIN_VALUE) overflows negative and would read as
    // fresh forever, pinning the cached verdict at its default
    private static final int NEVER = Integer.MIN_VALUE;
    private static int generation;
    private static int probedFbo = -1;
    private static int probedWidth;
    private static int probedHeight;
    private static int probedGeneration = -1;
    private static int probedAtTick = NEVER;
    private static int tickCounter;
    private static boolean probedStencilUsable;
    private static boolean probeLogged;

    private static boolean irisResolved;
    private static Method irisIsShaderPackInUse;
    private static Method irisAreShadersEnabled;
    private static Object irisConfig;
    private static Method irisIsRenderingShadowPass;
    private static Object irisInstance;
    private static Method optifineIsShaders;

    private static final int PACK_TTL_TICKS = 20;
    private static boolean packActive;
    private static int packCheckedAtTick = NEVER;
    private static boolean packLoggedState;
    private static boolean packLoggedValue;

    /** Drive from the client tick. */
    public static void tick() {
        tickCounter++;
    }

    /** Drop every cached verdict: disconnect, resource reload, pack toggle. */
    public static void invalidate() {
        generation++;
        packCheckedAtTick = NEVER;
        probedAtTick = NEVER;
    }

    /**
     * Whether the door portal has to give up its stencil mask and fall back to a flat
     * aperture. Measured, not assumed: under Iris the draw framebuffer at
     * AFTER_BLOCK_ENTITIES is the main render target and the stencil is present, so a
     * shaderpack alone is NOT a reason to drop the real portal -- only a genuinely
     * stencil-less target is. That still covers Fabulous and target-swapping mods.
     */
    public static boolean portalUsesCompat() {
        boolean stencil = stencilAvailable(); // probed regardless of mode, for the log
        EffectsMode mode = ClientConfig.effectsMode();
        if (mode == EffectsMode.COMPAT) {
            return true;
        }
        if (mode == EffectsMode.FULL) {
            return false;
        }
        return !stencil;
    }

    /**
     * Whether to swap the mod's core shaders for vanilla ones. The geometry stays; only
     * the programs change, so a pack shades our draws with its own instead of dropping
     * them.
     */
    public static boolean useVanillaShaders() {
        return effectsUseCompat();
    }

    /**
     * Whether the effects that ride on custom core shaders -- the man, the mirror
     * copies, the post chain -- must use the fallback.
     *
     * Deliberately not the same question as the portal's. Fabulous graphics costs the
     * stencil but leaves core shaders working, so a single global switch would downgrade
     * these three for every Fabulous user with nothing gained.
     */
    public static boolean effectsUseCompat() {
        EffectsMode mode = ClientConfig.effectsMode();
        if (mode == EffectsMode.COMPAT) {
            return true;
        }
        if (mode == EffectsMode.FULL) {
            return false;
        }
        return shaderPackActive();
    }

    /** True while a pack is re-rendering the world into its shadow map. */
    public static boolean isShadowPass() {
        resolveIris();
        if (irisIsRenderingShadowPass == null || irisInstance == null) {
            return false;
        }
        try {
            return (Boolean) irisIsRenderingShadowPass.invoke(irisInstance);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Whether the framebuffer currently bound for drawing has a usable stencil. Only
     * meaningful inside a render stage; at tick time the main target is bound and this
     * would wrongly report yes.
     */
    public static boolean stencilAvailable() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getMainRenderTarget().isStencilEnabled() && probe(mc);
    }

    private static boolean probe(Minecraft mc) {
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int width = mc.getMainRenderTarget().viewWidth;
        int height = mc.getMainRenderTarget().viewHeight;
        boolean fresh = probedAtTick != NEVER && tickCounter - probedAtTick < PROBE_TTL_TICKS;
        if (fresh && drawFbo == probedFbo && width == probedWidth
                && height == probedHeight && generation == probedGeneration) {
            return probedStencilUsable;
        }

        boolean usable;
        if (drawFbo != mc.getMainRenderTarget().frameBufferId) {
            // Someone else's target. Even if it has a stencil, it is not the surface the
            // rest of the portal's passes assume.
            usable = false;
        } else if (drawFbo == 0) {
            usable = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_STENCIL, GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE) > 0;
        } else {
            // Query GL_STENCIL_ATTACHMENT, never GL_DEPTH_STENCIL_ATTACHMENT: the combined
            // point raises GL_INVALID_OPERATION when depth and stencil differ. A combined
            // DEPTH32F_STENCIL8 texture is defined to answer at both separate points.
            usable = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_STENCIL_ATTACHMENT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) != GL11.GL_NONE;
        }
        GL11.glGetError(); // never leave a probe error for another mod to trip over

        probedFbo = drawFbo;
        probedWidth = width;
        probedHeight = height;
        probedGeneration = generation;
        probedAtTick = tickCounter;
        probedStencilUsable = usable;
        if (!probeLogged) {
            probeLogged = true;
            LOGGER.info("Framebuffer probe: draw fbo {}, main fbo {}, stencil {}",
                    drawFbo, mc.getMainRenderTarget().frameBufferId,
                    usable ? "available" : "MISSING");
        }
        return usable;
    }

    private static boolean shaderPackActive() {
        if (packCheckedAtTick != NEVER && tickCounter - packCheckedAtTick < PACK_TTL_TICKS) {
            return packActive;
        }
        packCheckedAtTick = tickCounter;
        packActive = irisPackInUse() || optifinePackInUse();
        if (!packLoggedState || packLoggedValue != packActive) {
            packLoggedState = true;
            packLoggedValue = packActive;
            LOGGER.info("Shaderpack {} -- effects on the {} path",
                    packActive ? "active" : "inactive", packActive ? "compat" : "full");
        }
        return packActive;
    }

    private static boolean irisPackInUse() {
        resolveIris();
        if (irisIsShaderPackInUse == null || irisInstance == null) {
            return false;
        }
        try {
            if (!(Boolean) irisIsShaderPackInUse.invoke(irisInstance)) {
                return false;
            }
            // isShaderPackInUse reports a pack being *selected*, which stays true after
            // the user switches shaders off. areShadersEnabled is the one that tracks the
            // toggle, so AUTO must not fall back when a pack is merely selected.
            if (irisAreShadersEnabled != null && irisConfig != null) {
                return (Boolean) irisAreShadersEnabled.invoke(irisConfig);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // Reflective on purpose: the ecosystem splits per version. 1.21.1 uses Iris on both
    // loaders; Forge 1.20.1 has no Iris at all and runs the Oculus fork or OptiFine. A
    // compile dependency would need a Maven repo this project does not have, and would
    // pin one fork's package name.
    private static void resolveIris() {
        if (irisResolved) {
            return;
        }
        irisResolved = true;
        if (!ModList.get().isLoaded("iris") && !ModList.get().isLoaded("oculus")) {
            return;
        }
        for (String name : new String[]{
                "net.irisshaders.iris.api.v0.IrisApi",  // Iris 1.6+, and modern Oculus
                "net.coderbot.iris.api.v0.IrisApi"}) {  // Iris 1.5 and older Oculus
            try {
                Class<?> api = Class.forName(name);
                irisInstance = api.getMethod("getInstance").invoke(null);
                irisIsShaderPackInUse = api.getMethod("isShaderPackInUse");
                irisIsRenderingShadowPass = api.getMethod("isRenderingShadowPass");
                try {
                    irisConfig = api.getMethod("getConfig").invoke(irisInstance);
                    irisAreShadersEnabled = irisConfig.getClass().getMethod("areShadersEnabled");
                    irisAreShadersEnabled.setAccessible(true);
                } catch (Throwable ignored) {
                    irisConfig = null; // older API, fall back to isShaderPackInUse alone
                }
                LOGGER.debug("Resolved shader API {}", name);
                return;
            } catch (Throwable ignored) {
                // Try the next candidate; absence is the normal case.
            }
        }
    }

    private static boolean optifinePackInUse() {
        if (optifineIsShaders == null) {
            try {
                optifineIsShaders = Class.forName("net.optifine.Config").getMethod("isShaders");
            } catch (Throwable ignored) {
                return false; // OptiFine absent, which is the norm outside 1.20.1 Forge
            }
        }
        try {
            return (Boolean) optifineIsShaders.invoke(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ShaderCompat() {
    }
}
