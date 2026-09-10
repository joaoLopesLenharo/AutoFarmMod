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
     * Checks if a soil position is eligible for the AutoFarm to cultivate.
     * Criteria:
     * - Above block is empty (air)
     * - Soil is dirt/grass or already tilled
     * - Water within maxProximity (if not already tilled)
     * - Not claimed by another farm
     */
    public static boolean isEligibleSoil(World world, Vector3i soilPos, int maxWaterProximity, UUID farmId) {
        if (world == null || soilPos == null) return false;

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
     * - Space immediately above (y+1) is empty air or soft foliage
     * - Vertical clearance above (y+2 to y+4) is empty
     * - Not claimed by another farm or already planted
     */
    public static boolean isEligibleForTree(World world, Vector3i soilPos, UUID farmId) {
        if (world == null || soilPos == null) return false;

        int soilY = soilPos.y;
        if (soilY + 4 > 255) return false;

        long chunkIdx = ChunkUtil.indexChunkFromBlock(soilPos.x, soilPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) return false;

        // Soil must be dirt or grass
        BlockType soilType = chunk.getBlockType(soilPos.x, soilPos.y, soilPos.z);
        if (soilType == null || soilType.getId() == null) return false;
        String lowerId = soilType.getId().toLowerCase(Locale.ROOT);
        if (!lowerId.contains("dirt") && !lowerId.contains("grass") && !lowerId.contains("soil")) return false;

        // Position y+1 can be air or soft replaceable foliage
        BlockType plantSpot = chunk.getBlockType(soilPos.x, soilY + 1, soilPos.z);
        if (plantSpot != null && !isAirOrFoliage(plantSpot.getId())) {
            return false;
        }

        // Check vertical clearance of 3 blocks above the sapling (y+2 to y+4)
        for (int dy = 2; dy <= 4; dy++) {
            BlockType above = chunk.getBlockType(soilPos.x, soilY + dy, soilPos.z);
            if (above != null && !isAir(above.getId())) {
                return false;
            }
        }

        // Check not already planted
        Vector3i cropPos = new Vector3i(soilPos.x, soilY + 1, soilPos.z);
        if (AutoFarmRegistry.getPlantedCrop(cropPos) != null) {
            return false;
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
