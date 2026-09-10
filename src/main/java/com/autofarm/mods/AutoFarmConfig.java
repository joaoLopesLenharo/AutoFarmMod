package com.autofarm.mods;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class AutoFarmConfig {

    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "config/autofarm.json";

    public int scanIntervalTicks = 40;
    public int horizontalRange = 4; // Limited to maximum water puddle irrigation radius (4 blocks)
    public int verticalRange = 4;
    public int waterProximityMax = 4;
    public int maxActivePlantsPerFarm = 64;
    public int maxPlantBatchPerCycle = 4;
    public long maturityTicksThreshold = 600;
    public double treeMaturityMinDays = 2.5;
    public int treeMinTrunkHeight = 5;

    private static AutoFarmConfig instance;

    public static AutoFarmConfig get() {
        if (instance == null) {
            instance = loadOrCreate();
        }
        return instance;
    }

    public static AutoFarmConfig loadOrCreate() {
        AutoFarmConfig config = new AutoFarmConfig();
        File file = new File(CONFIG_FILE);

        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[512];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    sb.append(buf, 0, read);
                }
                config.parseSimpleJson(sb.toString());
                System.out.println("[AutoFarm-CONFIG] Successfully loaded configuration from " + CONFIG_FILE);
                return config;
            } catch (Exception e) {
                System.err.println("[AutoFarm-CONFIG] Failed to read config file, falling back to defaults: " + e.getMessage());
            }
        } else {
            // Save defaults
            config.save();
        }
        return config;
    }

    public void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(CONFIG_FILE);
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(toJson());
            }
            System.out.println("[AutoFarm-CONFIG] Created default configuration at " + CONFIG_FILE);
        } catch (Exception e) {
            System.err.println("[AutoFarm-CONFIG] Failed to save config file: " + e.getMessage());
        }
    }

    public String toJson() {
        return "{\n"
                + "  \"scanIntervalTicks\": " + scanIntervalTicks + ",\n"
                + "  \"horizontalRange\": " + horizontalRange + ",\n"
                + "  \"verticalRange\": " + verticalRange + ",\n"
                + "  \"waterProximityMax\": " + waterProximityMax + ",\n"
                + "  \"maxActivePlantsPerFarm\": " + maxActivePlantsPerFarm + ",\n"
                + "  \"maxPlantBatchPerCycle\": " + maxPlantBatchPerCycle + ",\n"
                + "  \"maturityTicksThreshold\": " + maturityTicksThreshold + ",\n"
                + "  \"treeMaturityMinDays\": " + treeMaturityMinDays + ",\n"
                + "  \"treeMinTrunkHeight\": " + treeMinTrunkHeight + "\n"
                + "}\n";
    }

    public void parseSimpleJson(String json) {
        if (json == null) return;
        for (String line : json.split("[\r\n,]+")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"") && trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                String key = parts[0].replaceAll("[\"\\s]", "");
                String value = parts[1].replaceAll("[\"\\s}]", "");
                try {
                    switch (key) {
                        case "scanIntervalTicks" -> this.scanIntervalTicks = Integer.parseInt(value);
                        case "horizontalRange" -> this.horizontalRange = Integer.parseInt(value);
                        case "verticalRange" -> this.verticalRange = Integer.parseInt(value);
                        case "waterProximityMax" -> this.waterProximityMax = Integer.parseInt(value);
                        case "maxActivePlantsPerFarm" -> this.maxActivePlantsPerFarm = Integer.parseInt(value);
                        case "maxPlantBatchPerCycle" -> this.maxPlantBatchPerCycle = Integer.parseInt(value);
                        case "maturityTicksThreshold" -> this.maturityTicksThreshold = Long.parseLong(value);
                        case "treeMaturityMinDays" -> this.treeMaturityMinDays = Double.parseDouble(value);
                        case "treeMinTrunkHeight" -> this.treeMinTrunkHeight = Integer.parseInt(value);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}
