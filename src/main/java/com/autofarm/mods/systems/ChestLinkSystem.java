package com.autofarm.mods.systems;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

import java.util.Locale;

public class ChestLinkSystem {

    /**
     * Resolves the ItemContainer located at the given block position, if loaded.
     */
    public static ItemContainer getChestContainer(World world, Vector3i chestPos) {
        if (world == null || chestPos == null) {
            return null;
        }

        // Method 1: Use BlockModule.getComponent (official Hytale ECS block entity query)
        try {
            ItemContainerBlock containerBlock = BlockModule.getComponent(
                    ItemContainerBlock.getComponentType(), world, chestPos.x, chestPos.y, chestPos.z
            );
            if (containerBlock != null && containerBlock.getItemContainer() != null) {
                return containerBlock.getItemContainer();
            }
        } catch (Throwable ignored) {
        }

        // Method 2: Chunk store ECS entity
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(chestPos.x, chestPos.z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
            if (chunk != null) {
                Ref<ChunkStore> ref = chunk.getBlockComponentEntity(chestPos.x, chestPos.y, chestPos.z);
                if (ref != null && ref.isValid()) {
                    ItemContainerBlock containerBlock = ref.getStore().getComponent(ref, ItemContainerBlock.getComponentType());
                    if (containerBlock != null && containerBlock.getItemContainer() != null) {
                        return containerBlock.getItemContainer();
                    }
                }

                Holder<ChunkStore> holder = chunk.getBlockComponentHolder(chestPos.x, chestPos.y, chestPos.z);
                if (holder != null) {
                    ItemContainerBlock containerBlock = holder.getComponent(ItemContainerBlock.getComponentType());
                    if (containerBlock != null && containerBlock.getItemContainer() != null) {
                        return containerBlock.getItemContainer();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    /**
     * Checks if a linked chest still physically exists and has an inventory or is a valid container block.
     */
    public static boolean isChestValid(World world, Vector3i chestPos) {
        if (world == null || chestPos == null) {
            return false;
        }

        if (getChestContainer(world, chestPos) != null) {
            return true;
        }

        // Fallback: check if the block in chunk is a chest or declares an ItemContainerBlock component
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(chestPos.x, chestPos.z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
            if (chunk != null) {
                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType type = 
                        chunk.getBlockType(chestPos.x, chestPos.y, chestPos.z);
                if (type != null && type.getId() != null) {
                    String lower = type.getId().toLowerCase(Locale.ROOT);
                    if (lower.contains("chest") || lower.contains("container") || lower.contains("crate")) {
                        return true;
                    }
                    if (com.hypixel.hytale.server.core.modules.block.BlockEntity.declaresComponent(
                            type, ItemContainerBlock.getComponentType())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    public enum BlockFace {
        EAST(1, 0, 0, "Leste (+X)"),
        WEST(-1, 0, 0, "Oeste (-X)"),
        SOUTH(0, 0, 1, "Sul (+Z)"),
        NORTH(0, 0, -1, "Norte (-Z)"),
        UP(0, 1, 0, "Cima (+Y)"),
        DOWN(0, -1, 0, "Baixo (-Y)");

        public final int dx, dy, dz;
        public final String displayName;

        BlockFace(int dx, int dy, int dz, String displayName) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Vector3i getOffset(Vector3i origin) {
            return new Vector3i(origin.x + dx, origin.y + dy, origin.z + dz);
        }

        public static BlockFace fromString(String name) {
            if (name == null) return null;
            for (BlockFace f : values()) {
                if (f.name().equalsIgnoreCase(name) || f.displayName.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                    return f;
                }
            }
            return null;
        }
    }

    /**
     * Finds a chest directly touching (adjacent to) the AutoFarm block.
     * Enforces the rule: "o bau terá que estar encostado no bloco para funcionar".
     * If the farm has a specific face configured, only checks that face.
     * If "AUTO", checks all 6 touching faces.
     */
    public static Vector3i findAdjacentChest(World world, com.autofarm.mods.components.AutoFarmBlockComponent farm) {
        if (world == null || farm == null) {
            return null;
        }

        Vector3i origin = farm.getPosition();
        String selected = farm.getSelectedChestFace();

        // Specific face selected
        if (selected != null && !selected.equalsIgnoreCase("AUTO")) {
            BlockFace face = BlockFace.fromString(selected);
            if (face != null) {
                Vector3i candidate = face.getOffset(origin);
                if (isChestValid(world, candidate)) {
                    return candidate;
                }
            }
            return null; // Chest must be on the configured face!
        }

        // AUTO mode: Check all 6 adjacent faces touching the block
        for (BlockFace face : BlockFace.values()) {
            Vector3i candidate = face.getOffset(origin);
            if (isChestValid(world, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Resolves a readable display name of the block touching a position.
     */
    public static String getBlockDisplayNameAt(World world, Vector3i pos) {
        if (world == null || pos == null) {
            return "Desconhecido";
        }
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
            if (chunk == null) {
                return "Descarregado";
            }
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType type = chunk.getBlockType(pos.x, pos.y, pos.z);
            if (type == null || type.getId() == null || TerraformSystem.isAir(type.getId())) {
                return "Vazio (Ar)";
            }
            String id = type.getId();
            if (id.toLowerCase(Locale.ROOT).contains("chest")) {
                return "Baú (" + id + ")";
            }
            return id;
        } catch (Throwable t) {
            return "Desconhecido";
        }
    }

    /**
     * Finds the closest container (chest) touching originPos.
     * Returns null if no adjacent chest is touching the block.
     */
    public static Vector3i findNearestChest(World world, Vector3i originPos, int horizontalRange, int verticalRange) {
        if (world == null || originPos == null) {
            return null;
        }

        // Enforce strictly adjacent chests (touching one of the 6 faces)
        for (BlockFace face : BlockFace.values()) {
            Vector3i candidate = face.getOffset(originPos);
            if (isChestValid(world, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Withdraws 1 seed unit from the given container.
     * If preferredPlantType is provided, it tries to match it; otherwise it takes the first seed found.
     * Returns the seed type String or null if none available.
     */
    public static String withdrawSeedFromContainer(ItemContainer container, String preferredPlantType) {
        if (container == null) {
            return null;
        }

        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.getQuantity() <= 0) {
                continue;
            }

            String itemId = stack.getItemId();
            if (itemId == null) {
                continue;
            }

            if (isSeedItem(itemId, preferredPlantType)) {
                container.removeItemStackFromSlot(slot, 1);
                return itemId;
            }
        }

        return null;
    }

    /**
     * Withdraws 1 seed from the chest at chestPos in world.
     */
    public static String withdrawSeed(World world, Vector3i chestPos, String preferredPlantType) {
        ItemContainer container = getChestContainer(world, chestPos);
        return withdrawSeedFromContainer(container, preferredPlantType);
    }

    /**
     * Withdraws 1 plantable item (crop seed or tree sapling) from the container matching criteria.
     * If allowCrop is true, allows crop seeds. If allowTree is true, allows tree saplings.
     * Returns the item ID, or null if none available.
     */
    public static String withdrawPlantable(World world, Vector3i chestPos, boolean allowCrop, boolean allowTree) {
        ItemContainer container = getChestContainer(world, chestPos);
        if (container == null) {
            return null;
        }

        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.getQuantity() <= 0) {
                continue;
            }

            String itemId = stack.getItemId();
            if (itemId == null) {
                continue;
            }

            if (!com.autofarm.mods.catalog.PlantCatalog.isPlantable(itemId)) {
                continue;
            }

            com.autofarm.mods.catalog.PlantSpecies species = com.autofarm.mods.catalog.PlantCatalog.resolve(itemId);
            if (species != null) {
                if (species.isCrop() && !allowCrop) continue;
                if (species.isTree() && !allowTree) continue;
            }

            container.removeItemStackFromSlot(slot, 1);
            return itemId;
        }

        return null;
    }

    /**
     * Deposits an ItemStack into the container.
     * Returns the quantity that DID NOT FIT (0 if all fit, or the remainder for edge-case overflow handling).
     */
    public static int depositToContainer(ItemContainer container, ItemStack itemToDeposit) {
        if (itemToDeposit == null || itemToDeposit.getQuantity() <= 0) {
            return 0;
        }
        if (container == null) {
            return itemToDeposit.getQuantity();
        }

        ItemStackTransaction tx = container.addItemStack(itemToDeposit);
        if (tx != null && tx.succeeded()) {
            ItemStack remainder = tx.getRemainder();
            if (remainder == null || remainder.getQuantity() <= 0) {
                return 0; // Completely deposited
            }
            return remainder.getQuantity(); // Partial overflow
        }

        // Entire deposit failed / chest full
        return itemToDeposit.getQuantity();
    }

    /**
     * Deposits an ItemStack into the chest at chestPos in world.
     */
    public static int deposit(World world, Vector3i chestPos, ItemStack itemToDeposit) {
        ItemContainer container = getChestContainer(world, chestPos);
        return depositToContainer(container, itemToDeposit);
    }

    /**
     * Checks whether an itemId represents a seed, sapling, sprout, or plantable crop.
     */
    public static boolean isSeedItem(String itemId, String preferredPlantType) {
        if (itemId == null) {
            return false;
        }
        String lower = itemId.toLowerCase(Locale.ROOT);

        if (preferredPlantType != null && !preferredPlantType.isEmpty()) {
            return lower.contains(preferredPlantType.toLowerCase(Locale.ROOT));
        }

        return com.autofarm.mods.catalog.PlantCatalog.isPlantable(itemId);
    }
}
