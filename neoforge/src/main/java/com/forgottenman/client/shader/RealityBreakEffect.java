package com.forgottenman.client.shader;

import com.forgottenman.ForgottenMan;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModDimensions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

/**
 * Full-screen post pass
 */
@EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class RealityBreakEffect {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CHAIN_ID = ForgottenMan.id("shaders/post/reality_break.json");

    private static PostChain chain;
    private static boolean loadFailed;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    // AFTER_LEVEL draw order is otherwise undefined: and the post chain last, over everything
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != ModDimensions.TREE_ROOM || loadFailed
                || RealityState.getMirrorLevel() < 2) {
            return; // The dizziness sets in once the copies recede to infinity
        }
        // Runs under a pack too: by AFTER_LEVEL the pack has composited into the main
        // render target, which is exactly what the chain reads and writes.
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
        // Frame delta in ticks, not the partial tick, or the heartbeat speeds up with FPS
        chain.process(mc.getTimer().getGameTimeDeltaTicks());
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
