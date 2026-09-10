package com.autofarm.mods.catalog;

import java.util.List;

public class PlantSpecies {

    public enum PlantCategory {
        CROP,
        TREE
    }

    private final String id;
    private final PlantCategory category;
    private final List<String> seedItemIds;
    private final String blockId;
    private final String harvestItemId;
    private final int defaultHarvestQty;
    private final int defaultSeedQty;
    private final String treeWoodTrunkId;

    public PlantSpecies(String id, PlantCategory category, List<String> seedItemIds, 
                        String blockId, String harvestItemId, int defaultHarvestQty, 
                        int defaultSeedQty, String treeWoodTrunkId) {
        this.id = id;
        this.category = category;
        this.seedItemIds = seedItemIds;
        this.blockId = blockId;
        this.harvestItemId = harvestItemId;
        this.defaultHarvestQty = defaultHarvestQty;
        this.defaultSeedQty = defaultSeedQty;
        this.treeWoodTrunkId = treeWoodTrunkId;
    }

    public String getId() {
        return id;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public boolean isTree() {
        return category == PlantCategory.TREE;
    }

    public boolean isCrop() {
        return category == PlantCategory.CROP;
    }

    public List<String> getSeedItemIds() {
        return seedItemIds;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getHarvestItemId() {
        return harvestItemId;
    }

    public int getDefaultHarvestQty() {
        return defaultHarvestQty;
    }

    public int getDefaultSeedQty() {
        return defaultSeedQty;
    }

    public String getTreeWoodTrunkId() {
        return treeWoodTrunkId;
    }

    public boolean matchesSeed(String itemId) {
        if (itemId == null) return false;
        for (String seed : seedItemIds) {
            if (seed.equalsIgnoreCase(itemId)) {
                return true;
            }
        }
        return false;
    }
}
