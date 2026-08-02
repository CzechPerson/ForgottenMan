package com.forgottenman.client;

import com.forgottenman.ForgottenMan;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** Camera shake: layered sine noise on pitch/yaw/roll, decaying over the duration */
@Mod.EventBusSubscriber(modid = ForgottenMan.MOD_ID, value = Dist.CLIENT)
public final class ScreenShake {
    private static int duration;
    private static int ticksLeft;
    private static float intensity;

    // Degrees = peak angular amplitude
    public static void start(int durationTicks, float degrees) {
        duration = durationTicks;
        ticksLeft = durationTicks;
        intensity = degrees;
    }

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksLeft > 0) {
            ticksLeft--;
        }
    }

    @SubscribeEvent
    static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ticksLeft <= 0 || duration <= 0) {
            return;
        }
        float remaining = ticksLeft - (float) event.getPartialTick();
        float fade = Math.max(0.0F, remaining / duration);
        float t = (duration - remaining) * 0.9F;
        float strength = intensity * fade * fade; // Quick falloff, violent start
        event.setPitch(event.getPitch() + strength * noise(t * 2.1F));
        event.setYaw(event.getYaw() + strength * noise(t * 1.7F + 13.0F));
        event.setRoll(event.getRoll() + strength * 0.5F * noise(t * 2.7F + 71.0F));
    }

    // Cheap aperiodic wobble in [-1, 1]
    private static float noise(float x) {
        return (float) (Math.sin(x) * 0.6 + Math.sin(x * 2.33 + 1.7) * 0.4);
    }

    private ScreenShake() {
    }
}
