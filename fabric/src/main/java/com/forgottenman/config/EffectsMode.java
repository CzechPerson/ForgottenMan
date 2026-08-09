package com.forgottenman.config;

/**
 * Which rendering path the mod's effects take.
 *
 * The full path needs a stencil buffer and the mod's own core shaders. A shaderpack
 * takes both away: it draws into its own framebuffer, which has no stencil, and it
 * replaces the pipeline the core shaders expect.
 */
public enum EffectsMode {
    /** Full effects, falling back per effect when the framebuffer or a pack demands it. */
    AUTO,
    /** Always the full effects, even under a pack that will break them. */
    FULL,
    /** Always the shaderpack-safe path, pack or not. */
    COMPAT
}
