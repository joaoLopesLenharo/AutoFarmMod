package com.autofarm.mods.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.UUID;

public class AutoFarmBlockComponent implements Component<EntityStore> {

    public static final BuilderCodec<AutoFarmBlockComponent> CODEC = BuilderCodec.builder(
            AutoFarmBlockComponent.class,
            AutoFarmBlockComponent::new
    )
    .append(new KeyedCodec<>("FarmId", Codec.STRING), 
            (c, id) -> c.farmId = UUID.fromString(id), 
            c -> c.farmId.toString()).add()
    .append(new KeyedCodec<>("PosX", Codec.INTEGER), (c, x) -> c.posX = x, c -> c.posX).add()
    .append(new KeyedCodec<>("PosY", Codec.INTEGER), (c, y) -> c.posY = y, c -> c.posY).add()
    .append(new KeyedCodec<>("PosZ", Codec.INTEGER), (c, z) -> c.posZ = z, c -> c.posZ).add()
    .append(new KeyedCodec<>("Range", Codec.INTEGER), (c, r) -> c.range = r, c -> c.range).add()
    .append(new KeyedCodec<>("VerticalRange", Codec.INTEGER), (c, vr) -> c.verticalRange = vr, c -> c.verticalRange).add()
    .append(new KeyedCodec<>("ScanIntervalTicks", Codec.INTEGER), (c, s) -> c.scanIntervalTicks = s, c -> c.scanIntervalTicks).add()
    .append(new KeyedCodec<>("WaterProximityMax", Codec.INTEGER), (c, w) -> c.waterProximityMax = w, c -> c.waterProximityMax).add()
    .append(new KeyedCodec<>("MaxActivePlantsPerFarm", Codec.INTEGER), (c, m) -> c.maxActivePlantsPerFarm = m, c -> c.maxActivePlantsPerFarm).add()
    .append(new KeyedCodec<>("LastScanTick", Codec.LONG), (c, t) -> c.lastScanTick = t, c -> c.lastScanTick).add()
    .append(new KeyedCodec<>("SelectedChestFace", Codec.STRING), (c, f) -> c.selectedChestFace = f, c -> c.selectedChestFace).add()
    .build();

    private UUID farmId;
    private int posX;
    private int posY;
    private int posZ;
    private int range = 64;
    private int verticalRange = 8;
    private int scanIntervalTicks = 100; // ~5 seconds at 20 ticks/sec
    private int waterProximityMax = 4;
    private int maxActivePlantsPerFarm = 32;
    private Vector3i linkedChestPosition = null;
    private long lastScanTick = 0;
    private String selectedChestFace = "AUTO";

    public AutoFarmBlockComponent() {
        this.farmId = UUID.randomUUID();
    }

    public AutoFarmBlockComponent(UUID farmId, Vector3i position) {
        this.farmId = farmId;
        this.posX = position.x;
        this.posY = position.y;
        this.posZ = position.z;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public Vector3i getPosition() {
        return new Vector3i(posX, posY, posZ);
    }

    public void setPosition(Vector3i position) {
        this.posX = position.x;
        this.posY = position.y;
        this.posZ = position.z;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getVerticalRange() {
        return verticalRange;
    }

    public void setVerticalRange(int verticalRange) {
        this.verticalRange = verticalRange;
    }

    public int getScanIntervalTicks() {
        return scanIntervalTicks;
    }

    public void setScanIntervalTicks(int scanIntervalTicks) {
        this.scanIntervalTicks = scanIntervalTicks;
    }

    public int getWaterProximityMax() {
        return waterProximityMax;
    }

    public void setWaterProximityMax(int waterProximityMax) {
        this.waterProximityMax = waterProximityMax;
    }

    public int getMaxActivePlantsPerFarm() {
        return maxActivePlantsPerFarm;
    }

    public void setMaxActivePlantsPerFarm(int maxActivePlantsPerFarm) {
        this.maxActivePlantsPerFarm = maxActivePlantsPerFarm;
    }

    public Vector3i getLinkedChestPosition() {
        return linkedChestPosition;
    }

    public void setLinkedChestPosition(Vector3i linkedChestPosition) {
        this.linkedChestPosition = linkedChestPosition;
    }

    public long getLastScanTick() {
        return lastScanTick;
    }

    public void setLastScanTick(long lastScanTick) {
        this.lastScanTick = lastScanTick;
    }

    public String getSelectedChestFace() {
        return selectedChestFace != null ? selectedChestFace : "AUTO";
    }

    public void setSelectedChestFace(String selectedChestFace) {
        this.selectedChestFace = selectedChestFace;
    }

    /**
     * Returns the position of the block directly touching the specified face.
     * If face is null or "AUTO", returns null.
     */
    public Vector3i getFaceBlockPosition(String face) {
        if (face == null || "AUTO".equalsIgnoreCase(face)) {
            return null;
        }
        String upper = face.toUpperCase(java.util.Locale.ROOT);
        return switch (upper) {
            case "UP", "TOP" -> new Vector3i(posX, posY + 1, posZ);
            case "DOWN", "BOTTOM" -> new Vector3i(posX, posY - 1, posZ);
            case "NORTH" -> new Vector3i(posX, posY, posZ - 1);
            case "SOUTH" -> new Vector3i(posX, posY, posZ + 1);
            case "EAST" -> new Vector3i(posX + 1, posY, posZ);
            case "WEST" -> new Vector3i(posX - 1, posY, posZ);
            default -> null;
        };
    }

    @Override
    public AutoFarmBlockComponent clone() {
        AutoFarmBlockComponent copy = new AutoFarmBlockComponent(this.farmId, getPosition());
        copy.range = this.range;
        copy.verticalRange = this.verticalRange;
        copy.scanIntervalTicks = this.scanIntervalTicks;
        copy.waterProximityMax = this.waterProximityMax;
        copy.maxActivePlantsPerFarm = this.maxActivePlantsPerFarm;
        copy.linkedChestPosition = this.linkedChestPosition != null ? new Vector3i(this.linkedChestPosition) : null;
        copy.lastScanTick = this.lastScanTick;
        copy.selectedChestFace = this.selectedChestFace;
        return copy;
    }
}
