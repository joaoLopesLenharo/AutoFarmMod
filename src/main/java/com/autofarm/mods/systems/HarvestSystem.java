package com.autofarm.mods.systems;

import com.autofarm.mods.AutoFarmConfig;
import com.autofarm.mods.AutoFarmRegistry;
import com.autofarm.mods.catalog.PlantCatalog;
import com.autofarm.mods.catalog.PlantSpecies;
import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.autofarm.mods.components.AutoPlantedComponent;
import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HarvestSystem {

    public static final long DEFAULT_MATURITY_TICKS = 600;

    // Universal Essence & Tree Material IDs
    public static final String LIFE_ESSENCE_ITEM = "Ingredient_Life_Essence";
    public static final String STICK_ITEM = "Ingredient_Stick";
    public static final String TREE_SAP_ITEM = "Ingredient_Tree_Sap";
    public static final String PLANT_FIBRE_ITEM = "Ingredient_Fibre";
    public static final String TREE_BARK_ITEM = "Ingredient_Bark";

    /**
     * Retrieves current in-game day (elapsed days in Hytale world).
     * 1 day = 86,400 seconds (WorldTimeResource.SECONDS_PER_DAY).
     */
    public static double getInGameDay(World world) {
        if (world == null) return -1.0;
        try {
            if (world.getEntityStore() != null && world.getEntityStore().getStore() != null) {
                WorldTimeResource timeRes = world.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
                if (timeRes != null && timeRes.getGameTime() != null) {
                    long epochSec = timeRes.getGameTime().getEpochSecond();
                    return (double) epochSec / (double) WorldTimeResource.SECONDS_PER_DAY;
                }
            }
        } catch (Throwable ignored) {}
        return -1.0;
    }

    /**
     * Resolves the final growth stage index for a tree/plant based on its Farming asset config.
     */
    public static int getFinalStageIndex(String blockId) {
        if (blockId == null) return 5;
        try {
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt != null && bt.getFarming() != null && bt.getFarming().getStages() != null) {
                String stageSet = bt.getFarming().getStartingStageSet();
                if (stageSet == null) stageSet = "Default";
                FarmingStageData[] stages = bt.getFarming().getStages().get(stageSet);
                if (stages != null && stages.length > 0) {
                    return stages.length - 1;
                }
            }
        } catch (Throwable ignored) {}
        return 5;
    }

    /**
     * Resolves the minimum required in-game days for a tree to reach its final stage.
     */
    public static double getRequiredTreeGrowthDays(String blockId) {
        if (blockId == null) return AutoFarmConfig.get().treeMaturityMinDays;
        try {
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt != null && bt.getFarming() != null && bt.getFarming().getStages() != null) {
                String stageSet = bt.getFarming().getStartingStageSet();
                if (stageSet == null) stageSet = "Default";
                FarmingStageData[] stages = bt.getFarming().getStages().get(stageSet);
                if (stages != null && stages.length > 0) {
                    double totalSec = 0;
                    for (FarmingStageData stage : stages) {
                        if (stage.getDuration() != null) {
                            totalSec += stage.getDuration().min;
                        }
                    }
                    if (totalSec > 0) {
                        return totalSec / (double) WorldTimeResource.SECONDS_PER_DAY;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return AutoFarmConfig.get().treeMaturityMinDays;
    }

    /**
     * Inspects a single plant position to determine if it is ready for harvesting.
     */
    public static boolean isMature(World world, Vector3i plantPos, AutoPlantedComponent plant, 
                                  long currentTick, long maturityTicksThreshold) {
        if (world == null || plantPos == null || plant == null) {
            return false;
        }

        long chunkIdx = ChunkUtil.indexChunkFromBlock(plantPos.x, plantPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) {
            return false;
        }

        BlockType currentType = chunk.getBlockType(plantPos.x, plantPos.y, plantPos.z);
        if (currentType == null || TerraformSystem.isAir(currentType.getId())) {
            AutoFarmRegistry.removePlantedCrop(plantPos);
            return false;
        }

        PlantSpecies species = PlantCatalog.resolve(plant.getPlantType());

        // 1. Tree maturity: Trees have distinct growth stages calculated in in-game days.
        // A tree MUST only be cut down in its final (last) growth stage.
        if (species != null && species.isTree()) {
            return isTreeMature(world, chunk, plantPos, plant, species);
        }

        // 2. Crop maturity: Native ECS FarmingBlock component
        Holder<ChunkStore> holder = chunk.getBlockComponentHolder(plantPos.x, plantPos.y, plantPos.z);
        if (holder != null) {
            FarmingBlock farming = holder.getComponent(FarmingBlock.getComponentType());
            if (farming != null) {
                if (farming.getGrowthProgress() >= 1.0f) {
                    return true;
                }
                String stage = farming.getCurrentStageSet();
                if (stage != null && (stage.equalsIgnoreCase("StageFinal") || stage.equalsIgnoreCase("Harvested"))) {
                    return true;
                }
            }
        }

        // 3. BlockType name convention
        String typeId = currentType.getId();
        if (typeId != null) {
            String lower = typeId.toLowerCase(Locale.ROOT);
            if (lower.contains("stagefinal") || lower.contains("mature")) {
                return true;
            }
        }

        // 4. Tick fallback threshold for agricultural crops
        long elapsed = currentTick - plant.getPlantedAtTick();
        return elapsed >= maturityTicksThreshold;
    }

    /**
     * Checks if a tree has reached its final growth stage.
     * Trees have multiple growth stages (sapling -> intermediate stages -> final mature tree).
     * Hytale calculates tree growth in in-game days.
     * Only the final growth stage may be cut down.
     */
    public static boolean isTreeMature(World world, WorldChunk chunk, Vector3i plantPos, 
                                       AutoPlantedComponent plant, PlantSpecies species) {
        if (world == null || chunk == null || plantPos == null || plant == null || species == null) {
            return false;
        }

        BlockType currentType = chunk.getBlockType(plantPos.x, plantPos.y, plantPos.z);
        if (currentType == null || TerraformSystem.isAir(currentType.getId())) {
            AutoFarmRegistry.removePlantedCrop(plantPos);
            return false;
        }

        String currentId = currentType.getId().toLowerCase(Locale.ROOT);
        // Stage 0: If it is still a sapling block, it has not grown into a tree yet
        if (currentId.contains("sapling")) {
            return false;
        }

        AutoFarmConfig config = AutoFarmConfig.get();
        String saplingBlockId = species.getBlockId();
        int finalStage = getFinalStageIndex(saplingBlockId);

        // 1. Check ECS FarmingBlock component on root block
        Holder<ChunkStore> holder = chunk.getBlockComponentHolder(plantPos.x, plantPos.y, plantPos.z);
        if (holder != null) {
            FarmingBlock farming = holder.getComponent(FarmingBlock.getComponentType());
            if (farming != null) {
                float growthProgress = farming.getGrowthProgress();
                if (growthProgress < (float) finalStage) {
                    // Tree is in an intermediate growth stage (e.g. Stage 1, 2, 3 or 4 of 5)
                    return false;
                }
                // Reached or exceeded final stage
                return true;
            }
        }

        // 2. In-game Days Elapsed Check (Hytale calculates tree growth in in-game days)
        double currentDay = getInGameDay(world);
        double plantedDay = plant.getPlantedDay();
        double reqDays = getRequiredTreeGrowthDays(saplingBlockId);
        if (plantedDay > 0 && currentDay > 0) {
            double elapsedDays = currentDay - plantedDay;
            if (elapsedDays < reqDays) {
                // Not enough in-game days have passed for the tree to complete all stages
                return false;
            }
        }

        // 3. Physical Tree Structure Verification in Chunk:
        // Measure trunk height above ground (intermediate stages are only 1-3 blocks high)
        int trunkHeight = 0;
        boolean hasLeaves = false;
        for (int dy = 0; dy <= 24; dy++) {
            int checkY = plantPos.y + dy;
            if (checkY > 255) break;

            BlockType block = chunk.getBlockType(plantPos.x, checkY, plantPos.z);
            if (block != null && !TerraformSystem.isAir(block.getId())) {
                String id = block.getId().toLowerCase(Locale.ROOT);
                if (id.contains("trunk") || id.contains("wood_")) {
                    trunkHeight++;
                } else if (id.contains("leaves")) {
                    hasLeaves = true;
                    break;
                }
            } else {
                break;
            }
        }

        // A fully mature tree in Hytale has a trunk height >= treeMinTrunkHeight (default 5 blocks)
        if (trunkHeight < config.treeMinTrunkHeight) {
            return false;
        }

        // Verify leaves canopy exists around or above trunk
        if (!hasLeaves) {
            int topY = plantPos.y + trunkHeight;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 3; dy++) {
                        BlockType b = chunk.getBlockType(plantPos.x + dx, topY + dy, plantPos.z + dz);
                        if (b != null && b.getId() != null && b.getId().toLowerCase(Locale.ROOT).contains("leaves")) {
                            hasLeaves = true;
                            break;
                        }
                    }
                    if (hasLeaves) break;
                }
                if (hasLeaves) break;
            }
        }

        return hasLeaves;
    }

    /**
     * Determines complete drops for any plant species:
     * - Crops: Crop item + seeds + Life Essence (Ingredient_Life_Essence).
     * - Trees: Wood trunk logs + saplings + sticks + sap + leaves fiber + bark.
     */
    public record DropEntry(String itemId, int quantity) {}

    /**
     * Calculates the full drop list entries for crops or trees.
     */
    public static List<DropEntry> calculateHarvestDropEntries(PlantSpecies species, int logCount) {
        List<DropEntry> entries = new ArrayList<>();
        if (species == null) return entries;

        if (species.isTree()) {
            int totalLogs = Math.max(logCount, species.getDefaultHarvestQty());
            String trunkId = species.getTreeWoodTrunkId() != null ? species.getTreeWoodTrunkId() : "Wood_Oak_Trunk";
            
            // 1. Wood trunk logs
            entries.add(new DropEntry(trunkId, totalLogs));
            // 2. Saplings for continuous replanting
            String saplingId = !species.getSeedItemIds().isEmpty() ? species.getSeedItemIds().get(0) : "Plant_Sapling_Oak";
            entries.add(new DropEntry(saplingId, Math.max(2, species.getDefaultSeedQty())));
            // 3. Sticks
            entries.add(new DropEntry(STICK_ITEM, 3));
            // 4. Tree Sap
            entries.add(new DropEntry(TREE_SAP_ITEM, 2));
            // 5. Plant Fibre (leaves drop)
            entries.add(new DropEntry(PLANT_FIBRE_ITEM, 2));
            // 6. Tree Bark
            entries.add(new DropEntry(TREE_BARK_ITEM, 1));
        } else {
            // 1. Crop Food / Harvest item
            entries.add(new DropEntry(species.getHarvestItemId(), species.getDefaultHarvestQty()));
            // 2. Replant seeds
            String seedId = !species.getSeedItemIds().isEmpty() ? species.getSeedItemIds().get(0) : "Plant_Seeds_Wheat";
            entries.add(new DropEntry(seedId, species.getDefaultSeedQty()));
            // 3. Life Essence (Essência da Vida)
            entries.add(new DropEntry(LIFE_ESSENCE_ITEM, 3));
        }

        return entries;
    }

    /**
     * Determines complete drops as ItemStacks for server world operations.
     */
    public static List<ItemStack> getHarvestDrops(PlantSpecies species, int logCount) {
        List<ItemStack> drops = new ArrayList<>();
        List<DropEntry> entries = calculateHarvestDropEntries(species, logCount);

        for (DropEntry entry : entries) {
            try {
                drops.add(new ItemStack(entry.itemId(), entry.quantity()));
            } catch (Throwable t) {
                // In headless tests outside Hytale runtime, ItemStack constructor throws
            }
        }

        return drops;
    }

    public static boolean harvestCrop(World world, Vector3i plantPos, AutoPlantedComponent plant, 
                                      Vector3i chestPos, UUID farmId) {
        return harvestCrop(world, null, null, plantPos, plant, chestPos, farmId);
    }

    /**
     * Harvests a mature crop or tree:
     * - Clears blocks.
     * - Generates and deposits all native drops into linked chest (food, seeds, Life Essence, wood, sap, fiber, bark).
     * - Vacuums any dropped entity items on the ground within the harvest radius.
     */
    public static boolean harvestCrop(World world, Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
                                      Vector3i plantPos, AutoPlantedComponent plant, 
                                      Vector3i chestPos, UUID farmId) {
        if (world == null || plantPos == null || plant == null) {
            return false;
        }

        // INVIOLABLE RULE: Verify ownership
        if (!farmId.equals(plant.getFarmId())) {
            return false;
        }

        long chunkIdx = ChunkUtil.indexChunkFromBlock(plantPos.x, plantPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) {
            return false;
        }

        PlantSpecies species = PlantCatalog.resolve(plant.getPlantType());
        int felledLogs = 0;

        if (species != null && species.isTree()) {
            // Tree felling: Scan and clear trunk column (up to 16 blocks high)
            for (int dy = 0; dy <= 16; dy++) {
                int ty = plantPos.y + dy;
                if (ty > 255) break;

                BlockType block = chunk.getBlockType(plantPos.x, ty, plantPos.z);
                if (block != null && !TerraformSystem.isAir(block.getId())) {
                    String id = block.getId().toLowerCase(Locale.ROOT);
                    if (id.contains("trunk") || id.contains("sapling") || id.contains("wood_") || id.contains("leaves")) {
                        chunk.setBlock(plantPos.x, ty, plantPos.z, "Empty");
                        felledLogs++;
                    } else if (dy > 0) {
                        break;
                    }
                }
            }
        } else {
            // Crop clearing
            boolean removed = chunk.setBlock(plantPos.x, plantPos.y, plantPos.z, "Empty");
            if (!removed) {
                chunk.breakBlock(plantPos.x, plantPos.y, plantPos.z, 0);
            }
        }

        // 1. Deposit complete drop list into chest
        List<ItemStack> drops = getHarvestDrops(species, felledLogs);
        for (ItemStack item : drops) {
            int remainder = ChestLinkSystem.deposit(world, chestPos, item);
            if (remainder > 0) {
                System.out.println("[AutoFarm-HARVEST] OVERFLOW: Chest at " + chestPos + " full! Remainder: " 
                        + remainder + "x " + item.getItemId());
            }
        }

        // 2. Vacuum any physical item drops spawned on the ground
        double vacuumRadius = (species != null && species.isTree()) ? 8.0 : 4.0;
        vacuumNearbyItemEntities(world, store, buffer, plantPos, chestPos, vacuumRadius);

        // 3. Unlink plant from registry (frees position for continuous replanting)
        AutoFarmRegistry.removePlantedCrop(plantPos);

        String category = (species != null && species.isTree()) ? "Tree" : "Crop";
        System.out.println("[AutoFarm-HARVEST] Farm " + farmId + ": Harvested mature " + category + " [" 
                + (species != null ? species.getId() : "unknown") + "] at " + plantPos 
                + " -> Complete drops & Life Essence deposited to chest at " + chestPos);
        return true;
    }

    /**
     * Vacuums all physical dropped item entities in the world near plantPos into chestPos.
     */
    public static void vacuumNearbyItemEntities(World world, Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
                                                Vector3i center, Vector3i chestPos, double radius) {
        if (world == null || store == null || buffer == null || chestPos == null || center == null) {
            return;
        }

        try {
            double radiusSq = radius * radius;
            Query<EntityStore> itemQuery = Query.and(ItemComponent.getComponentType(), TransformComponent.getComponentType());

            store.forEachChunk(itemQuery, (chunk, cmdBuffer) -> {
                int size = chunk.size();
                for (int i = 0; i < size; i++) {
                    TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
                    ItemComponent itemComp = chunk.getComponent(i, ItemComponent.getComponentType());

                    if (transform != null && itemComp != null) {
                        Vector3d pos = transform.getPosition();
                        if (pos != null) {
                            double distSq = pos.distanceSquared(center.x + 0.5, center.y + 0.5, center.z + 0.5);
                            if (distSq <= radiusSq) {
                                ItemStack stack = itemComp.getItemStack();
                                if (stack != null && stack.getQuantity() > 0) {
                                    int remainder = ChestLinkSystem.deposit(world, chestPos, stack);
                                    if (remainder <= 0) {
                                        // Successfully deposited full stack, despawn entity
                                        Ref<EntityStore> ref = chunk.getReferenceTo(i);
                                        if (ref != null && buffer != null) {
                                            buffer.removeEntity(ref, RemoveReason.REMOVE);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            // Non-fatal safety catch for headless or mock environments
            System.out.println("[AutoFarm-HARVEST] Entity vacuum scan info: " + t.getMessage());
        }
    }

    public static int processHarvestCycle(World world, AutoFarmBlockComponent farm, long currentTick) {
        return processHarvestCycle(world, null, null, farm, currentTick);
    }

    /**
     * Scans all plants owned by this farm and harvests any mature ones.
     */
    public static int processHarvestCycle(World world, Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
                                          AutoFarmBlockComponent farm, long currentTick) {
        if (world == null || farm == null) {
            return 0;
        }

        Vector3i chestPos = farm.getLinkedChestPosition();
        if (chestPos == null || !ChestLinkSystem.isChestValid(world, chestPos)) {
            return 0;
        }

        UUID farmId = farm.getFarmId();
        Map<Vector3i, AutoPlantedComponent> farmCrops = AutoFarmRegistry.getCropsForFarm(farmId);
        if (farmCrops.isEmpty()) {
            return 0;
        }

        int harvestedCount = 0;
        for (Map.Entry<Vector3i, AutoPlantedComponent> entry : farmCrops.entrySet()) {
            Vector3i plantPos = entry.getKey();
            AutoPlantedComponent plant = entry.getValue();

            if (isMature(world, plantPos, plant, currentTick, DEFAULT_MATURITY_TICKS)) {
                boolean success = harvestCrop(world, store, buffer, plantPos, plant, chestPos, farmId);
                if (success) {
                    harvestedCount++;
                }
            }
        }

        return harvestedCount;
    }
}
