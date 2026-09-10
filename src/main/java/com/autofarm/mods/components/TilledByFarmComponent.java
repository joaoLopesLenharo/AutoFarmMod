package com.autofarm.mods.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.UUID;

public class TilledByFarmComponent implements Component<EntityStore> {

    public static final BuilderCodec<TilledByFarmComponent> CODEC = BuilderCodec.builder(
            TilledByFarmComponent.class,
            TilledByFarmComponent::new
    )
    .append(new KeyedCodec<>("FarmId", Codec.STRING), 
            (c, id) -> c.farmId = UUID.fromString(id), 
            c -> c.farmId.toString()).add()
    .append(new KeyedCodec<>("PosX", Codec.INTEGER), (c, x) -> c.posX = x, c -> c.posX).add()
    .append(new KeyedCodec<>("PosY", Codec.INTEGER), (c, y) -> c.posY = y, c -> c.posY).add()
    .append(new KeyedCodec<>("PosZ", Codec.INTEGER), (c, z) -> c.posZ = z, c -> c.posZ).add()
    .build();

    private UUID farmId;
    private int posX;
    private int posY;
    private int posZ;

    public TilledByFarmComponent() {
        this.farmId = UUID.randomUUID();
    }

    public TilledByFarmComponent(UUID farmId, Vector3i soilPosition) {
        this.farmId = farmId;
        this.posX = soilPosition.x;
        this.posY = soilPosition.y;
        this.posZ = soilPosition.z;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public Vector3i getSoilPosition() {
        return new Vector3i(posX, posY, posZ);
    }

    public void setSoilPosition(Vector3i soilPosition) {
        this.posX = soilPosition.x;
        this.posY = soilPosition.y;
        this.posZ = soilPosition.z;
    }

    @Override
    public Component<EntityStore> clone() {
        return new TilledByFarmComponent(this.farmId, getSoilPosition());
    }
}
