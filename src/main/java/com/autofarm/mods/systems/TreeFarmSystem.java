package com.autofarm.mods.systems;

import com.autofarm.mods.AutoFarmMod;
import com.autofarm.mods.catalog.PlantCatalog;
import com.autofarm.mods.catalog.PlantSpecies;
import com.autofarm.mods.components.AutoTreeFarmBlockComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.Random;
import java.util.UUID;

public class TreeFarmSystem extends EntityTickingSystem<EntityStore> {

    private long globalTickCounter = 0;
    private final Random random = new Random();

    @Override
    public Query<EntityStore> getQuery() {
        if (AutoFarmMod.AUTO_TREE_FARM_COMPONENT_TYPE != null) {
            return AutoFarmMod.AUTO_TREE_FARM_COMPONENT_TYPE;
        }
        return Query.any();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        AutoTreeFarmBlockComponent farm = null;
        if (AutoFarmMod.AUTO_TREE_FARM_COMPONENT_TYPE != null) {
            farm = chunk.getComponent(index, AutoFarmMod.AUTO_TREE_FARM_COMPONENT_TYPE);
        }

        if (farm == null) {
            return;
        }

        // Clean up entity if the tree farm block was removed
        if (AutoFarmMod.findTreeFarmAt(farm.getPosition()) == null) {
            if (buffer != null) {
                buffer.removeEntity(chunk.getReferenceTo(index), com.hypixel.hytale.component.RemoveReason.REMOVE);
            }
            return;
        }

        World world = null;
        if (store != null && store.getExternalData() instanceof EntityStore entityStore) {
            world = entityStore.getWorld();
        }

        runTreeFarmCycle(world, farm);
    }

    public void runTreeFarmCycle(World world, AutoTreeFarmBlockComponent farm) {
        if (world == null || farm == null) {
            return;
        }

        long currentTick = ++globalTickCounter;
        long interval = farm.getProductionIntervalTicks();
        if (currentTick - farm.getLastProductionTick() < interval) {
            return;
        }

        UUID farmId = farm.getFarmId();
        Vector3i farmPos = farm.getPosition();

        // 1. Mandatory requirement: A block of dirt directly underneath (y - 1)
        Vector3i dirtPos = new Vector3i(farmPos.x, farmPos.y - 1, farmPos.z);
        long chunkIdx = ChunkUtil.indexChunkFromBlock(dirtPos.x, dirtPos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
        if (chunk == null) {
            return;
        }

        BlockType soilType = chunk.getBlockType(dirtPos.x, dirtPos.y, dirtPos.z);
        boolean hasDirt = soilType != null 
                && TerraformSystem.isTillableSoilOrTilled(soilType.getId()) 
                && !TerraformSystem.isWaterAt(world, dirtPos.x, dirtPos.y, dirtPos.z);
        farm.setHasDirtUnderneath(hasDirt);

        if (!hasDirt) {
            if (currentTick % (interval * 4) == 0) {
                System.out.println("[AutoTreeFarm-CYCLE] Tree Farm " + farmId + " at " + farmPos 
                        + " is IDLE (requer um bloco de terra diretamente abaixo do bloco).");
            }
            return;
        }

        // 2. Mandatory requirement: Linked chest touching the block
        Vector3i linkedChest = farm.getLinkedChestPosition();
        if (linkedChest != null && !ChestLinkSystem.isChestValid(world, linkedChest)) {
            farm.setLinkedChestPosition(null);
            linkedChest = null;
        }

        if (linkedChest == null) {
            linkedChest = findAdjacentChestForTreeFarm(world, farm);
            if (linkedChest != null) {
                farm.setLinkedChestPosition(linkedChest);
                System.out.println("[AutoTreeFarm-CYCLE] Found and linked chest at " + linkedChest 
                        + " (Face: " + farm.getSelectedChestFace() + ")");
            }
        }

        if (linkedChest == null) {
            if (currentTick % (interval * 4) == 0) {
                System.out.println("[AutoTreeFarm-CYCLE] Tree Farm " + farmId + " at " + farmPos 
                        + " is IDLE (aguardando baú encostado no bloco. Face: " + farm.getSelectedChestFace() + ").");
            }
            return;
        }

        // 3. Sapling requirement: Needs a sapling active in the block
        String saplingId = farm.getSaplingItemId();
        if (saplingId == null || saplingId.isEmpty()) {
            // Withdraw 1 tree sapling from linked chest
            saplingId = ChestLinkSystem.withdrawPlantable(world, linkedChest, false, true);
            if (saplingId != null) {
                farm.setSaplingItemId(saplingId);
                System.out.println("[AutoTreeFarm-CYCLE] Tree Farm " + farmId + ": Loaded sapling [" + saplingId + "] from chest.");
            } else {
                if (currentTick % (interval * 4) == 0) {
                    System.out.println("[AutoTreeFarm-CYCLE] Tree Farm " + farmId + " at " + farmPos 
                            + " is IDLE (aguardando muda de árvore / sapling no baú encostado).");
                }
                return;
            }
        }

        // 4. Production cycle: Generate a small portion of normal tree drops
        farm.setLastProductionTick(currentTick);
        PlantSpecies species = PlantCatalog.resolve(saplingId);
        String woodId = (species != null && species.getTreeWoodTrunkId() != null) 
                ? species.getTreeWoodTrunkId() 
                : "Wood_Oak_Trunk";

        // Primary drop: 1 to 2 wood trunk logs
        int woodQty = 1 + random.nextInt(2);
        ChestLinkSystem.deposit(world, linkedChest, new ItemStack(woodId, woodQty));

        // Secondary drops: stick, tree sap, bark, or foliage fiber
        String[] secondaries = {
                HarvestSystem.STICK_ITEM,
                HarvestSystem.TREE_SAP_ITEM,
                HarvestSystem.TREE_BARK_ITEM,
                HarvestSystem.PLANT_FIBRE_ITEM
        };
        String secondary = secondaries[random.nextInt(secondaries.length)];
        ChestLinkSystem.deposit(world, linkedChest, new ItemStack(secondary, 1));

        // Occasional extra sapling drop (15% chance)
        if (random.nextFloat() < 0.15f) {
            ChestLinkSystem.deposit(world, linkedChest, new ItemStack(saplingId, 1));
        }

        System.out.println("[AutoTreeFarm-PRODUCE] Tree Farm " + farmId + " produced " 
                + woodQty + "x " + woodId + " + 1x " + secondary + " into chest at " + linkedChest);
    }

    public static Vector3i findAdjacentChestForTreeFarm(World world, AutoTreeFarmBlockComponent farm) {
        if (world == null || farm == null) return null;
        Vector3i origin = farm.getPosition();
        String selected = farm.getSelectedChestFace();

        if (selected != null && !selected.equalsIgnoreCase("AUTO")) {
            ChestLinkSystem.BlockFace face = ChestLinkSystem.BlockFace.fromString(selected);
            if (face != null) {
                Vector3i candidate = face.getOffset(origin);
                if (ChestLinkSystem.isChestValid(world, candidate)) {
                    return candidate;
                }
                return null;
            }
        }

        // AUTO: check all 6 faces
        for (ChestLinkSystem.BlockFace face : ChestLinkSystem.BlockFace.values()) {
            Vector3i candidate = face.getOffset(origin);
            if (ChestLinkSystem.isChestValid(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
