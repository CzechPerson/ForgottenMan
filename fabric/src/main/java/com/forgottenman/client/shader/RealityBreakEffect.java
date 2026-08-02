package com.forgottenman.client.shader;

import com.forgottenman.ForgottenMan;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModDimensions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Full-screen post pass
 */
public final class RealityBreakEffect {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CHAIN_ID = ForgottenMan.id("shaders/post/reality_break.json");

    private static PostChain chain;
    private static boolean loadFailed;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    /** Runs after the level is drawn, Fabric's closest stage to NeoForge's AFTER_LEVEL */
    public static void renderAfterLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != ModDimensions.TREE_ROOM || loadFailed
                || RealityState.getMirrorLevel() < 2) {
            return; // The dizziness sets in once the copies recede to infinity
        }
        if (chain == null && !load(mc)) {
            return;
        }
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (width != lastWidth || height != lastHeight) {
            chain.resize(width, height);
            lastWidth = width;
            lastHeight = height;
        }
        // 1.20.1's PostChain treats this as a timestamp, not a delta: it keeps the
        // previous value and advances its clock by the difference, rolling over when
        // the partial tick wraps at a tick boundary. Feeding it a frame delta makes
        // every shrinking frame look like a wrap and jump the clock a whole tick, so
        // the heartbeat races and scales with FPS. (1.21.1 wants the delta instead.)
        chain.process(mc.getFrameTime());
    }

    private static boolean load(Minecraft mc) {
        try {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN_ID);
            chain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            lastWidth = mc.getWindow().getWidth();
            lastHeight = mc.getWindow().getHeight();
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load reality-break post chain", e);
            loadFailed = true;
            chain = null;
            return false;
        }
    }

    private RealityBreakEffect() {
    }
}
