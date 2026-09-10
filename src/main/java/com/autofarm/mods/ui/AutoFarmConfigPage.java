package com.autofarm.mods.ui;

import com.autofarm.mods.components.AutoFarmBlockComponent;
import com.autofarm.mods.systems.ChestLinkSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

public class AutoFarmConfigPage extends CustomUIPage {

    private final AutoFarmBlockComponent farm;
    private final World world;

    public AutoFarmConfigPage(PlayerRef playerRef, AutoFarmBlockComponent farm, World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
        this.farm = farm;
        this.world = world;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append("AutoFarm/AutoFarmConfigPage.ui");

        String selectedFace = farm.getSelectedChestFace();
        Vector3i chestPos = ChestLinkSystem.findAdjacentChest(world, farm);
        boolean hasWater = world == null || com.autofarm.mods.systems.TerraformSystem.hasWaterUnderneath(world, farm.getPosition());
        String waterStatus = hasWater ? " [ÁGUA: OK]" : " [ALERTA: Falta água abaixo da máquina!]";

        String status = "Face: " + selectedFace + waterStatus + 
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
            Vector3i chestPos = ChestLinkSystem.findAdjacentChest(world, farm);
            farm.setLinkedChestPosition(chestPos);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw("[AutoFarm] Modo AUTO ativado." +
                        (chestPos != null ? " Baú detectado em [" + chestPos.x + ", " + chestPos.y + ", " + chestPos.z + "]."
                                          : " Nenhum baú encostado no bloco.")));
            }
            rebuild();
            return;
        }

        for (ChestLinkSystem.BlockFace face : ChestLinkSystem.BlockFace.values()) {
            if (data.contains(face.name())) {
                farm.setSelectedChestFace(face.name());
                Vector3i chestPos = ChestLinkSystem.findAdjacentChest(world, farm);
                farm.setLinkedChestPosition(chestPos);
                if (playerRef != null) {
                    playerRef.sendMessage(Message.raw("[AutoFarm] Face configurada: " + face.getDisplayName() +
                            (chestPos != null ? " (Baú conectado!)"
                                              : " (Atenção: nenhum baú encostado nesta face)")));
                }
                rebuild();
                return;
            }
        }
    }
}
