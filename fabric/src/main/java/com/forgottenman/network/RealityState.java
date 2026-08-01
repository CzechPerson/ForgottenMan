package com.forgottenman.network;

/**
 * Client-side mirror level: 0 intact, 1 one ring of copies, 2 the full hall.
 * No client-only classes on purpose, so common networking code can reference it
 * on a dedicated server.
 */
public final class RealityState {
    private static volatile int mirrorLevel;

    public static int getMirrorLevel() {
        return mirrorLevel;
    }

    public static void setMirrorLevel(int level) {
        mirrorLevel = level;
    }

    private RealityState() {
    }
}
