package com.autofarm.mods.systems;

import com.autofarm.mods.AutoFarmConfig;
import com.autofarm.mods.AutoFarmMod;
import com.autofarm.mods.AutoFarmRegistry;
import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.UUID;

public class FarmScanSystem extends EntityTickingSystem<EntityStore> {

    private long globalTickCounter = 0;

    @Override
    public Query<EntityStore> getQuery() {
        if (AutoFarmMod.AUTO_FARM_COMPONENT_TYPE != null) {
            return AutoFarmMod.AUTO_FARM_COMPONENT_TYPE;
        }
        return Query.any();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk, 
                     Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        AutoFarmBlockComponent farm = null;
        if (AutoFarmMod.AUTO_FARM_COMPONENT_TYPE != null) {
            farm = chunk.getComponent(index, AutoFarmMod.AUTO_FARM_COMPONENT_TYPE);
        }

        if (farm == null) {
            return;
        }

        // Clean up entity if the farm was unregistered or destroyed
        if (AutoFarmRegistry.findFarmAt(farm.getPosition()) == null) {
            if (buffer != null) {
                buffer.removeEntity(chunk.getReferenceTo(index), com.hypixel.hytale.component.RemoveReason.REMOVE);
            }
            return;
        }

        World world = null;
        if (store != null && store.getExternalData() instanceof EntityStore entityStore) {
            world = entityStore.getWorld();
        }

        runAutonomousCycle(world, store, buffer, farm);
    }

    public void runAutonomousCycle(World world, AutoFarmBlockComponent farm) {
        runAutonomousCycle(world, null, null, farm);
    }

    /**
     * Complete autonomous operation cycle for a single AutoFarm Block:
     * 1. Revalidates or discovers linked chest.
     * 2. Executes Harvest Cycle (harvests mature crops and trees, vacuums drops into chest).
     * 3. Executes Planting & Terraforming Cycle (tills soil, places crops or tree saplings).
     * 4. Reports diagnostics.
     */
    public void runAutonomousCycle(World world, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, AutoFarmBlockComponent farm) {
        long currentTick = ++globalTickCounter;
        AutoFarmConfig config = AutoFarmConfig.get();

        long interval = farm.getScanIntervalTicks() > 0 ? farm.getScanIntervalTicks() : config.scanIntervalTicks;
        if (currentTick - farm.getLastScanTick() < interval) {
            return;
        }
        farm.setLastScanTick(currentTick);

        UUID farmId = farm.getFarmId();
        Vector3i farmPos = farm.getPosition();
        int range = farm.getRange() > 0 ? farm.getRange() : config.horizontalRange;
        int vRange = farm.getVerticalRange() > 0 ? farm.getVerticalRange() : config.verticalRange;

        // 1. Revalidate / Find Linked Chest touching the configured face
        Vector3i linkedChest = farm.getLinkedChestPosition();
        if (linkedChest != null && world != null && !ChestLinkSystem.isChestValid(world, linkedChest)) {
            System.out.println("[AutoFarm-CYCLE] Linked chest at " + linkedChest + " was destroyed or removed. Resetting link.");
            farm.setLinkedChestPosition(null);
            linkedChest = null;
        }

        if (linkedChest == null && world != null) {
            linkedChest = ChestLinkSystem.findAdjacentChest(world, farm);
            if (linkedChest != null) {
                farm.setLinkedChestPosition(linkedChest);
                System.out.println("[AutoFarm-CYCLE] Found and linked adjacent chest at " + linkedChest + " (Face: " + farm.getSelectedChestFace() + ")");
            }
        }

        if (linkedChest == null) {
            // Farm cannot operate without an adjacent chest
            System.out.println("[AutoFarm-CYCLE] Farm " + farmId + " at " + farmPos + " is IDLE (aguardando baú encostado no bloco. Face configurada: " + farm.getSelectedChestFace() + ").");
            return;
        }

        // 2. Check for water puddle directly underneath the machine
        boolean hasWater = world == null || TerraformSystem.hasWaterUnderneath(world, farmPos);
        if (!hasWater && currentTick % (interval * 5) == 0) {
            System.out.println("[AutoFarm-CYCLE] Farm " + farmId + " at " + farmPos + " is IDLE (aguardando poça d'água diretamente abaixo da máquina para irrigação).");
        }

        // 3. Run Harvest Cycle (harvests only truly mature crops and fully grown trees)
        int harvested = 0;
        if (world != null) {
            harvested = HarvestSystem.processHarvestCycle(world, store, buffer, farm, currentTick);
        }

        // 4. Run Planting & Terraforming Cycle (requires water puddle underneath to irrigate)
        int planted = 0;
        if (world != null && hasWater) {
            planted = PlantingSystem.processPlantingCycle(world, farm, currentTick, config.maxPlantBatchPerCycle);
        }

        // 5. Status summary
        int activePlants = AutoFarmRegistry.countActivePlantsForFarm(farmId);
        if (harvested > 0 || planted > 0 || currentTick % (interval * 5) == 0) {
            System.out.println("[AutoFarm-CYCLE] Farm " + farmId + " status: " 
                    + activePlants + "/" + farm.getMaxActivePlantsPerFarm() + " active plants | "
                    + "Harvested: " + harvested + " | Planted: " + planted + " | Chest: " + linkedChest
                    + " | Water: " + (hasWater ? "OK" : "MISSING UNDER MACHINE"));
        }
    }
}
