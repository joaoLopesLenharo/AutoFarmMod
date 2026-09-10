package com.autofarm.mods.systems;

import com.autofarm.mods.AutoFarmRegistry;
import com.autofarm.mods.components.AutoPlantedComponent;
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

                // Check from 3 blocks below up to 2 blocks above
                for (int dy = -3; dy <= 2; dy++) {
                    int x = originX + dx;
                    int y = originY + dy;
                    int z = originZ + dz;

                    if (isWaterAt(world, x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if there is a water puddle/source directly underneath or around the base of the machine.
     * Searches in a 5x5 footprint up to 3 blocks down, with fallback to 4-block water proximity.
     */
    public static boolean hasWaterUnderneath(World world, Vector3i farmPos) {
        if (world == null || farmPos == null) return false;

        // 1. Direct check under and around the machine base (dx, dz in [-2, 2], dy in [-3, 0])
        for (int dy = 0; dy >= -3; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int x = farmPos.x + dx;
                    int y = farmPos.y + dy;
                    int z = farmPos.z + dz;
                    if (isWaterAt(world, x, y, z)) {
                        return true;
                    }
                }
            }
        }

        // 2. Fallback to general water proximity (within maximum irrigation radius 4)
        return isNearWater(world, farmPos.x, farmPos.y, farmPos.z, 4);
    }

    public static boolean isWaterAt(World world, int x, int y, int z) {
        if (world == null || y < 0 || y > 255) return false;
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) return false;

        // Check fluid id
        try {
            if (chunk.getFluidId(x, y, z) != 0) {
                return true;
            }
        } catch (Throwable ignored) {}

        // Check fluid level
        try {
            if (chunk.getFluidLevel(x, y, z) > 0) {
                return true;
            }
        } catch (Throwable ignored) {}

        // Check block type and material properties
        try {
            BlockType type = chunk.getBlockType(x, y, z);
            if (type != null) {
                if (type.getId() != null) {
                    String id = type.getId().toLowerCase(Locale.ROOT);
                    if (id.contains("water") || id.contains("fluid")) {
                        return true;
                    }
                }
                if (type.getGroup() != null && type.getGroup().toLowerCase(Locale.ROOT).contains("water")) {
                    return true;
                }
                if (type.getBlockSoundSetId() != null && type.getBlockSoundSetId().toLowerCase(Locale.ROOT).contains("water")) {
                    return true;
                }
                if (type.getBlockParticleSetId() != null && type.getBlockParticleSetId().toLowerCase(Locale.ROOT).contains("water")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

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
     * - Spot above (y+1) is empty air or soft foliage
     * - Not claimed by another farm or already planted
     * - Spacing from other trees: at least 2 blocks from any other planted tree
     * - Vertical clearance: at least 4 blocks of open air above sapling (y+2 to y+5)
     */
    public static boolean isEligibleForTree(World world, Vector3i soilPos, UUID farmId) {
        return isEligibleForTree(world, soilPos, farmId, null, null);
    }

    public static boolean isEligibleForTree(World world, Vector3i soilPos, UUID farmId, Vector3i farmPos, Vector3i chestPos) {
        if (world == null || soilPos == null) return false;

        // Must not be the machine or chest position
        if (farmPos != null && soilPos.x == farmPos.x && soilPos.z == farmPos.z) return false;
        if (chestPos != null && soilPos.x == chestPos.x && soilPos.z == chestPos.z) return false;

        int soilY = soilPos.y;
        if (soilY + 5 > 255) return false;

        long chunkIdx = ChunkUtil.indexChunkFromBlock(soilPos.x, soilPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) return false;

        // Soil must be dirt, grass, or soil
        BlockType soilType = chunk.getBlockType(soilPos.x, soilY, soilPos.z);
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

        // Tree Spacing: at least 2 blocks apart from other planted TREES (agricultural crops don't block trees)
        if (farmId != null) {
            var farmCrops = AutoFarmRegistry.getCropsForFarm(farmId);
            for (var entry : farmCrops.entrySet()) {
                AutoPlantedComponent otherPlant = entry.getValue();
                com.autofarm.mods.catalog.PlantSpecies otherSpecies = com.autofarm.mods.catalog.PlantCatalog.resolve(otherPlant.getPlantType());
                if (otherSpecies != null && otherSpecies.isTree()) {
                    Vector3i otherPos = entry.getKey();
                    int dx = Math.abs(soilPos.x - otherPos.x);
                    int dz = Math.abs(soilPos.z - otherPos.z);
                    if (Math.max(dx, dz) < 2) {
                        return false; // Too close to another planted tree
                    }
                }
            }
        }

        // Vertical clearance: at least 4 blocks of open air above sapling (y+2 to y+5)
        for (int dy = 2; dy <= 5; dy++) {
            BlockType above = chunk.getBlockType(soilPos.x, soilY + dy, soilPos.z);
            if (above != null && !isAirOrFoliage(above.getId())) {
                return false;
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
