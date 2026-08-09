package com.forgottenman.mixin.client;

import com.forgottenman.compat.ShaderCompat;
import com.forgottenman.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The held item is lit from real block light, which the tree room has none of, so a
 * pack renders it black; without a pack forceBrightLightmap already brightens it and
 * this is a no-op. Forge cancels RenderHandEvent and reissues the render fullbright
 * through an access transformer; Fabric can simply rewrite the light argument on its
 * way into the same call. Matches both invocations, one per hand.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyArg(
            method = "renderHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            index = 9)
    private int forgottenman$fullbrightHeldItem(int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension() == ModDimensions.TREE_ROOM
                && ShaderCompat.useVanillaShaders()) {
            return LightTexture.FULL_BRIGHT;
        }
        return packedLight;
    }
}
