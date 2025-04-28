package com.chaosthedude.explorerscompass.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Manages persistent storage of found structure locations
 */
public class StructureDataManager {

    private static final String DATA_DIR = "structurefinder";
    private static final String FOUND_STRUCTURES_FILE = "found_structures.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map of structure ID to coordinates (X, Z)
    private static Map<String, Pair<Integer, Integer>> foundStructures = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initializes the data manager and loads any saved structures
     */
    public static void init() {
        if (initialized) {
            return;
        }

        ExplorersCompass.LOGGER.info("Initializing StructureDataManager");
        createDirectories();
        loadFoundStructures();
        initialized = true;
    }

    /**
     * Adds a found structure to persistent storage
     */
    public static void addFoundStructure(ResourceLocation structureKey, int x, int z) {
        ExplorersCompass.LOGGER.info("Adding structure to persistent storage: " + structureKey + " at X: " + x + ", Z: " + z);
        foundStructures.put(structureKey.toString(), Pair.of(x, z));
        saveFoundStructures();
    }

    /**
     * Gets the coordinates of a found structure, or null if not found
     */
    public static Pair<Integer, Integer> getFoundStructureCoordinates(ResourceLocation structureKey) {
        return foundStructures.get(structureKey.toString());
    }

    /**
     * Checks if a structure has been found
     */
    public static boolean isStructureFound(ResourceLocation structureKey) {
        return foundStructures.containsKey(structureKey.toString());
    }

    /**
     * Gets all found structure data
     */
    public static Map<String, Pair<Integer, Integer>> getAllFoundStructures() {
        return foundStructures;
    }

    /**
     * Clears all found structure data
     */
    public static void clearFoundStructures() {
        foundStructures.clear();
        saveFoundStructures();
    }

    /**
     * Creates required directories if they don't exist
     */
    private static void createDirectories() {
        File dataDir = new File(Minecraft.getInstance().gameDirectory, DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            ExplorersCompass.LOGGER.info("Created data directory: " + dataDir.getAbsolutePath());
        }
    }

    /**
     * Loads found structures from JSON file
     */
    private static void loadFoundStructures() {
        File dataFile = new File(new File(Minecraft.getInstance().gameDirectory, DATA_DIR), FOUND_STRUCTURES_FILE);

        if (!dataFile.exists()) {
            ExplorersCompass.LOGGER.info("Found structures file doesn't exist yet, starting with empty data");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            foundStructures.clear();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String structureKey = entry.getKey();
                JsonObject coordsObj = entry.getValue().getAsJsonObject();
                int x = coordsObj.get("x").getAsInt();
                int z = coordsObj.get("z").getAsInt();

                foundStructures.put(structureKey, Pair.of(x, z));
            }

            ExplorersCompass.LOGGER.info("Loaded " + foundStructures.size() + " found structures from file");
        } catch (IOException e) {
            ExplorersCompass.LOGGER.error("Error loading found structures", e);
        }
    }

    /**
     * Saves found structures to JSON file
     */
    private static void saveFoundStructures() {
        File dataFile = new File(new File(Minecraft.getInstance().gameDirectory, DATA_DIR), FOUND_STRUCTURES_FILE);

        try (FileWriter writer = new FileWriter(dataFile)) {
            JsonObject json = new JsonObject();

            for (Map.Entry<String, Pair<Integer, Integer>> entry : foundStructures.entrySet()) {
                JsonObject coordsObj = new JsonObject();
                coordsObj.addProperty("x", entry.getValue().getFirst());
                coordsObj.addProperty("z", entry.getValue().getSecond());

                json.add(entry.getKey(), coordsObj);
            }

            GSON.toJson(json, writer);
            ExplorersCompass.LOGGER.info("Saved " + foundStructures.size() + " found structures to file");
        } catch (IOException e) {
            ExplorersCompass.LOGGER.error("Error saving found structures", e);
        }
    }
}