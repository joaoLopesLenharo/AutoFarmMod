package com.autofarm.mods.systems;

import com.autofarm.mods.AutoFarmRegistry;
import com.autofarm.mods.catalog.PlantCatalog;
import com.autofarm.mods.catalog.PlantSpecies;
import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.autofarm.mods.components.AutoPlantedComponent;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.joml.Vector3i;

import java.util.UUID;

public class PlantingSystem {

    /**
     * Resolves the block ID to place for any given seed or sapling item ID.
     */
    public static String getCropBlockForSeed(String seedItemId) {
        PlantSpecies species = PlantCatalog.resolve(seedItemId);
        return species != null ? species.getBlockId() : "Plant_Crop_Wheat_Block";
    }

    /**
     * Plants a crop or tree sapling at the specified soil position:
     * - Crops: Ensure soil is tilled near water and place crop block.
     * - Trees: Place sapling directly on dirt/grass with vertical clearance.
     * Registers ownership in AutoFarmRegistry.
     */
    public static boolean plantCrop(World world, Vector3i soilPos, UUID farmId, Vector3i chestPos, long currentTick) {
        return plantCrop(world, soilPos, farmId, null, chestPos, currentTick, true, true);
    }

    public static boolean plantCrop(World world, Vector3i soilPos, UUID farmId, Vector3i chestPos, long currentTick,
                                   boolean eligibleCrop, boolean eligibleTree) {
        return plantCrop(world, soilPos, farmId, null, chestPos, currentTick, eligibleCrop, eligibleTree);
    }

    public static boolean plantCrop(World world, Vector3i soilPos, UUID farmId, Vector3i origin, Vector3i chestPos, long currentTick,
                                   boolean eligibleCrop, boolean eligibleTree) {
        if (world == null || soilPos == null || farmId == null || chestPos == null) {
            return false;
        }

        Vector3i plantPos = new Vector3i(soilPos.x, soilPos.y + 1, soilPos.z);
        if (plantPos.y > 255) {
            return false;
        }

        // 1. Withdraw 1 matching seed or sapling from linked chest
        String seedItem = ChestLinkSystem.withdrawPlantable(world, chestPos, eligibleCrop, eligibleTree);
        if (seedItem == null) {
            return false; // No matching plantable item in chest
        }

        PlantSpecies species = PlantCatalog.resolve(seedItem);

        // 2. Prepare terrain according to plant category
        if (species.isCrop()) {
            boolean tilled = TerraformSystem.tillSoil(world, soilPos, farmId);
            if (!tilled) {
                // Refund seed back into chest
                ChestLinkSystem.deposit(world, chestPos, new com.hypixel.hytale.server.core.inventory.ItemStack(seedItem, 1));
                return false;
            }
        } else if (species.isTree()) {
            // Trees need dirt/grass, spacing, and vertical clearance
            if (!TerraformSystem.isEligibleForTree(world, soilPos, farmId, origin, chestPos)) {
                // Refund sapling back into chest
                ChestLinkSystem.deposit(world, chestPos, new com.hypixel.hytale.server.core.inventory.ItemStack(seedItem, 1));
                return false;
            }
        }

        // 3. Place plant or sapling block in world chunk
        long chunkIdx = ChunkUtil.indexChunkFromBlock(plantPos.x, plantPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) {
            ChestLinkSystem.deposit(world, chestPos, new com.hypixel.hytale.server.core.inventory.ItemStack(seedItem, 1));
            return false;
        }

        String blockToPlace = species.getBlockId();
        boolean placed = chunk.setBlock(plantPos.x, plantPos.y, plantPos.z, blockToPlace);
        if (!placed) {
            System.out.println("[AutoFarm-PLANT] Farm " + farmId + ": Failed to set block " + blockToPlace + " at " + plantPos);
            ChestLinkSystem.deposit(world, chestPos, new com.hypixel.hytale.server.core.inventory.ItemStack(seedItem, 1));
            return false;
        }

        // 4. Register ownership in AutoFarmRegistry (Inviolable rule enforcement)
        double currentDay = HarvestSystem.getInGameDay(world);
        AutoPlantedComponent plantedComponent = new AutoPlantedComponent(farmId, species.getId(), currentTick, currentDay, plantPos);
        AutoFarmRegistry.registerPlantedCrop(plantPos, plantedComponent);

        String categoryName = species.isTree() ? "Tree Sapling" : "Crop";
        System.out.println("[AutoFarm-PLANT] Farm " + farmId + ": Planted " + categoryName + " [" 
                + species.getId() + "] (" + blockToPlace + ") at " + plantPos);
        return true;
    }

