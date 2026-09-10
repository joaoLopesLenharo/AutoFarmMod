package com.autofarm.mods.systems;

import com.autofarm.mods.AutoFarmRegistry;
import com.autofarm.mods.components.TilledByFarmComponent;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.joml.Vector3i;

import java.util.Locale;
import java.util.UUID;

public class TerraformSystem {

    public static final String TILLED_SOIL_BLOCK = "Soil_Dirt_Tilled";

    /**
     * Checks whether water (fluid or water block) is within maxProximity blocks horizontally/vertically.
     */
    public static boolean isNearWater(World world, int originX, int originY, int originZ, int maxProximity) {
        if (world == null) return false;

        for (int dx = -maxProximity; dx <= maxProximity; dx++) {
            for (int dz = -maxProximity; dz <= maxProximity; dz++) {
                if (dx * dx + dz * dz > maxProximity * maxProximity) continue;

                // Check same level and 1 block below/above
                for (int dy = -1; dy <= 1; dy++) {
                    int x = originX + dx;
                    int y = originY + dy;
                    int z = originZ + dz;

                    if (y < 0 || y > 255) continue;

                    long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                    WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
                    if (chunk == null) continue;

                    // Check fluid id
                    if (chunk.getFluidId(x, y, z) != 0) {
                        return true;
                    }

                    // Check block type name
                    BlockType type = chunk.getBlockType(x, y, z);
                    if (type != null && type.getId() != null) {
                        String id = type.getId().toLowerCase(Locale.ROOT);
                        if (id.contains("water") || id.contains("fluid_water")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if there is a water puddle/source directly underneath the AutoFarm machine.
     * The machine requires water at (farmPos.x, farmPos.y - 1, farmPos.z) or (farmPos.x, farmPos.y - 2, farmPos.z).
     */
    public static boolean hasWaterUnderneath(World world, Vector3i farmPos) {
        if (world == null || farmPos == null) return false;
        return isWaterAt(world, farmPos.x, farmPos.y - 1, farmPos.z)
                || isWaterAt(world, farmPos.x, farmPos.y - 2, farmPos.z);
    }

    public static boolean isWaterAt(World world, int x, int y, int z) {
        if (world == null || y < 0 || y > 255) return false;
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) return false;

        try {
            if (chunk.getFluidId(x, y, z) != 0) {
                return true;
            }
        } catch (Throwable ignored) {}

        BlockType type = chunk.getBlockType(x, y, z);
        if (type != null && type.getId() != null) {
            String id = type.getId().toLowerCase(Locale.ROOT);
            return id.contains("water") || id.contains("fluid_water");
        }
        return false;
    }

    /**
     * Checks if a soil position is eligible for the AutoFarm to cultivate.
     * Criteria:
     * - Within maximum water irrigation range (4 blocks)
     * - Above block is empty (air)
     * - Soil is dirt/grass or already tilled
     * - Not claimed by another farm
     */
    public static boolean isEligibleSoil(World world, Vector3i soilPos, int maxWaterProximity, UUID farmId) {
        return isEligibleSoil(world, soilPos, maxWaterProximity, farmId, null);
    }

    public static boolean isEligibleSoil(World world, Vector3i soilPos, int maxWaterProximity, UUID farmId, Vector3i farmPos) {
        if (world == null || soilPos == null) return false;

        // Soil must be within maximum water puddle irrigation radius (4 blocks)
        if (farmPos != null) {
            int dx = Math.abs(soilPos.x - farmPos.x);
            int dz = Math.abs(soilPos.z - farmPos.z);
            if (Math.max(dx, dz) > 4) {
                return false;
            }
        }

        // Check above is Air
        int aboveY = soilPos.y + 1;
        if (aboveY > 255) return false;

        long chunkIdx = ChunkUtil.indexChunkFromBlock(soilPos.x, soilPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) return false;

        BlockType aboveType = chunk.getBlockType(soilPos.x, aboveY, soilPos.z);
        if (aboveType != null && !isAirOrFoliage(aboveType.getId())) {
            return false;
        }

        // Check not already planted
        Vector3i cropPos = new Vector3i(soilPos.x, aboveY, soilPos.z);
        if (AutoFarmRegistry.getPlantedCrop(cropPos) != null) {
            return false;
        }

        // Check if tilled by another farm
        TilledByFarmComponent tilledBy = AutoFarmRegistry.getTilledSoil(soilPos);
        if (tilledBy != null && !farmId.equals(tilledBy.getFarmId())) {
            return false; // Claimed by another farm
        }

        BlockType soilType = chunk.getBlockType(soilPos.x, soilPos.y, soilPos.z);
        if (soilType == null || soilType.getId() == null) return false;

        String lowerId = soilType.getId().toLowerCase(Locale.ROOT);
        boolean isAlreadyTilled = lowerId.contains("tilled");
        boolean isTillableDirt = lowerId.contains("dirt") || lowerId.contains("grass") || lowerId.contains("soil");

        return isAlreadyTilled || isTillableDirt;
    }

    /**
     * Checks if a soil position is eligible for planting a sapling/tree.
     * Criteria:
     * - Soil is dirt/grass (does not need tilling)
     * - Not adjacent to machine or chest (distance >= 2 blocks)
     * - Space between trees: at least 3 blocks apart from other trees/saplings
     * - Free of surrounding walls/blocks in 3x3 area
     * - High vertical clearance (at least 7 blocks above sapling)
     * - Not claimed by another farm or already planted
     */
    public static boolean isEligibleForTree(World world, Vector3i soilPos, UUID farmId) {
        return isEligibleForTree(world, soilPos, farmId, null, null);
    }

    public static boolean isEligibleForTree(World world, Vector3i soilPos, UUID farmId, Vector3i farmPos, Vector3i chestPos) {
        if (world == null || soilPos == null) return false;

        // 1. Must not be directly adjacent to machine or chest (needs at least 2 blocks distance)
        if (farmPos != null) {
            int distFarm = Math.max(Math.abs(soilPos.x - farmPos.x), Math.abs(soilPos.z - farmPos.z));
            if (distFarm < 2) return false;
        }
        if (chestPos != null) {
            int distChest = Math.max(Math.abs(soilPos.x - chestPos.x), Math.abs(soilPos.z - chestPos.z));
            if (distChest < 2) return false;
        }

        int soilY = soilPos.y;
        if (soilY + 8 > 255) return false;

        long chunkIdx = ChunkUtil.indexChunkFromBlock(soilPos.x, soilPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) return false;

        // Soil must be dirt or grass
        BlockType soilType = chunk.getBlockType(soilPos.x, soilPos.y, soilPos.z);
        if (soilType == null || soilType.getId() == null) return false;
        String lowerId = soilType.getId().toLowerCase(Locale.ROOT);
        if (!lowerId.contains("dirt") && !lowerId.contains("grass") && !lowerId.contains("soil")) return false;

        // Spot for sapling (y+1) can be air or soft replaceable foliage
        BlockType plantSpot = chunk.getBlockType(soilPos.x, soilY + 1, soilPos.z);
        if (plantSpot != null && !isAirOrFoliage(plantSpot.getId())) {
            return false;
        }

        // Check not already planted
        Vector3i cropPos = new Vector3i(soilPos.x, soilY + 1, soilPos.z);
        if (AutoFarmRegistry.getPlantedCrop(cropPos) != null) {
            return false;
        }

        // 2. Minimum Tree Spacing: at least 3 blocks apart from other planted crops/trees
        if (farmId != null) {
            var farmCrops = AutoFarmRegistry.getCropsForFarm(farmId);
            for (var entry : farmCrops.entrySet()) {
                Vector3i otherPos = entry.getKey();
                int dx = Math.abs(soilPos.x - otherPos.x);
                int dz = Math.abs(soilPos.z - otherPos.z);
                if (Math.max(dx, dz) < 3) {
                    return false; // Too close to another planted tree
                }
            }
        }

        // 3. Scan nearby blocks in chunk within radius 3 for existing tree trunks or saplings
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = 0; dy <= 4; dy++) {
                    int cx = soilPos.x + dx;
                    int cy = soilY + 1 + dy;
                    int cz = soilPos.z + dz;
                    long cIdx = ChunkUtil.indexChunkFromBlock(cx, cz);
                    WorldChunk cChunk = world.getChunkIfLoaded(cIdx);
                    if (cChunk == null) continue;
                    BlockType b = cChunk.getBlockType(cx, cy, cz);
                    if (b != null && b.getId() != null) {
                        String bId = b.getId().toLowerCase(Locale.ROOT);
                        if (bId.contains("trunk") || bId.contains("sapling")) {
                            return false; // Existing tree too close!
                        }
                    }
                }
            }
        }

        // 4. Vertical clearance: at least 7 blocks of open air above sapling (y+2 to y+8)
        for (int dy = 2; dy <= 8; dy++) {
            BlockType above = chunk.getBlockType(soilPos.x, soilY + dy, soilPos.z);
            if (above != null && !isAir(above.getId())) {
                return false;
            }
        }

        // 5. Horizontal clearance: surrounding blocks at y+1 and y+2 must not be solid walls
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = 1; dy <= 2; dy++) {
                    BlockType side = chunk.getBlockType(soilPos.x + dx, soilY + dy, soilPos.z + dz);
                    if (side != null && !isAirOrFoliage(side.getId())) {
                        return false; // Blocked by adjacent wall/obstacle
                    }
                }
            }
        }

