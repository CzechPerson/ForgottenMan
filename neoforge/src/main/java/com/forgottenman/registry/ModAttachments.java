package com.forgottenman.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Per-player state. 1.20.1 has no attachment API, so this lives in the player's
 * PlayerPersisted tag -- the one subtree Forge carries across a respawn, which is
 * the copyOnDeath behaviour the room relies on.
 */
public final class ModAttachments {
    private static final String ENTRY_DOOR = "entry_door";
    private static final String ENTRY_DIMENSION = "entry_door_dimension";
    private static final String MET_MAN = "met_man";

    /** Whether this player has talked to the man behind the tree */
    public static boolean hasMetMan(Player player) {
        return data(player).getBoolean(MET_MAN);
    }

    public static void setMetMan(Player player, boolean met) {
        data(player).putBoolean(MET_MAN, met);
    }

    /** The door the player last entered the tree room through */
    public static boolean hasEntryDoor(Player player) {
        return data(player).contains(ENTRY_DOOR);
    }

    @Nullable
    public static GlobalPos getEntryDoor(Player player) {
        CompoundTag tag = data(player);
        if (!tag.contains(ENTRY_DOOR)) {
            return null;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(ENTRY_DIMENSION));
        if (dimension == null) {
            return null;
        }
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension),
                BlockPos.of(tag.getLong(ENTRY_DOOR)));
    }

    public static void setEntryDoor(Player player, GlobalPos pos) {
        CompoundTag tag = data(player);
        tag.putLong(ENTRY_DOOR, pos.pos().asLong());
        tag.putString(ENTRY_DIMENSION, pos.dimension().location().toString());
    }

    public static void clearEntryDoor(Player player) {
        CompoundTag tag = data(player);
        tag.remove(ENTRY_DOOR);
        tag.remove(ENTRY_DIMENSION);
    }

    // getCompound hands back a fresh tag when the key is missing, so the subtag has
    // to be put in place first or every write would land on a throwaway copy
    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private ModAttachments() {
    }
}
