package com.autofarm.mods.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlantCatalog {

    private static final Map<String, PlantSpecies> REGISTRY = new ConcurrentHashMap<>();
    private static final List<PlantSpecies> ALL_SPECIES = new ArrayList<>();

    static {
        // ==========================================
        // 1. CROPS (Agricultural - Requires Tilled Soil)
        // ==========================================
        registerCrop("wheat", "Wheat", "Plant_Crop_Wheat_Item", 2, 1);
        registerCrop("carrot", "Carrot", "Plant_Crop_Carrot_Item", 2, 1);
        registerCrop("corn", "Corn", "Plant_Crop_Corn_Item", 2, 1);
        registerCrop("potato", "Potato", "Plant_Crop_Potato_Item", 2, 1);
        registerCrop("tomato", "Tomato", "Plant_Crop_Tomato_Item", 2, 1);
        registerCrop("onion", "Onion", "Plant_Crop_Onion_Item", 2, 1);
        registerCrop("lettuce", "Lettuce", "Plant_Crop_Lettuce_Item", 2, 1);
        registerCrop("pumpkin", "Pumpkin", "Plant_Crop_Pumpkin_Item", 1, 1);
        registerCrop("turnip", "Turnip", "Plant_Crop_Turnip_Item", 2, 1);
        registerCrop("rice", "Rice", "Plant_Crop_Rice_Item", 2, 1);
        registerCrop("cauliflower", "Cauliflower", "Plant_Crop_Cauliflower_Item", 1, 1);
        registerCrop("chilli", "Chilli", "Plant_Crop_Chilli_Item", 2, 1);
        registerCrop("aubergine", "Aubergine", "Plant_Crop_Aubergine_Item", 2, 1);
        registerCrop("cotton", "Cotton", "Plant_Crop_Cotton_Item", 2, 1);
        registerCrop("berry", "Berry", "Plant_Crop_Berry_Block", 2, 1);
        registerCrop("mushroom", "Mushroom", "Plant_Crop_Mushroom_Block", 2, 1);

        // Alchemical / Potion Crops
        registerCrop("health1", "Health1", "Plant_Crop_Health1", 2, 1);
        registerCrop("health2", "Health2", "Plant_Crop_Health2", 2, 1);
        registerCrop("health3", "Health3", "Plant_Crop_Health3", 2, 1);
        registerCrop("mana1", "Mana1", "Plant_Crop_Mana1", 2, 1);
        registerCrop("mana2", "Mana2", "Plant_Crop_Mana2", 2, 1);
        registerCrop("mana3", "Mana3", "Plant_Crop_Mana3", 2, 1);
        registerCrop("stamina1", "Stamina1", "Plant_Crop_Stamina1", 2, 1);
        registerCrop("stamina2", "Stamina2", "Plant_Crop_Stamina2", 2, 1);
        registerCrop("stamina3", "Stamina3", "Plant_Crop_Stamina3", 2, 1);

        // ==========================================
        // 2. TREES (Silvicultural - Dirt/Grass, Vertical Clearance)
        // ==========================================
        String[] treeSpecies = {
            "Oak", "Birch", "Pine", "Redwood", "Apple", "Amber", "Ash", "Aspen",
            "Azure", "Bamboo", "Banyan", "Beech", "Bottletree", "Camphor", "Cedar",
            "Crystal", "Dry", "Fig_Blue", "Fir", "Fire", "Frostwood", "Gumboab",
            "Ice", "Jungle", "Maple", "Palm", "Palo", "Petrified", "Poisoned",
            "Sallow", "Spiral", "Spruce", "Stormbark", "Willow", "Windwillow", "Wisteria_Wild"
        };

        for (String tree : treeSpecies) {
            registerTree(tree.toLowerCase(Locale.ROOT), tree);
        }
    }

    private static void registerCrop(String key, String displayName, String harvestItem, int harvestQty, int seedQty) {
        List<String> seeds = new ArrayList<>();
        seeds.add("Plant_Seeds_" + displayName);
        seeds.add("Plant_Seeds_" + displayName + "_Eternal");
        seeds.add("Crop_" + displayName + "_Seed");
        seeds.add(displayName.toLowerCase(Locale.ROOT) + "_seed");

        String blockId = "Plant_Crop_" + displayName + "_Block";

        PlantSpecies species = new PlantSpecies(
                key,
                PlantSpecies.PlantCategory.CROP,
                seeds,
                blockId,
                harvestItem,
                harvestQty,
                seedQty,
                null
        );

        REGISTRY.put(key, species);
        ALL_SPECIES.add(species);
        for (String s : seeds) {
            REGISTRY.put(s.toLowerCase(Locale.ROOT), species);
        }
        REGISTRY.put(blockId.toLowerCase(Locale.ROOT), species);
        REGISTRY.put((blockId + "_Eternal").toLowerCase(Locale.ROOT), species);
    }

    private static void registerTree(String key, String treeName) {
        List<String> seeds = new ArrayList<>();
        seeds.add("Plant_Sapling_" + treeName);
        seeds.add("Plant_Seeds_" + treeName);
        seeds.add("Tree_Sapling_" + treeName);
        seeds.add(treeName.toLowerCase(Locale.ROOT) + "_sapling");

        String blockId = "Plant_Sapling_" + treeName;
        String trunkId = "Wood_" + treeName + "_Trunk";

        PlantSpecies species = new PlantSpecies(
                key,
                PlantSpecies.PlantCategory.TREE,
                seeds,
                blockId,
                trunkId, // Harvest gives trunk logs
                4,       // Yields 4 wood logs
                1,       // Drops 1 sapling for replanting
                trunkId
        );

        REGISTRY.put(key, species);
        ALL_SPECIES.add(species);
        for (String s : seeds) {
            REGISTRY.put(s.toLowerCase(Locale.ROOT), species);
        }
        REGISTRY.put(blockId.toLowerCase(Locale.ROOT), species);
        REGISTRY.put(trunkId.toLowerCase(Locale.ROOT), species);
    }

    /**
     * Resolves a PlantSpecies from a seed item ID or block ID.
     * If not explicitly listed, dynamically infers whether it is a crop or tree.
     */
    public static PlantSpecies resolve(String identifier) {
        if (identifier == null) {
            return getDefaultCrop();
        }

        String lower = identifier.toLowerCase(Locale.ROOT);
        PlantSpecies found = REGISTRY.get(lower);
        if (found != null) {
            return found;
        }

        // Dynamic heuristic matching for custom or unlisted modded items
        if (lower.contains("sapling") || lower.contains("tree") || lower.contains("wood_")) {
            String name = extractCleanName(lower, "sapling", "tree", "plant_", "wood_", "_trunk", "seeds_");
            String capName = capitalize(name);
            String saplingBlock = "Plant_Sapling_" + capName;
            String trunkBlock = "Wood_" + capName + "_Trunk";
            PlantSpecies dynamicTree = new PlantSpecies(
                    lower,
                    PlantSpecies.PlantCategory.TREE,
                    List.of(identifier, saplingBlock),
                    saplingBlock,
                    trunkBlock,
                    4,
                    1,
                    trunkBlock
            );
            REGISTRY.put(lower, dynamicTree);
            return dynamicTree;
        }

        if (lower.contains("seed") || lower.contains("crop") || lower.contains("plant_")) {
            String name = extractCleanName(lower, "plant_seeds_", "plant_crop_", "_block", "_item", "_eternal", "seed", "crop");
            String capName = capitalize(name);
            String cropBlock = "Plant_Crop_" + capName + "_Block";
            String cropItem = "Plant_Crop_" + capName + "_Item";
            PlantSpecies dynamicCrop = new PlantSpecies(
                    lower,
                    PlantSpecies.PlantCategory.CROP,
                    List.of(identifier),
                    cropBlock,
                    cropItem,
                    2,
                    1,
                    null
            );
            REGISTRY.put(lower, dynamicCrop);
            return dynamicCrop;
        }

        return getDefaultCrop();
    }

    /**
     * Checks whether an itemId represents any plantable seed, sapling, or sprout.
     */
    public static boolean isPlantable(String itemId) {
        if (itemId == null) return false;
        String lower = itemId.toLowerCase(Locale.ROOT);
        if (REGISTRY.containsKey(lower)) return true;

        return lower.contains("seed")
                || lower.contains("sapling")
                || lower.contains("sprout")
                || lower.contains("plant_")
                || lower.contains("crop");
    }

    public static PlantSpecies getDefaultCrop() {
        return REGISTRY.get("wheat");
    }

    public static List<PlantSpecies> getAllRegisteredSpecies() {
        return Collections.unmodifiableList(ALL_SPECIES);
    }

    private static String extractCleanName(String input, String... prefixesAndSuffixes) {
        String res = input;
        for (String p : prefixesAndSuffixes) {
            res = res.replace(p, "");
        }
        res = res.replace("_", " ").trim();
        if (res.isEmpty()) return "Unknown";
        return res.split("\\s+")[0];
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return "Plant";
        return Character.toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
    }
}
