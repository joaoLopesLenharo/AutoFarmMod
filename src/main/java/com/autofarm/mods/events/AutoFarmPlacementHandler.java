package com.autofarm.mods.events;

import com.autofarm.mods.AutoFarmMod;
import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.UUID;

public class AutoFarmPlacementHandler extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public AutoFarmPlacementHandler() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, PlaceBlockEvent event) {
        if (event == null || event.getItemInHand() == null) {
            return;
        }

        String itemId = event.getItemInHand().getItemId();
        if (itemId != null) {
            String lower = itemId.toLowerCase();
            if (lower.contains("autotreefarm_block")) {
                Vector3i targetPos = new Vector3i(event.getTargetBlock());
                UUID farmId = UUID.randomUUID();

                com.autofarm.mods.components.AutoTreeFarmBlockComponent comp = 
                        new com.autofarm.mods.components.AutoTreeFarmBlockComponent(farmId, targetPos);
                Holder<EntityStore> holder = store.getRegistry().newHolder();
                holder.addComponent(AutoFarmMod.AUTO_TREE_FARM_COMPONENT_TYPE, comp);

                if (buffer != null) {
                    buffer.addEntity(holder, AddReason.SPAWN);
                } else {
                    store.addEntity(holder, AddReason.SPAWN);
                }
                AutoFarmMod.registerTreeFarm(comp);

                System.out.println("[AutoTreeFarm] AutoTreeFarm Block placed at " + targetPos + " | FarmId: " + farmId);
            } else if (lower.contains("autofarm_block")) {
                Vector3i targetPos = new Vector3i(event.getTargetBlock());
                UUID farmId = UUID.randomUUID();

                AutoFarmBlockComponent comp = new AutoFarmBlockComponent(farmId, targetPos);
                Holder<EntityStore> holder = store.getRegistry().newHolder();
                holder.addComponent(AutoFarmMod.AUTO_FARM_COMPONENT_TYPE, comp);

                if (buffer != null) {
                    buffer.addEntity(holder, AddReason.SPAWN);
                } else {
                    store.addEntity(holder, AddReason.SPAWN);
                }
                AutoFarmMod.registerFarm(comp);

                System.out.println("[AutoFarm] AutoFarm Block placed at " + targetPos + " | FarmId: " + farmId);
            }
        }
    }
}
