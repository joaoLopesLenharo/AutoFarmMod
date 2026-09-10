package com.autofarm.mods;

import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.autofarm.mods.components.AutoPlantedComponent;
import com.autofarm.mods.components.TilledByFarmComponent;
import com.autofarm.mods.systems.ChestLinkSystem;
import com.autofarm.mods.systems.FarmScanSystem;
import org.joml.Vector3i;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class AutoFarmTest {

    @BeforeEach
    public void setup() {
        AutoFarmRegistry.clearAll();
    }

    @Test
    public void testFarmComponentCreationAndCloning() {
        UUID farmId = UUID.randomUUID();
        Vector3i pos = new Vector3i(10, 64, 20);
        AutoFarmBlockComponent farm = new AutoFarmBlockComponent(farmId, pos);

        Assertions.assertEquals(farmId, farm.getFarmId());
        Assertions.assertEquals(pos, farm.getPosition());
        Assertions.assertEquals(64, farm.getRange());
        Assertions.assertEquals(8, farm.getVerticalRange());
        Assertions.assertEquals(4, farm.getWaterProximityMax());
        Assertions.assertEquals(32, farm.getMaxActivePlantsPerFarm());

        AutoFarmBlockComponent cloned = farm.clone();
        Assertions.assertEquals(farm.getFarmId(), cloned.getFarmId());
        Assertions.assertEquals(farm.getPosition(), cloned.getPosition());
    }

    @Test
    public void testInviolableRuleFarmDestructionUnlinksCrops() {
        UUID farmId = UUID.randomUUID();
        Vector3i farmPos = new Vector3i(0, 60, 0);
        AutoFarmBlockComponent farm = new AutoFarmBlockComponent(farmId, farmPos);
        AutoFarmRegistry.registerFarm(farm);

        Vector3i cropPos = new Vector3i(2, 60, 3);
        AutoPlantedComponent crop = new AutoPlantedComponent(farmId, "Wheat", 100L, cropPos);
        AutoFarmRegistry.registerPlantedCrop(cropPos, crop);

        Vector3i soilPos = new Vector3i(2, 59, 3);
        TilledByFarmComponent soil = new TilledByFarmComponent(farmId, soilPos);
        AutoFarmRegistry.registerTilledSoil(soilPos, soil);

        Assertions.assertEquals(1, AutoFarmRegistry.countActivePlantsForFarm(farmId));
        Assertions.assertNotNull(AutoFarmRegistry.getPlantedCrop(cropPos));
        Assertions.assertNotNull(AutoFarmRegistry.getTilledSoil(soilPos));

        // When farm is destroyed: all claims MUST be unlinked (inviolable rule)
        AutoFarmRegistry.unregisterFarm(farmId);

        Assertions.assertEquals(0, AutoFarmRegistry.countActivePlantsForFarm(farmId));
        Assertions.assertNull(AutoFarmRegistry.getPlantedCrop(cropPos));
        Assertions.assertNull(AutoFarmRegistry.getTilledSoil(soilPos));
    }

    @Test
    public void testChestLinkSystemSeedIdentification() {
        // Broad seed checks
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Crop_Wheat_Seed", null));
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Corn_Sprout", null));
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Tree_Sapling_Oak", null));
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Carrot_Crop", null));
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Potato_Seed", null));
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Cabbage_Seeds", null));

        // Non-seed items
        Assertions.assertFalse(ChestLinkSystem.isSeedItem("Ingredient_Bar_Iron", null));
        Assertions.assertFalse(ChestLinkSystem.isSeedItem("Tool_Hoe_Iron", null));
        Assertions.assertFalse(ChestLinkSystem.isSeedItem("Rock", null));
        Assertions.assertFalse(ChestLinkSystem.isSeedItem(null, null));

        // Preferred seed checks
        Assertions.assertTrue(ChestLinkSystem.isSeedItem("Crop_Wheat_Seed", "Wheat"));
        Assertions.assertFalse(ChestLinkSystem.isSeedItem("Crop_Carrot_Seed", "Wheat"));
    }

    @Test
    public void testChestLinkSystemNullSafety() {
        // Verify null safety for world / positions when world is not loaded
        Assertions.assertNull(ChestLinkSystem.findNearestChest(null, new Vector3i(0, 0, 0), 64, 8));
        Assertions.assertNull(ChestLinkSystem.getChestContainer(null, new Vector3i(0, 0, 0)));
        Assertions.assertFalse(ChestLinkSystem.isChestValid(null, new Vector3i(0, 0, 0)));
        Assertions.assertNull(ChestLinkSystem.withdrawSeedFromContainer(null, null));
        Assertions.assertEquals(0, ChestLinkSystem.depositToContainer(null, null));
    }

    @Test
    public void testFarmScanSystemLogModeExecution() {
        UUID farmId = UUID.randomUUID();
        Vector3i pos = new Vector3i(100, 65, 200);
        AutoFarmBlockComponent farm = new AutoFarmBlockComponent(farmId, pos);
        farm.setScanIntervalTicks(0); // force scan immediately

        FarmScanSystem scanSystem = new FarmScanSystem();
        Assertions.assertDoesNotThrow(() -> scanSystem.runAutonomousCycle(null, farm));
    }

    @Test
    public void testPlantingSystemSeedMapping() {
        Assertions.assertEquals("Plant_Crop_Wheat_Block", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed("Plant_Seeds_Wheat"));
        Assertions.assertEquals("Plant_Crop_Carrot_Block", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed("Plant_Seeds_Carrot"));
        Assertions.assertEquals("Plant_Crop_Corn_Block", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed("Corn_Seeds"));
        Assertions.assertEquals("Plant_Crop_Wheat_Block", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed(null));
    }

    @Test
    public void testHarvestSystemInviolableOwnership() {
        UUID farm1 = UUID.randomUUID();
        UUID farm2 = UUID.randomUUID();
        Vector3i cropPos = new Vector3i(10, 64, 10);
        AutoPlantedComponent crop = new AutoPlantedComponent(farm1, "Wheat", 100L, cropPos);
        AutoFarmRegistry.registerPlantedCrop(cropPos, crop);

        // Farm2 must NEVER be able to harvest Farm1's crop (Inviolable rule)
        boolean harvestedByForeignFarm = com.autofarm.mods.systems.HarvestSystem.harvestCrop(
                null, cropPos, crop, new Vector3i(0, 0, 0), farm2
        );
        Assertions.assertFalse(harvestedByForeignFarm);
        Assertions.assertNotNull(AutoFarmRegistry.getPlantedCrop(cropPos));
    }

    @Test
    public void testAutoFarmConfigDefaultsAndJson() {
        AutoFarmConfig config = new AutoFarmConfig();
        Assertions.assertEquals(40, config.scanIntervalTicks);
        Assertions.assertEquals(64, config.horizontalRange);
        Assertions.assertEquals(8, config.verticalRange);
        Assertions.assertEquals(4, config.waterProximityMax);
        Assertions.assertEquals(64, config.maxActivePlantsPerFarm);

        String json = config.toJson();
        Assertions.assertTrue(json.contains("\"horizontalRange\": 64"));

        AutoFarmConfig parsed = new AutoFarmConfig();
        parsed.parseSimpleJson(json);
        Assertions.assertEquals(64, parsed.horizontalRange);
        Assertions.assertEquals(4, parsed.waterProximityMax);
    }

    @Test
    public void testPlantCatalogComprehensiveCropsAndTrees() {
        // Agricultural crops
        com.autofarm.mods.catalog.PlantSpecies wheat = com.autofarm.mods.catalog.PlantCatalog.resolve("Plant_Seeds_Wheat");
        Assertions.assertNotNull(wheat);
        Assertions.assertTrue(wheat.isCrop());
        Assertions.assertEquals("Plant_Crop_Wheat_Block", wheat.getBlockId());

        com.autofarm.mods.catalog.PlantSpecies potato = com.autofarm.mods.catalog.PlantCatalog.resolve("Plant_Seeds_Potato");
        Assertions.assertNotNull(potato);
        Assertions.assertTrue(potato.isCrop());
        Assertions.assertEquals("Plant_Crop_Potato_Block", potato.getBlockId());

        com.autofarm.mods.catalog.PlantSpecies tomato = com.autofarm.mods.catalog.PlantCatalog.resolve("Plant_Seeds_Tomato");
        Assertions.assertNotNull(tomato);
        Assertions.assertTrue(tomato.isCrop());

        // Silvicultural trees
        com.autofarm.mods.catalog.PlantSpecies oak = com.autofarm.mods.catalog.PlantCatalog.resolve("Plant_Sapling_Oak");
        Assertions.assertNotNull(oak);
        Assertions.assertTrue(oak.isTree());
        Assertions.assertEquals("Plant_Sapling_Oak", oak.getBlockId());
        Assertions.assertEquals("Wood_Oak_Trunk", oak.getTreeWoodTrunkId());

        com.autofarm.mods.catalog.PlantSpecies birch = com.autofarm.mods.catalog.PlantCatalog.resolve("birch_sapling");
        Assertions.assertNotNull(birch);
        Assertions.assertTrue(birch.isTree());
        Assertions.assertEquals("Plant_Sapling_Birch", birch.getBlockId());
        Assertions.assertEquals("Wood_Birch_Trunk", birch.getTreeWoodTrunkId());

        com.autofarm.mods.catalog.PlantSpecies redwood = com.autofarm.mods.catalog.PlantCatalog.resolve("Plant_Seeds_Redwood");
        Assertions.assertNotNull(redwood);
        Assertions.assertTrue(redwood.isTree());
        Assertions.assertEquals("Plant_Sapling_Redwood", redwood.getBlockId());
    }

    @Test
    public void testPlantCatalogDynamicResolution() {
        // Modded or unlisted crop
        com.autofarm.mods.catalog.PlantSpecies customCrop = com.autofarm.mods.catalog.PlantCatalog.resolve("mod_magic_seed");
        Assertions.assertNotNull(customCrop);
        Assertions.assertTrue(customCrop.isCrop());

        // Modded or unlisted tree sapling
        com.autofarm.mods.catalog.PlantSpecies customSapling = com.autofarm.mods.catalog.PlantCatalog.resolve("mystic_sapling");
        Assertions.assertNotNull(customSapling);
        Assertions.assertTrue(customSapling.isTree());
    }

    @Test
    public void testTreeSaplingBlockMappingInPlantingSystem() {
        Assertions.assertEquals("Plant_Sapling_Oak", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed("Plant_Sapling_Oak"));
        Assertions.assertEquals("Plant_Sapling_Birch", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed("Tree_Sapling_Birch"));
        Assertions.assertEquals("Plant_Sapling_Pine", 
                com.autofarm.mods.systems.PlantingSystem.getCropBlockForSeed("Plant_Seeds_Pine"));
    }

    @Test
    public void testLifeEssenceInCropDropsAndCompleteTreeDrops() {
        // Crop: Wheat
        com.autofarm.mods.catalog.PlantSpecies wheat = com.autofarm.mods.catalog.PlantCatalog.resolve("wheat");
        java.util.List<com.autofarm.mods.systems.HarvestSystem.DropEntry> cropDrops = 
                com.autofarm.mods.systems.HarvestSystem.calculateHarvestDropEntries(wheat, 0);

        boolean hasLifeEssence = cropDrops.stream().anyMatch(d -> "Ingredient_Life_Essence".equals(d.itemId()));
        Assertions.assertTrue(hasLifeEssence, "Crops must yield Ingredient_Life_Essence!");

        // Tree: Oak
        com.autofarm.mods.catalog.PlantSpecies oak = com.autofarm.mods.catalog.PlantCatalog.resolve("oak");
        java.util.List<com.autofarm.mods.systems.HarvestSystem.DropEntry> treeDrops = 
                com.autofarm.mods.systems.HarvestSystem.calculateHarvestDropEntries(oak, 6);

        boolean hasLogs = treeDrops.stream().anyMatch(d -> "Wood_Oak_Trunk".equals(d.itemId()));
        boolean hasSapling = treeDrops.stream().anyMatch(d -> "Plant_Sapling_Oak".equals(d.itemId()));
        boolean hasSticks = treeDrops.stream().anyMatch(d -> "Ingredient_Stick".equals(d.itemId()));
        boolean hasSap = treeDrops.stream().anyMatch(d -> "Ingredient_Tree_Sap".equals(d.itemId()));
        boolean hasFibre = treeDrops.stream().anyMatch(d -> "Ingredient_Fibre".equals(d.itemId()));
        boolean hasBark = treeDrops.stream().anyMatch(d -> "Ingredient_Bark".equals(d.itemId()));

        Assertions.assertTrue(hasLogs, "Tree must yield wood logs!");
        Assertions.assertTrue(hasSapling, "Tree must yield saplings for replanting!");
        Assertions.assertTrue(hasSticks, "Tree must drop sticks!");
        Assertions.assertTrue(hasSap, "Tree must drop tree sap!");
        Assertions.assertTrue(hasFibre, "Tree leaves must drop fibre!");
        Assertions.assertTrue(hasBark, "Tree must drop bark!");
    }

    @Test
    public void testTreeGrowthStagesAndFinalStageResolution() {
        // Verify stage resolution from real Hytale assets
        int oakFinalStage = com.autofarm.mods.systems.HarvestSystem.getFinalStageIndex("Plant_Sapling_Oak");
        Assertions.assertTrue(oakFinalStage >= 4, "Oak tree must have at least 5 stages (index >= 4)");

        int birchFinalStage = com.autofarm.mods.systems.HarvestSystem.getFinalStageIndex("Plant_Sapling_Birch");
        Assertions.assertTrue(birchFinalStage >= 4, "Birch tree must have at least 5 stages (index >= 4)");

        int appleFinalStage = com.autofarm.mods.systems.HarvestSystem.getFinalStageIndex("Plant_Sapling_Apple");
        Assertions.assertTrue(appleFinalStage >= 4, "Apple tree must have 5 stages (index 4)");
    }

    @Test
    public void testTreeGrowthDaysCalculationFromAssets() {
        // In headless tests without game assets loaded, it uses the config fallback (2.5 days).
        // In live game with assets loaded, it sums the duration of the stages.
        double oakDays = com.autofarm.mods.systems.HarvestSystem.getRequiredTreeGrowthDays("Plant_Sapling_Oak");
        Assertions.assertTrue(oakDays >= 2.0, "Oak tree growth days must be >= 2.0: " + oakDays);

        double fallbackDays = AutoFarmConfig.get().treeMaturityMinDays;
        Assertions.assertEquals(2.5, fallbackDays);
    }

    @Test
    public void testAutoPlantedComponentDayPersistence() {
        UUID farmId = UUID.randomUUID();
        Vector3i pos = new Vector3i(5, 60, 5);
        AutoPlantedComponent plant = new AutoPlantedComponent(farmId, "Plant_Sapling_Oak", 1000L, 3.5, pos);

        Assertions.assertEquals(3.5, plant.getPlantedDay());
        AutoPlantedComponent cloned = (AutoPlantedComponent) plant.clone();
        Assertions.assertEquals(3.5, cloned.getPlantedDay());
        Assertions.assertEquals(farmId, cloned.getFarmId());
    }

    @Test
    public void testTreeMaturityRejectsPrematureCuts() {
        // Null or mock chunk must safely reject maturity
        Assertions.assertFalse(com.autofarm.mods.systems.HarvestSystem.isTreeMature(null, null, null, null, null));
    }

    @Test
    public void testBlockFaceOffsetsAndDisplayNames() {
        Vector3i origin = new Vector3i(10, 50, 10);

        Vector3i up = ChestLinkSystem.BlockFace.UP.getOffset(origin);
        Assertions.assertEquals(new Vector3i(10, 51, 10), up);
        Assertions.assertEquals("Cima (+Y)", ChestLinkSystem.BlockFace.UP.getDisplayName());

        Vector3i down = ChestLinkSystem.BlockFace.DOWN.getOffset(origin);
        Assertions.assertEquals(new Vector3i(10, 49, 10), down);
        Assertions.assertEquals("Baixo (-Y)", ChestLinkSystem.BlockFace.DOWN.getDisplayName());

        Vector3i north = ChestLinkSystem.BlockFace.NORTH.getOffset(origin);
        Assertions.assertEquals(new Vector3i(10, 50, 9), north);
        Assertions.assertEquals("Norte (-Z)", ChestLinkSystem.BlockFace.NORTH.getDisplayName());

        Vector3i south = ChestLinkSystem.BlockFace.SOUTH.getOffset(origin);
        Assertions.assertEquals(new Vector3i(10, 50, 11), south);
        Assertions.assertEquals("Sul (+Z)", ChestLinkSystem.BlockFace.SOUTH.getDisplayName());

        Vector3i east = ChestLinkSystem.BlockFace.EAST.getOffset(origin);
        Assertions.assertEquals(new Vector3i(11, 50, 10), east);
        Assertions.assertEquals("Leste (+X)", ChestLinkSystem.BlockFace.EAST.getDisplayName());

        Vector3i west = ChestLinkSystem.BlockFace.WEST.getOffset(origin);
        Assertions.assertEquals(new Vector3i(9, 50, 10), west);
        Assertions.assertEquals("Oeste (-X)", ChestLinkSystem.BlockFace.WEST.getDisplayName());

        // Test fromString resolution
        Assertions.assertEquals(ChestLinkSystem.BlockFace.NORTH, ChestLinkSystem.BlockFace.fromString("NORTH"));
        Assertions.assertEquals(ChestLinkSystem.BlockFace.SOUTH, ChestLinkSystem.BlockFace.fromString("Sul"));
        Assertions.assertEquals(ChestLinkSystem.BlockFace.UP, ChestLinkSystem.BlockFace.fromString("Cima"));
        Assertions.assertNull(ChestLinkSystem.BlockFace.fromString("INVALID"));
    }

    @Test
    public void testSelectedChestFaceAndFacePosition() {
        AutoFarmBlockComponent farm = new AutoFarmBlockComponent(UUID.randomUUID(), new Vector3i(10, 60, 20));
        Assertions.assertEquals("AUTO", farm.getSelectedChestFace());

        farm.setSelectedChestFace("NORTH");
        Assertions.assertEquals("NORTH", farm.getSelectedChestFace());
        Assertions.assertEquals(new Vector3i(10, 60, 19), farm.getFaceBlockPosition("NORTH"));

        farm.setSelectedChestFace("UP");
        Assertions.assertEquals(new Vector3i(10, 61, 20), farm.getFaceBlockPosition("UP"));

        farm.setSelectedChestFace("DOWN");
        Assertions.assertEquals(new Vector3i(10, 59, 20), farm.getFaceBlockPosition("DOWN"));

        farm.setSelectedChestFace("EAST");
        Assertions.assertEquals(new Vector3i(11, 60, 20), farm.getFaceBlockPosition("EAST"));

        farm.setSelectedChestFace("WEST");
        Assertions.assertEquals(new Vector3i(9, 60, 20), farm.getFaceBlockPosition("WEST"));

        farm.setSelectedChestFace("AUTO");
        Assertions.assertNull(farm.getFaceBlockPosition("AUTO"));

        // Test cloning
        farm.setSelectedChestFace("SOUTH");
        AutoFarmBlockComponent clone = farm.clone();
        Assertions.assertEquals("SOUTH", clone.getSelectedChestFace());
    }

    @Test
    public void testChestLinkAdjacentConstraintNullSafety() {
        AutoFarmBlockComponent farm = new AutoFarmBlockComponent(UUID.randomUUID(), new Vector3i(0, 60, 0));
        farm.setSelectedChestFace("AUTO");
        Assertions.assertNull(ChestLinkSystem.findAdjacentChest(null, farm));
        Assertions.assertNull(ChestLinkSystem.findAdjacentChest(null, null));

        farm.setSelectedChestFace("NORTH");
        Assertions.assertNull(ChestLinkSystem.findAdjacentChest(null, farm));
    }
}
