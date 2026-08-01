package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ForgottenMan.MOD_ID);

    /** The game's narration typer blip (snd_text) */
    public static final Supplier<SoundEvent> MAN_VOICE = SOUNDS.register("man_voice",
            () -> SoundEvent.createVariableRangeEvent(ForgottenMan.id("man_voice")));
    /** Egg room music, loops while in the dimension */
    public static final Supplier<SoundEvent> EGG_ROOM = SOUNDS.register("egg_room",
            () -> SoundEvent.createVariableRangeEvent(ForgottenMan.id("egg_room")));
    /** First mirror layer tearing off (snd_weirdeffect) */
    public static final Supplier<SoundEvent> REALITY_CRACK = SOUNDS.register("reality_crack",
            () -> SoundEvent.createVariableRangeEvent(ForgottenMan.id("reality_crack")));
    /** Reality expanding to infinity (snd_screenshake) */
    public static final Supplier<SoundEvent> REALITY_SHATTER = SOUNDS.register("reality_shatter",
            () -> SoundEvent.createVariableRangeEvent(ForgottenMan.id("reality_shatter")));
    /** The void claiming a door */
    public static final Supplier<SoundEvent> VOID_CLAIM = SOUNDS.register("void_claim",
            () -> SoundEvent.createVariableRangeEvent(ForgottenMan.id("void_claim")));
    /** Receiving the egg (snd_egg) */
    public static final Supplier<SoundEvent> EGG_GET = SOUNDS.register("egg_get",
            () -> SoundEvent.createVariableRangeEvent(ForgottenMan.id("egg_get")));

    private ModSounds() {
    }
}
