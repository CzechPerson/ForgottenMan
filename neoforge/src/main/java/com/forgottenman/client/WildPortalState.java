package com.forgottenman.client;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Client-side mirror of the doors the server says are wearing a portal */
public final class WildPortalState {
    private static volatile Set<BlockPos> armed = Collections.emptySet();

    public static void set(List<BlockPos> positions) {
        armed = positions.isEmpty() ? Collections.emptySet() : new HashSet<>(positions);
    }

    public static Set<BlockPos> armed() {
        return armed;
    }

    public static void clear() {
        armed = Collections.emptySet();
    }

    private WildPortalState() {
    }
}
