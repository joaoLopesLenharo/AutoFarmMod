package com.autofarm.mods;

import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.autofarm.mods.components.AutoPlantedComponent;
import com.autofarm.mods.components.TilledByFarmComponent;
import com.autofarm.mods.events.AutoFarmDestructionHandler;
import com.autofarm.mods.events.AutoFarmPlacementHandler;
import com.autofarm.mods.systems.FarmScanSystem;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoFarmMod extends JavaPlugin {

    public static ComponentType<EntityStore, AutoFarmBlockComponent> AUTO_FARM_COMPONENT_TYPE;
    public static ComponentType<EntityStore, com.autofarm.mods.components.AutoTreeFarmBlockComponent> AUTO_TREE_FARM_COMPONENT_TYPE;
    public static ComponentType<EntityStore, AutoPlantedComponent> AUTO_PLANTED_COMPONENT_TYPE;
    public static ComponentType<EntityStore, TilledByFarmComponent> TILLED_BY_FARM_COMPONENT_TYPE;

    private static AutoFarmMod instance;

    public AutoFarmMod(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static AutoFarmMod getInstance() {
        return instance;
    }

    @Override
    public void setup() {
        super.setup();
        System.out.println("=================================================");
        System.out.println("[AutoFarmMod] Initializing AutoFarm Plugin v1.0.0");

        try {
            ComponentRegistryProxy<EntityStore> registry = this.getEntityStoreRegistry();

            // 1. Register ECS Components
            AUTO_FARM_COMPONENT_TYPE = registry.registerComponent(
                    AutoFarmBlockComponent.class,
                    "autofarm_data",
                    AutoFarmBlockComponent.CODEC
            );

            AUTO_TREE_FARM_COMPONENT_TYPE = registry.registerComponent(
                    com.autofarm.mods.components.AutoTreeFarmBlockComponent.class,
                    "autotreefarm_data",
                    com.autofarm.mods.components.AutoTreeFarmBlockComponent.CODEC
            );

            AUTO_PLANTED_COMPONENT_TYPE = registry.registerComponent(
                    AutoPlantedComponent.class,
                    "autofarm_planted",
                    AutoPlantedComponent.CODEC
            );

            TILLED_BY_FARM_COMPONENT_TYPE = registry.registerComponent(
                    TilledByFarmComponent.class,
                    "autofarm_tilled",
                    TilledByFarmComponent.CODEC
            );

            System.out.println("[AutoFarmMod] Registered ECS Components successfully.");

            // 2. Register Systems & Event Handlers
            registry.registerSystem(new AutoFarmPlacementHandler());
            registry.registerSystem(new AutoFarmDestructionHandler());
            registry.registerSystem(new FarmScanSystem());
            registry.registerSystem(new com.autofarm.mods.systems.TreeFarmSystem());

            // 3. Register Hytale Built-in 'F' Key Custom UI Interactions
            try {
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.registerCustomPageSupplier(
                        this,
                        com.autofarm.mods.ui.AutoFarmConfigPage.class,
                        "AutoFarm_Config",
                        (playerEntityRef, accessor, playerRef, ctx) -> {
                            Vector3i pos = null;
                            if (ctx != null && ctx.getTargetBlock() != null) {
                                com.hypixel.hytale.protocol.BlockPosition bp = ctx.getTargetBlock();
                                pos = new Vector3i(bp.x, bp.y, bp.z);
                            }
                            com.hypixel.hytale.server.core.universe.world.World world = null;
                            if (playerRef != null && playerRef.getWorldUuid() != null) {
                                world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
                            }
                            if (world == null && accessor != null && accessor.getExternalData() instanceof EntityStore es) {
                                world = es.getWorld();
                            }
                            AutoFarmBlockComponent farm = pos != null ? findFarmAt(pos) : null;
                            if (farm == null && pos != null) {
                                farm = new AutoFarmBlockComponent(UUID.randomUUID(), pos);
                                registerFarm(farm);
                            }
                            return new com.autofarm.mods.ui.AutoFarmConfigPage(playerRef, farm, world);
                        }
                );
                System.out.println("[AutoFarmMod] Registered OpenCustomUIInteraction for 'AutoFarm_Config'.");

                com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.registerCustomPageSupplier(
                        this,
                        com.autofarm.mods.ui.AutoTreeFarmConfigPage.class,
                        "AutoTreeFarm_Config",
                        (playerEntityRef, accessor, playerRef, ctx) -> {
                            Vector3i pos = null;
                            if (ctx != null && ctx.getTargetBlock() != null) {
                                com.hypixel.hytale.protocol.BlockPosition bp = ctx.getTargetBlock();
                                pos = new Vector3i(bp.x, bp.y, bp.z);
                            }
                            com.hypixel.hytale.server.core.universe.world.World world = null;
                            if (playerRef != null && playerRef.getWorldUuid() != null) {
                                world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
                            }
                            if (world == null && accessor != null && accessor.getExternalData() instanceof EntityStore es) {
                                world = es.getWorld();
                            }
                            com.autofarm.mods.components.AutoTreeFarmBlockComponent farm = pos != null ? findTreeFarmAt(pos) : null;
                            if (farm == null && pos != null) {
                                farm = new com.autofarm.mods.components.AutoTreeFarmBlockComponent(UUID.randomUUID(), pos);
                                registerTreeFarm(farm);
                            }
                            return new com.autofarm.mods.ui.AutoTreeFarmConfigPage(playerRef, farm, world);
                        }
                );
                System.out.println("[AutoFarmMod] Registered OpenCustomUIInteraction for 'AutoTreeFarm_Config'.");
            } catch (Throwable t) {
                System.err.println("[AutoFarmMod] Warning: could not register OpenCustomUIInteraction: " + t.getMessage());
                t.printStackTrace();
            }

            // 4. Register Player Block Interaction (fallback / right-click support)
            this.getEventRegistry().registerGlobal(com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent.class, this::onPlayerInteract);

            System.out.println("[AutoFarmMod] Registered Placement/Destruction Handlers, FarmScanSystem, TreeFarmSystem, and UI Handlers.");
        } catch (Exception e) {
            System.err.println("[AutoFarmMod] Error during setup: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[AutoFarmMod] AutoFarm Plugin setup complete!");
        System.out.println("=================================================");
    }

    private void onPlayerInteract(com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent event) {
        if (event == null) {
            return;
        }

        Vector3i targetBlock = event.getTargetBlock();
        if (targetBlock == null) {
            return;
        }

        com.hypixel.hytale.component.Ref<EntityStore> playerEntityRef = event.getPlayerRef();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return;
        }
        com.hypixel.hytale.component.Store<EntityStore> store = playerEntityRef.getStore();
        if (store == null) {
            return;
        }
        EntityStore entityStore = store.getExternalData();
        if (entityStore == null) {
            return;
        }
        com.hypixel.hytale.server.core.universe.world.World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        String blockName = com.autofarm.mods.systems.ChestLinkSystem.getBlockDisplayNameAt(world, targetBlock);
        String lowerName = blockName != null ? blockName.toLowerCase(java.util.Locale.ROOT) : "";

        // Check if interaction was with AutoTreeFarm block
        if (lowerName.contains("autotreefarm")) {
            com.autofarm.mods.components.AutoTreeFarmBlockComponent treeFarm = findTreeFarmAt(targetBlock);
            if (treeFarm == null) {
                treeFarm = new com.autofarm.mods.components.AutoTreeFarmBlockComponent(UUID.randomUUID(), new Vector3i(targetBlock));
                registerTreeFarm(treeFarm);
            }

            event.setCancelled(true);
            com.hypixel.hytale.server.core.entity.entities.Player player = event.getPlayer();
            if (player != null && player.getPageManager() != null) {
                com.hypixel.hytale.server.core.universe.PlayerRef playerRef = store.getComponent(playerEntityRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                player.getPageManager().openCustomPage(
                        playerEntityRef,
                        store,
                        new com.autofarm.mods.ui.AutoTreeFarmConfigPage(playerRef, treeFarm, world)
                );
            }
            return;
        }

        // Check if interaction was with AutoFarm block
        AutoFarmBlockComponent farm = findFarmAt(targetBlock);
        if (farm == null && (lowerName.contains("autofarm") || lowerName.equalsIgnoreCase("autofarm_block"))) {
            farm = new AutoFarmBlockComponent(UUID.randomUUID(), new Vector3i(targetBlock));
            registerFarm(farm);
        }

        if (farm != null) {
            event.setCancelled(true);
            com.hypixel.hytale.server.core.entity.entities.Player player = event.getPlayer();
            if (player != null && player.getPageManager() != null) {
                com.hypixel.hytale.server.core.universe.PlayerRef playerRef = store.getComponent(playerEntityRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                player.getPageManager().openCustomPage(
                        playerEntityRef,
                        store,
                        new com.autofarm.mods.ui.AutoFarmConfigPage(playerRef, farm, world)
                );
            }
        }
    }

    public static void registerFarm(AutoFarmBlockComponent farm) {
        AutoFarmRegistry.registerFarm(farm);
    }

    public static void unregisterFarm(UUID farmId) {
        AutoFarmRegistry.unregisterFarm(farmId);
    }

    public static AutoFarmBlockComponent findFarmAt(Vector3i pos) {
        return AutoFarmRegistry.findFarmAt(pos);
    }

    public static Collection<AutoFarmBlockComponent> getActiveFarms() {
        return AutoFarmRegistry.getActiveFarms();
    }

    public static void registerTreeFarm(com.autofarm.mods.components.AutoTreeFarmBlockComponent farm) {
        AutoFarmRegistry.registerTreeFarm(farm);
    }

    public static void unregisterTreeFarm(UUID farmId) {
        AutoFarmRegistry.unregisterTreeFarm(farmId);
    }

    public static com.autofarm.mods.components.AutoTreeFarmBlockComponent findTreeFarmAt(Vector3i pos) {
        return AutoFarmRegistry.findTreeFarmAt(pos);
    }

    public static Collection<com.autofarm.mods.components.AutoTreeFarmBlockComponent> getActiveTreeFarms() {
        return AutoFarmRegistry.getActiveTreeFarms();
    }

    public static int countActivePlantsForFarm(UUID farmId) {
        return AutoFarmRegistry.countActivePlantsForFarm(farmId);
    }

    public static void registerPlantedCrop(Vector3i pos, AutoPlantedComponent crop) {
        AutoFarmRegistry.registerPlantedCrop(pos, crop);
    }

    public static AutoPlantedComponent getPlantedCrop(Vector3i pos) {
        return AutoFarmRegistry.getPlantedCrop(pos);
    }

    public static void registerTilledSoil(Vector3i pos, TilledByFarmComponent soil) {
        AutoFarmRegistry.registerTilledSoil(pos, soil);
    }

    public static TilledByFarmComponent getTilledSoil(Vector3i pos) {
        return AutoFarmRegistry.getTilledSoil(pos);
    }
}
