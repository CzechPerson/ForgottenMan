package com.forgottenman.registry;

import com.forgottenman.ForgottenMan;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/** The dimension is data-driven, see data/forgottenman/dimension(_type)/tree_room.json */
public final class ModDimensions {
    public static final ResourceKey<Level> TREE_ROOM =
            ResourceKey.create(Registries.DIMENSION, ForgottenMan.id("tree_room"));
    public static final ResourceKey<DimensionType> TREE_ROOM_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, ForgottenMan.id("tree_room"));

    private ModDimensions() {
    }
}
