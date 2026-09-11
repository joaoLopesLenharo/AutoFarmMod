package com.autofarm.mods.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.Objects;
import java.util.UUID;

public class AutoTreeFarmBlockComponent implements Component<EntityStore>, Cloneable {

    private UUID farmId;
    private Vector3i position;
    private Vector3i linkedChestPosition;
    private String selectedChestFace; // "AUTO", "NORTH", "SOUTH", "EAST", "WEST", "UP", "DOWN"
    private String saplingItemId;      // e.g. "Plant_Sapling_Oak"
    private long productionIntervalTicks; // Ticks between resource production (default 300 ~ 15s)
    private long lastProductionTick;
    private boolean hasDirtUnderneath;

    public static final BuilderCodec<AutoTreeFarmBlockComponent> CODEC = BuilderCodec.builder(
            AutoTreeFarmBlockComponent.class,
            AutoTreeFarmBlockComponent::new
    )
    .append(new KeyedCodec<>("FarmId", Codec.STRING),
            (c, id) -> c.farmId = UUID.fromString(id),
            c -> c.farmId != null ? c.farmId.toString() : UUID.randomUUID().toString()).add()
    .append(new KeyedCodec<>("PosX", Codec.INTEGER), (c, x) -> { if (c.position == null) c.position = new Vector3i(); c.position.x = x; }, c -> c.position != null ? c.position.x : 0).add()
    .append(new KeyedCodec<>("PosY", Codec.INTEGER), (c, y) -> { if (c.position == null) c.position = new Vector3i(); c.position.y = y; }, c -> c.position != null ? c.position.y : 0).add()
    .append(new KeyedCodec<>("PosZ", Codec.INTEGER), (c, z) -> { if (c.position == null) c.position = new Vector3i(); c.position.z = z; }, c -> c.position != null ? c.position.z : 0).add()
    .append(new KeyedCodec<>("SelectedChestFace", Codec.STRING), (c, f) -> c.selectedChestFace = f, c -> c.selectedChestFace != null ? c.selectedChestFace : "AUTO").add()
    .append(new KeyedCodec<>("SaplingItemId", Codec.STRING), (c, s) -> c.saplingItemId = s, c -> c.saplingItemId != null ? c.saplingItemId : "").add()
    .append(new KeyedCodec<>("ProductionIntervalTicks", Codec.LONG), (c, t) -> c.productionIntervalTicks = t, c -> c.productionIntervalTicks).add()
    .append(new KeyedCodec<>("LastProductionTick", Codec.LONG), (c, t) -> c.lastProductionTick = t, c -> c.lastProductionTick).add()
    .build();

    public AutoTreeFarmBlockComponent() {
        this(UUID.randomUUID(), new Vector3i(0, 0, 0));
    }

    public AutoTreeFarmBlockComponent(UUID farmId, Vector3i position) {
        this.farmId = farmId != null ? farmId : UUID.randomUUID();
        this.position = position != null ? new Vector3i(position) : new Vector3i(0, 0, 0);
        this.linkedChestPosition = null;
        this.selectedChestFace = "AUTO";
        this.saplingItemId = null;
        this.productionIntervalTicks = 300L; // Default: ~15 seconds per cycle
        this.lastProductionTick = 0L;
        this.hasDirtUnderneath = false;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public Vector3i getPosition() {
        return position;
    }

    public void setPosition(Vector3i position) {
        this.position = position;
    }

    public Vector3i getLinkedChestPosition() {
        return linkedChestPosition;
    }

    public void setLinkedChestPosition(Vector3i linkedChestPosition) {
        this.linkedChestPosition = linkedChestPosition;
    }

    public String getSelectedChestFace() {
        return selectedChestFace != null ? selectedChestFace : "AUTO";
    }

    public void setSelectedChestFace(String selectedChestFace) {
        this.selectedChestFace = selectedChestFace;
    }

    public String getSaplingItemId() {
        return saplingItemId;
    }

    public void setSaplingItemId(String saplingItemId) {
        this.saplingItemId = saplingItemId;
    }

    public long getProductionIntervalTicks() {
        return productionIntervalTicks > 0 ? productionIntervalTicks : 300L;
    }

    public void setProductionIntervalTicks(long productionIntervalTicks) {
        this.productionIntervalTicks = productionIntervalTicks;
    }

    public long getLastProductionTick() {
        return lastProductionTick;
    }

    public void setLastProductionTick(long lastProductionTick) {
        this.lastProductionTick = lastProductionTick;
    }

    public boolean isHasDirtUnderneath() {
        return hasDirtUnderneath;
    }

    public void setHasDirtUnderneath(boolean hasDirtUnderneath) {
        this.hasDirtUnderneath = hasDirtUnderneath;
    }

    @Override
    public AutoTreeFarmBlockComponent clone() {
        try {
            AutoTreeFarmBlockComponent cloned = (AutoTreeFarmBlockComponent) super.clone();
            cloned.farmId = this.farmId;
            cloned.position = this.position != null ? new Vector3i(this.position) : null;
            cloned.linkedChestPosition = this.linkedChestPosition != null ? new Vector3i(this.linkedChestPosition) : null;
            cloned.selectedChestFace = this.selectedChestFace;
            cloned.saplingItemId = this.saplingItemId;
            cloned.productionIntervalTicks = this.productionIntervalTicks;
            cloned.lastProductionTick = this.lastProductionTick;
            cloned.hasDirtUnderneath = this.hasDirtUnderneath;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutoTreeFarmBlockComponent that)) return false;
        return Objects.equals(farmId, that.farmId) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmId, position);
    }
}