    /**
     * Scans and executes planting actions for crops and trees.
     * Capped at maximum water puddle irrigation radius (4 blocks).
     */
    public static int processPlantingCycle(World world, AutoFarmBlockComponent farm, long currentTick, int maxBatch) {
        boolean hasWater = farm != null && TerraformSystem.hasWaterUnderneath(world, farm.getPosition());
        return processPlantingCycle(world, farm, currentTick, maxBatch, hasWater);
    }

    public static int processPlantingCycle(World world, AutoFarmBlockComponent farm, long currentTick, int maxBatch, boolean hasWater) {
        if (world == null || farm == null) {
            return 0;
        }

        Vector3i chestPos = farm.getLinkedChestPosition();
        if (chestPos == null || !ChestLinkSystem.isChestValid(world, chestPos)) {
            return 0;
        }

        UUID farmId = farm.getFarmId();
        int activeCount = AutoFarmRegistry.countActivePlantsForFarm(farmId);
        int maxAllowed = farm.getMaxActivePlantsPerFarm();
        if (activeCount >= maxAllowed) {
            return 0;
        }

        int remainingAllowance = Math.min(maxAllowed - activeCount, maxBatch);
        int plantedCount = 0;

        Vector3i origin = farm.getPosition();
        // Maximum space around is strictly limited to what the water puddle underneath can irrigate (4 blocks)
        int range = Math.min(farm.getRange() > 0 ? farm.getRange() : 4, 4);
        int vRange = Math.min(farm.getVerticalRange() > 0 ? farm.getVerticalRange() : 4, 4);
        int waterProximity = farm.getWaterProximityMax();

        for (int r = 1; r <= range && plantedCount < remainingAllowance; r++) {
            for (int dx = -r; dx <= r && plantedCount < remainingAllowance; dx++) {
                for (int dz = -r; dz <= r && plantedCount < remainingAllowance; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    for (int dy = -vRange; dy <= vRange && plantedCount < remainingAllowance; dy++) {
                        int x = origin.x + dx;
                        int y = origin.y + dy;
                        int z = origin.z + dz;

                        if (x == origin.x && y == origin.y && z == origin.z) continue;
                        if (x == chestPos.x && y == chestPos.y && z == chestPos.z) continue;

                        Vector3i soilCandidate = new Vector3i(x, y, z);
                        Vector3i plantPos = new Vector3i(x, y + 1, z);
                        if (plantPos.equals(origin) || plantPos.equals(chestPos)) continue;
                        
                        // Check eligibility for crops (requires hasWater + valid soil within radius)
                        // Trees do NOT require water (trees grow on dirt/grass with proper spacing and vertical clearance)
                        boolean eligibleCrop = hasWater && TerraformSystem.isEligibleSoil(world, soilCandidate, waterProximity, farmId, origin);
                        boolean eligibleTree = TerraformSystem.isEligibleForTree(world, soilCandidate, farmId, origin, chestPos);

                        if (eligibleCrop || eligibleTree) {
                            boolean success = plantCrop(world, soilCandidate, farmId, origin, chestPos, currentTick, eligibleCrop, eligibleTree);
                            if (success) {
                                plantedCount++;
                            }
                        }
                    }
                }
            }
        }

        return plantedCount;
    }
}
