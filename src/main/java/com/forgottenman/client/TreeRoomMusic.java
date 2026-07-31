package com.forgottenman.client;

import com.forgottenman.registry.ModDimensions;
import com.forgottenman.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Egg room music, loops while in the dimension */
public class TreeRoomMusic extends AbstractTickableSoundInstance {
    public TreeRoomMusic() {
        super(ModSounds.EGG_ROOM.get(), SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.6F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != ModDimensions.TREE_ROOM) {
            this.stop();
        }
    }
}
