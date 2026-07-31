package com.forgottenman.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Camera shake: layered sine noise on pitch/yaw, decaying over the duration. Applied
 * by CameraMixin once the camera has been set up.
 *
 * NeoForge's ViewportEvent.ComputeCameraAngles also carried a roll term, which vanilla
 * Camera#setRotation has no parameter for (NeoForge patches a three-argument overload
 * in). Roll is dropped here rather than faked with a pose-stack rotation.
 */
public final class ScreenShake {
    private static int duration;
    private static int ticksLeft;
    private static float intensity;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksLeft > 0) {
                ticksLeft--;
            }
        });
    }

    // Degrees = peak angular amplitude
    public static void start(int durationTicks, float degrees) {
        duration = durationTicks;
        ticksLeft = durationTicks;
        intensity = degrees;
    }

    public static boolean active() {
        return ticksLeft > 0 && duration > 0;
    }

    public static float pitchOffset(float partialTick) {
        return strength(partialTick) * noise(phase(partialTick) * 2.1F);
    }

    public static float yawOffset(float partialTick) {
        return strength(partialTick) * noise(phase(partialTick) * 1.7F + 13.0F);
    }

    private static float remaining(float partialTick) {
        return ticksLeft - partialTick;
    }

    private static float strength(float partialTick) {
        float fade = Math.max(0.0F, remaining(partialTick) / duration);
        return intensity * fade * fade; // Quick falloff, violent start
    }

    private static float phase(float partialTick) {
        return (duration - remaining(partialTick)) * 0.9F;
    }

    // Cheap aperiodic wobble in [-1, 1]
    private static float noise(float x) {
        return (float) (Math.sin(x) * 0.6 + Math.sin(x * 2.33 + 1.7) * 0.4);
    }

    private ScreenShake() {
    }
}
