package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public final class ModSounds {
    /** The game's narration typer blip (snd_text) */
    public static final Supplier<SoundEvent> MAN_VOICE = register("man_voice");
    /** Egg room music, loops while in the dimension */
    public static final Supplier<SoundEvent> EGG_ROOM = register("egg_room");
    /** First mirror layer tearing off (snd_weirdeffect) */
    public static final Supplier<SoundEvent> REALITY_CRACK = register("reality_crack");
    /** Reality expanding to infinity (snd_screenshake) */
    public static final Supplier<SoundEvent> REALITY_SHATTER = register("reality_shatter");
    /** The void claiming a door */
    public static final Supplier<SoundEvent> VOID_CLAIM = register("void_claim");
    /** Receiving the egg (snd_egg) */
    public static final Supplier<SoundEvent> EGG_GET = register("egg_get");

    private static Supplier<SoundEvent> register(String name) {
        SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, ForgottenMan.id(name),
                SoundEvent.createVariableRangeEvent(ForgottenMan.id(name)));
        return () -> event;
    }

    public static void register() {
    }

    private ModSounds() {
    }
}