        return true;
    }

    /**
     * Tills the block at soilPos, turning it into Soil_Dirt_Tilled and tagging it with TilledByFarmComponent.
     */
    public static boolean tillSoil(World world, Vector3i soilPos, UUID farmId) {
        if (world == null || soilPos == null || farmId == null) return false;

        long chunkIdx = ChunkUtil.indexChunkFromBlock(soilPos.x, soilPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) return false;

        BlockType current = chunk.getBlockType(soilPos.x, soilPos.y, soilPos.z);
        if (current != null && current.getId() != null && current.getId().equalsIgnoreCase(TILLED_SOIL_BLOCK)) {
            // Already tilled, ensure ownership
            AutoFarmRegistry.registerTilledSoil(soilPos, new TilledByFarmComponent(farmId, soilPos));
            return true;
        }

        // Modify world block
        boolean success = chunk.setBlock(soilPos.x, soilPos.y, soilPos.z, TILLED_SOIL_BLOCK);
        if (success) {
            AutoFarmRegistry.registerTilledSoil(soilPos, new TilledByFarmComponent(farmId, soilPos));
            System.out.println("[AutoFarm-TERRAFORM] Farm " + farmId + ": Tilled soil at " + soilPos);
            return true;
        }

        return false;
    }

    public static boolean isAir(String blockId) {
        if (blockId == null) return true;
        String lower = blockId.toLowerCase(Locale.ROOT);
        return lower.equals("empty") || lower.equals("air") || lower.isEmpty();
    }

    public static boolean isAirOrFoliage(String blockId) {
        if (blockId == null) return true;
        String lower = blockId.toLowerCase(Locale.ROOT);
        return isAir(blockId)
                || lower.contains("foliage")
                || lower.contains("flower")
                || lower.contains("grass_tall")
                || lower.contains("grass_short")
                || lower.contains("weed")
                || lower.contains("pebble");
    }
}
