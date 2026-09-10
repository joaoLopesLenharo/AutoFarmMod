package com.autofarm.mods.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.UUID;

public class AutoPlantedComponent implements Component<EntityStore> {

    public static final BuilderCodec<AutoPlantedComponent> CODEC = BuilderCodec.builder(
            AutoPlantedComponent.class,
            AutoPlantedComponent::new
    )
    .append(new KeyedCodec<>("FarmId", Codec.STRING), 
            (c, id) -> c.farmId = UUID.fromString(id), 
            c -> c.farmId.toString()).add()
    .append(new KeyedCodec<>("PlantType", Codec.STRING), (c, t) -> c.plantType = t, c -> c.plantType).add()
    .append(new KeyedCodec<>("PlantedAtTick", Codec.LONG), (c, t) -> c.plantedAtTick = t, c -> c.plantedAtTick).add()
    .append(new KeyedCodec<>("PlantedDay", Codec.DOUBLE), (c, d) -> c.plantedDay = d != null ? d : 0.0, c -> c.plantedDay).add()
    .append(new KeyedCodec<>("PosX", Codec.INTEGER), (c, x) -> c.posX = x, c -> c.posX).add()
    .append(new KeyedCodec<>("PosY", Codec.INTEGER), (c, y) -> c.posY = y, c -> c.posY).add()
    .append(new KeyedCodec<>("PosZ", Codec.INTEGER), (c, z) -> c.posZ = z, c -> c.posZ).add()
    .build();

    private UUID farmId;
    private String plantType;
    private long plantedAtTick;
    private double plantedDay = 0.0;
    private int posX;
    private int posY;
    private int posZ;

    public AutoPlantedComponent() {
        this.farmId = UUID.randomUUID();
        this.plantType = "";
    }

    public AutoPlantedComponent(UUID farmId, String plantType, long plantedAtTick, Vector3i cropPosition) {
        this(farmId, plantType, plantedAtTick, 0.0, cropPosition);
    }

    public AutoPlantedComponent(UUID farmId, String plantType, long plantedAtTick, double plantedDay, Vector3i cropPosition) {
        this.farmId = farmId;
        this.plantType = plantType;
        this.plantedAtTick = plantedAtTick;
        this.plantedDay = plantedDay;
        this.posX = cropPosition.x;
        this.posY = cropPosition.y;
        this.posZ = cropPosition.z;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public long getPlantedAtTick() {
        return plantedAtTick;
    }

    public void setPlantedAtTick(long plantedAtTick) {
        this.plantedAtTick = plantedAtTick;
    }

    public double getPlantedDay() {
        return plantedDay;
    }

    public void setPlantedDay(double plantedDay) {
        this.plantedDay = plantedDay;
    }

    public Vector3i getCropPosition() {
        return new Vector3i(posX, posY, posZ);
    }

    public void setCropPosition(Vector3i cropPosition) {
        this.posX = cropPosition.x;
        this.posY = cropPosition.y;
        this.posZ = cropPosition.z;
    }

    @Override
    public Component<EntityStore> clone() {
        return new AutoPlantedComponent(this.farmId, this.plantType, this.plantedAtTick, this.plantedDay, getCropPosition());
    }
}
