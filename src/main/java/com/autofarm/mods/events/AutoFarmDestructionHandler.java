package com.autofarm.mods.events;

import com.autofarm.mods.AutoFarmMod;
import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

public class AutoFarmDestructionHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public AutoFarmDestructionHandler() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, BreakBlockEvent event) {
        if (event == null || event.getBlockType() == null) {
            return;
        }

        String blockId = event.getBlockType().getId();
        if (blockId != null && blockId.toLowerCase().contains("autofarm_block")) {
            Vector3i targetPos = event.getTargetBlock();
            AutoFarmBlockComponent farm = AutoFarmMod.findFarmAt(targetPos);
            if (farm != null) {
                AutoFarmMod.unregisterFarm(farm.getFarmId());
                System.out.println("[AutoFarm] AutoFarm Block destroyed at " + targetPos + " (FarmId: " + farm.getFarmId() + "). Unlinked all associated farm metadata.");
            }
        }
    }
}
