package com.autofarm.mods.ui;

import com.autofarm.mods.catalog.PlantCatalog;
import com.autofarm.mods.catalog.PlantSpecies;
import com.autofarm.mods.components.AutoTreeFarmBlockComponent;
import com.autofarm.mods.systems.ChestLinkSystem;
import com.autofarm.mods.systems.TerraformSystem;
import com.autofarm.mods.systems.TreeFarmSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

public class AutoTreeFarmConfigPage extends CustomUIPage {

    private final AutoTreeFarmBlockComponent farm;
    private final World world;

    public AutoTreeFarmConfigPage(PlayerRef playerRef, AutoTreeFarmBlockComponent farm, World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
        this.farm = farm;
        this.world = world;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append("AutoFarm/AutoFarmConfigPage.ui");

        // Verify dirt directly underneath (y - 1)
        Vector3i farmPos = farm.getPosition();
        Vector3i dirtPos = new Vector3i(farmPos.x, farmPos.y - 1, farmPos.z);
        boolean hasDirt = false;
        if (world != null) {
            long chunkIdx = ChunkUtil.indexChunkFromBlock(dirtPos.x, dirtPos.z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIdx);
            if (chunk != null) {
                BlockType bt = chunk.getBlockType(dirtPos.x, dirtPos.y, dirtPos.z);
                hasDirt = bt != null 
                        && TerraformSystem.isTillableSoilOrTilled(bt.getId()) 
                        && !TerraformSystem.isWaterAt(world, dirtPos.x, dirtPos.y, dirtPos.z);
            }
        }
        farm.setHasDirtUnderneath(hasDirt);

        String selectedFace = farm.getSelectedChestFace();
        Vector3i chestPos = TreeFarmSystem.findAdjacentChestForTreeFarm(world, farm);
        String saplingId = farm.getSaplingItemId();
        PlantSpecies species = saplingId != null ? PlantCatalog.resolve(saplingId) : null;
        String saplingName = species != null ? species.getId() : (saplingId != null ? saplingId : "Nenhuma (coloque no baú)");

        String dirtStatus = hasDirt ? " [TERRA ABAIXO: OK]" : " [ALERTA: Requer bloco de terra abaixo!]";
        String status = "TreeFarm | Face: " + selectedFace + dirtStatus +
                " | Muda: " + saplingName +
                (chestPos != null ? " [BAÚ: " + chestPos.x + ", " + chestPos.y + ", " + chestPos.z + "]" : " [NENHUM BAÚ ENCONTRADO]");
        commands.set("#ActiveFaceInfo.Text", status);

        // Update each face label and bind activation events
        for (ChestLinkSystem.BlockFace face : ChestLinkSystem.BlockFace.values()) {
            Vector3i adjPos = face.getOffset(farm.getPosition());
            String blockName = ChestLinkSystem.getBlockDisplayNameAt(world, adjPos);

            switch (face) {
                case UP -> {
                    commands.set("#FaceUpLabel.Text", "Cima (+Y): " + blockName);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceUp", EventData.of("Face", "UP"));
                }
                case DOWN -> {
                    commands.set("#FaceDownLabel.Text", "Baixo (-Y): " + blockName);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceDown", EventData.of("Face", "DOWN"));
                }
                case NORTH -> {
                    commands.set("#FaceNorthLabel.Text", "Norte (-Z): " + blockName);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceNorth", EventData.of("Face", "NORTH"));
                }
                case SOUTH -> {
                    commands.set("#FaceSouthLabel.Text", "Sul (+Z): " + blockName);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceSouth", EventData.of("Face", "SOUTH"));
                }
                case EAST -> {
                    commands.set("#FaceEastLabel.Text", "Leste (+X): " + blockName);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceEast", EventData.of("Face", "EAST"));
                }
                case WEST -> {
                    commands.set("#FaceWestLabel.Text", "Oeste (-X): " + blockName);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceWest", EventData.of("Face", "WEST"));
                }
            }
        }

        // Action buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnFaceAuto", EventData.of("Face", "AUTO"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose", EventData.of("Face", "CLOSE"));
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String data) {
        if (data == null) {
            return;
        }

        if (data.contains("CLOSE")) {
            close();
            return;
        }

        if (data.contains("AUTO")) {
            farm.setSelectedChestFace("AUTO");
            Vector3i chestPos = TreeFarmSystem.findAdjacentChestForTreeFarm(world, farm);
            farm.setLinkedChestPosition(chestPos);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw("[AutoTreeFarm] Modo AUTO ativado." +
                        (chestPos != null ? " Baú detectado em [" + chestPos.x + ", " + chestPos.y + ", " + chestPos.z + "]."
                                          : " Nenhum baú encostado no bloco.")));
            }
            close();
            return;
        }

        for (ChestLinkSystem.BlockFace face : ChestLinkSystem.BlockFace.values()) {
            if (data.contains(face.name())) {
                farm.setSelectedChestFace(face.name());
                Vector3i candidate = face.getOffset(farm.getPosition());
                boolean valid = ChestLinkSystem.isChestValid(world, candidate);
                farm.setLinkedChestPosition(valid ? candidate : null);

                if (playerRef != null) {
                    playerRef.sendMessage(Message.raw("[AutoTreeFarm] Face configurada para: " + face.getDisplayName() +
                            (valid ? ". Baú vinculado com sucesso em " + candidate + "!"
                                   : ". Nenhum baú válido nesta face.")));
                }
                close();
                return;
            }
        }
    }
}
