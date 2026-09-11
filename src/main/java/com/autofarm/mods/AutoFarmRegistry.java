package com.autofarm.mods;

import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.autofarm.mods.components.AutoPlantedComponent;
import com.autofarm.mods.components.TilledByFarmComponent;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoFarmRegistry {

    private static final Map<UUID, AutoFarmBlockComponent> ACTIVE_FARMS = new ConcurrentHashMap<>();
    private static final Map<Vector3i, AutoPlantedComponent> ACTIVE_CROPS = new ConcurrentHashMap<>();
    private static final Map<Vector3i, TilledByFarmComponent> TILLED_SOIL = new ConcurrentHashMap<>();

    public static void registerFarm(AutoFarmBlockComponent farm) {
        if (farm != null && farm.getFarmId() != null) {
            ACTIVE_FARMS.put(farm.getFarmId(), farm);
        }
    }

    public static void unregisterFarm(UUID farmId) {
        if (farmId == null) return;
        ACTIVE_FARMS.remove(farmId);
        // INVIOLABLE RULE: When farm is destroyed, unlink any ownership markers
        ACTIVE_CROPS.values().removeIf(crop -> farmId.equals(crop.getFarmId()));
        TILLED_SOIL.values().removeIf(soil -> farmId.equals(soil.getFarmId()));
    }

    public static AutoFarmBlockComponent findFarmAt(Vector3i pos) {
        if (pos == null) return null;
        for (AutoFarmBlockComponent farm : ACTIVE_FARMS.values()) {
            if (pos.equals(farm.getPosition())) {
                return farm;
            }
        }
        return null;
    }

    public static Collection<AutoFarmBlockComponent> getActiveFarms() {
        return ACTIVE_FARMS.values();
    }

    public static int countActivePlantsForFarm(UUID farmId) {
        if (farmId == null) return 0;
        int count = 0;
        for (AutoPlantedComponent crop : ACTIVE_CROPS.values()) {
            if (farmId.equals(crop.getFarmId())) {
                count++;
            }
        }
        return count;
    }

    public static void registerPlantedCrop(Vector3i pos, AutoPlantedComponent crop) {
        if (pos != null && crop != null) {
            ACTIVE_CROPS.put(pos, crop);
        }
    }

    public static AutoPlantedComponent getPlantedCrop(Vector3i pos) {
        return pos != null ? ACTIVE_CROPS.get(pos) : null;
    }

    public static void registerTilledSoil(Vector3i pos, TilledByFarmComponent soil) {
        if (pos != null && soil != null) {
            TILLED_SOIL.put(pos, soil);
        }
    }

    public static TilledByFarmComponent getTilledSoil(Vector3i pos) {
        return pos != null ? TILLED_SOIL.get(pos) : null;
    }

    public static void removePlantedCrop(Vector3i pos) {
        if (pos != null) {
            ACTIVE_CROPS.remove(pos);
        }
    }

    public static Map<Vector3i, AutoPlantedComponent> getCropsForFarm(UUID farmId) {
        Map<Vector3i, AutoPlantedComponent> result = new ConcurrentHashMap<>();
        if (farmId == null) return result;
        for (Map.Entry<Vector3i, AutoPlantedComponent> entry : ACTIVE_CROPS.entrySet()) {
            if (farmId.equals(entry.getValue().getFarmId())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static void removeTilledSoil(Vector3i pos) {
        if (pos != null) {
            TILLED_SOIL.remove(pos);
        }
    }

    private static final Map<UUID, com.autofarm.mods.components.AutoTreeFarmBlockComponent> ACTIVE_TREE_FARMS = new ConcurrentHashMap<>();

    public static void registerTreeFarm(com.autofarm.mods.components.AutoTreeFarmBlockComponent farm) {
        if (farm != null && farm.getFarmId() != null) {
            ACTIVE_TREE_FARMS.put(farm.getFarmId(), farm);
        }
    }

    public static void unregisterTreeFarm(UUID farmId) {
        if (farmId == null) return;
        ACTIVE_TREE_FARMS.remove(farmId);
    }

    public static com.autofarm.mods.components.AutoTreeFarmBlockComponent findTreeFarmAt(Vector3i pos) {
        if (pos == null) return null;
        for (com.autofarm.mods.components.AutoTreeFarmBlockComponent farm : ACTIVE_TREE_FARMS.values()) {
            if (pos.equals(farm.getPosition())) {
                return farm;
            }
        }
        return null;
    }

    public static Collection<com.autofarm.mods.components.AutoTreeFarmBlockComponent> getActiveTreeFarms() {
        return ACTIVE_TREE_FARMS.values();
    }

    public static void clearAll() {
        ACTIVE_FARMS.clear();
        ACTIVE_TREE_FARMS.clear();
        ACTIVE_CROPS.clear();
        TILLED_SOIL.clear();
    }
}
