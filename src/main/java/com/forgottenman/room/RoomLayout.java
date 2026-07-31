package com.forgottenman.room;

import com.forgottenman.entity.ManEntity;
import com.forgottenman.registry.ModAttachments;
import com.forgottenman.registry.ModBlocks;
import com.forgottenman.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tree room, defined in code so the same block data drives both server placement
 * and client mesh baking (door portals + mirror copies). Local coords with (0,0,0) at
 * the north-west corner, anchored at {@link #ORIGIN} in the dimension.
 */
public final class RoomLayout {
    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    public static final int SIZE_X = 21;
    public static final int SIZE_Y = 14;
    public static final int SIZE_Z = 21;
    public static final BlockPos CENTER_LOCAL = new BlockPos(10, 0, 7);

    public static final BlockPos RETURN_DOOR_LOCAL = new BlockPos(10, 1, 19);
    public static final Direction RETURN_DOOR_FACING = Direction.SOUTH;
    /** Arrival spot for players coming through a door: on the path, facing the tree. */
    public static final BlockPos ARRIVAL_LOCAL = new BlockPos(10, 1, 16);
    public static final float ARRIVAL_YAW = 180.0F; // north, toward the tree
    /** The man's spot: north of the 2x2 trunk, hidden from the entrance. */
    public static final BlockPos MAN_LOCAL = new BlockPos(10, 1, 3);

    // North-west corner of the 2x2 trunk core
    private static final int TREE_X = 9;
    private static final int TREE_Z = 5;

    private static Map<BlockPos, BlockState> blocks;

    /** Immutable local-pos to state map of the whole room. Built on demand from either the server or render thread. */
    public static synchronized Map<BlockPos, BlockState> blocks() {
        if (blocks == null) {
            blocks = Collections.unmodifiableMap(build());
        }
        return blocks;
    }

    private static Map<BlockPos, BlockState> build() {
        Map<BlockPos, BlockState> map = new LinkedHashMap<>();

        // Flat plum cross at y=0: wide platform under the tree, narrow path south to the door.
        BlockState plum = ModBlocks.PLUM_GROUND.get().defaultBlockState();
        for (int x = 3; x <= 17; x++) {
            for (int z = 2; z <= 12; z++) {
                map.put(new BlockPos(x, 0, z), plum);
            }
        }
        for (int x = 9; x <= 11; x++) {
            for (int z = 13; z <= 19; z++) {
                map.put(new BlockPos(x, 0, z), plum);
            }
        }

        // Tree crown
        addBlob(map, 10.5, 9.2, 6.0, 6.8, 3.4, 4.6);  // Main mass
        addBlob(map, 6.0, 7.0, 6.5, 3.2, 2.4, 2.6);   // Hanging blob on the left
        addBlob(map, 14.5, 10.0, 5.5, 2.6, 2.2, 2.4); // Upper bump on the right

        // Trunk
        for (int[] p : new int[][]{{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}, {-1, 2}, {2, -1}}) {
            map.put(new BlockPos(TREE_X + p[0], 1, TREE_Z + p[1]), barkFor(1));
        }
        // 2x2 core, y 1..3
        for (int y = 1; y <= 3; y++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    map.put(new BlockPos(TREE_X + dx, y, TREE_Z + dz), barkFor(y));
                }
            }
        }
        // lean right: 2x2 shifted +x at y4, tapering upward into the crown
        for (int dx = 1; dx <= 2; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                map.put(new BlockPos(TREE_X + dx, 4, TREE_Z + dz), barkFor(4));
            }
        }
        map.put(new BlockPos(TREE_X + 2, 5, TREE_Z), barkFor(5));
        map.put(new BlockPos(TREE_X + 2, 5, TREE_Z + 1), barkFor(5));
        map.put(new BlockPos(TREE_X + 2, 6, TREE_Z + 1), barkFor(6));
        // left arm
        map.put(new BlockPos(TREE_X - 1, 4, TREE_Z), barkFor(4));
        map.put(new BlockPos(TREE_X - 2, 5, TREE_Z - 1), barkFor(5));
        map.put(new BlockPos(TREE_X - 3, 6, TREE_Z - 1), barkFor(6));
        // One bare branch poking out of the crown to the right
        map.put(new BlockPos(16, 9, 6), barkFor(9));
        map.put(new BlockPos(17, 9, 6), barkFor(9));

        // Grass tufts on free floor tiles. Deterministic hash so the baked mirror copies match
        BlockState grass = ModBlocks.PLUM_GRASS.get().defaultBlockState();
        for (int x = 3; x <= 17; x++) {
            for (int z = 2; z <= 17; z++) {
                BlockPos spot = new BlockPos(x, 1, z);
                if (!plum.equals(map.get(new BlockPos(x, 0, z))) || map.containsKey(spot)
                        || spot.equals(MAN_LOCAL) || spot.equals(ARRIVAL_LOCAL)) {
                    continue;
                }
                if (hash(x * 5 + 3, z * 7 + 1) % 100 < 15) {
                    map.put(spot, grass);
                }
            }
        }

        // Return door at the south end of the path.
        BlockState doorLower = ModBlocks.MYSTERIOUS_DOOR.get().defaultBlockState()
                .setValue(DoorBlock.FACING, RETURN_DOOR_FACING)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        map.put(RETURN_DOOR_LOCAL, doorLower);
        map.put(RETURN_DOOR_LOCAL.above(), doorLower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // Invisible void walls
        BlockState wall = ModBlocks.VOID_WALL.get().defaultBlockState();
        for (int x = -2; x <= SIZE_X + 1; x++) {
            for (int z = -2; z <= SIZE_Z + 1; z++) {
                boolean walkable = map.containsKey(new BlockPos(x, 0, z));
                for (int y = -1; y <= SIZE_Y; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (map.containsKey(pos)) {
                        continue;
                    }
                    if (walkable && y >= 1) {
                        continue; // open air above the floor
                    }
                    map.put(pos, wall);
                }
            }
        }

        return map;
    }

    // Bark shade by height
    private static BlockState barkFor(int y) {
        if (y <= 1) {
            return ModBlocks.VOID_BARK.get().defaultBlockState();
        }
        if (y == 2) {
            return ModBlocks.VOID_BARK_1.get().defaultBlockState();
        }
        return ModBlocks.VOID_BARK_2.get().defaultBlockState();
    }

    // Solid ellipsoid of crown blocks
    private static void addBlob(Map<BlockPos, BlockState> map, double cx, double cy, double cz,
                                double rx, double ry, double rz) {
        for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++) {
            for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++) {
                for (int z = (int) Math.floor(cz - rz); z <= (int) Math.ceil(cz + rz); z++) {
                    double ex = (x - cx) / rx;
                    double ey = (y - cy) / ry;
                    double ez = (z - cz) / rz;
                    if (ex * ex + ey * ey + ez * ez <= 1.0) {
                        map.put(new BlockPos(x, y, z), crownBlock(x, y, z));
                    }
                }
            }
        }
    }

    // ~60% scarlet, ~25% bright magenta, ~15% deep magenta, in chunky 2x2x2 patches
    private static BlockState crownBlock(int x, int y, int z) {
        int h = hash((x >> 1) * 31 + (y >> 1), (z >> 1) * 17 - (y >> 1)) % 100;
        if (h < 60) {
            return ModBlocks.SCARLET_LEAVES.get().defaultBlockState();
        }
        if (h < 85) {
            return ModBlocks.MAGENTA_LEAVES.get().defaultBlockState();
        }
        return ModBlocks.DEEP_MAGENTA_LEAVES.get().defaultBlockState();
    }

    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        return h & 0x7fffffff;
    }

    public static boolean isPlaced(ServerLevel level) {
        // Checks a block unique to the current layout so rooms built by older versions
        // get rebuilt on next entry. Re-point this whenever the layout changes.
        return level.getBlockState(ORIGIN.offset(TREE_X, 3, TREE_Z)).is(ModBlocks.VOID_BARK_2.get());
    }

    /** Spawns the man behind the tree */
    public static void ensureManPresent(ServerLevel level, ServerPlayer player) {
        if (ModAttachments.hasMetMan(player)) {
            return; // He has nothing more for you, for now
        }
        AABB roomBox = new AABB(ORIGIN.getX(), ORIGIN.getY(), ORIGIN.getZ(),
                ORIGIN.getX() + SIZE_X, ORIGIN.getY() + SIZE_Y, ORIGIN.getZ() + SIZE_Z);
        if (!level.getEntitiesOfClass(ManEntity.class, roomBox).isEmpty()) {
            return;
        }
        ManEntity man = ModEntities.MAN.get().create(level);
        if (man == null) {
            return;
        }
        BlockPos pos = ORIGIN.offset(MAN_LOCAL);
        man.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(man);
    }

    public static void place(ServerLevel level) {
        Map<BlockPos, BlockState> layout = blocks();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos local = new BlockPos.MutableBlockPos();
        // Sweep the whole box so stale blocks from older layouts get cleared; flag 2
        // (no neighbor updates) keeps the door halves from popping off mid-placement
        for (int x = -2; x <= SIZE_X + 1; x++) {
            for (int z = -2; z <= SIZE_Z + 1; z++) {
                for (int y = -1; y <= SIZE_Y; y++) {
                    local.set(x, y, z);
                    BlockState desired = layout.getOrDefault(local, air);
                    BlockPos world = ORIGIN.offset(local);
                    if (!level.getBlockState(world).equals(desired)) {
                        level.setBlock(world, desired, 2);
                    }
                }
            }
        }
    }

    private RoomLayout() {
    }
}
