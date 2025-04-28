package com.chaosthedude.explorerscompass.gui;

import java.util.*;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.network.CompassSearchPacket;
import com.chaosthedude.explorerscompass.network.StopSearchesPacket;
import com.chaosthedude.explorerscompass.network.TeleportPacket;
import com.chaosthedude.explorerscompass.sorting.ISorting;
import com.chaosthedude.explorerscompass.sorting.NameSorting;
import com.chaosthedude.explorerscompass.util.StructureUtils;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureFinderScreen extends Screen {

    private Level level;
    private Player player;
    private List<ResourceLocation> allowedStructureKeys;
    private List<ResourceLocation> structureKeysMatchingSearch;
    private Map<ResourceLocation, SearchResult> searchResults;
    private Button searchButton;
    private Button searchGroupButton;
    private Button sortByButton;
    private Button teleportButton;
    private Button cancelButton;
    private TransparentTextField searchTextField;
    private StructureSearchList selectionList;
    private ISorting sortingCategory;
    private Set<ResourceLocation> activeSearches = new HashSet<>();
    private Button stopSearchButton;

    public StructureFinderScreen(Level level, Player player, List<ResourceLocation> allowedStructureKeys) {
        super(Component.translatable("string.explorerscompass.structureFinder"));
        this.level = level;
        this.player = player;

        this.allowedStructureKeys = new ArrayList<ResourceLocation>(allowedStructureKeys);
        structureKeysMatchingSearch = new ArrayList<ResourceLocation>(this.allowedStructureKeys);
        searchResults = new HashMap<>();
        sortingCategory = new NameSorting();

        // Load any previously found structures from persistent storage
        loadSavedStructures();
    }

    // Add this new method to load saved structures
    private void loadSavedStructures() {
        // Initialize the data manager if needed
        com.chaosthedude.explorerscompass.util.StructureDataManager.init();

        // Load all found structures from persistent storage
        ExplorersCompass.LOGGER.info("Loading saved structure data");
        Map<String, com.mojang.datafixers.util.Pair<Integer, Integer>> savedStructures =
                com.chaosthedude.explorerscompass.util.StructureDataManager.getAllFoundStructures();

        // Add each saved structure to our search results
        for (Map.Entry<String, com.mojang.datafixers.util.Pair<Integer, Integer>> entry : savedStructures.entrySet()) {
            try {
                ResourceLocation key = new ResourceLocation(entry.getKey());
                int x = entry.getValue().getFirst();
                int z = entry.getValue().getSecond();

                // Only add if it's in our allowed structures list
                if (allowedStructureKeys.contains(key)) {
                    searchResults.put(key, new SearchResult(x, z));
                    ExplorersCompass.LOGGER.info("Loaded saved structure: " + key + " at X: " + x + ", Z: " + z);
                }
            } catch (Exception e) {
                ExplorersCompass.LOGGER.error("Error loading saved structure: " + entry.getKey(), e);
            }
        }

        ExplorersCompass.LOGGER.info("Loaded " + searchResults.size() + " structures from persistent storage");
    }

    @Override
    public boolean mouseScrolled(double scroll1, double scroll2, double scroll3) {
        return selectionList.mouseScrolled(scroll1, scroll2, scroll3);
    }

    @Override
    protected void init() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        setupWidgets();

        // Debug output
        ExplorersCompass.LOGGER.info("Structure Finder opened with " + allowedStructureKeys.size() + " structures");
        for (ResourceLocation key : allowedStructureKeys) {
            ExplorersCompass.LOGGER.info("Available structure: " + key);
        }
    }

    @Override
    public void tick() {
        searchTextField.tick();
        teleportButton.active = hasSelectedStructureLocation();

        // Check if the allowed structure list has synced
        if (allowedStructureKeys.size() != ExplorersCompass.allowedStructureKeys.size()) {
            removeWidget(selectionList);
            allowedStructureKeys = new ArrayList<ResourceLocation>(ExplorersCompass.allowedStructureKeys);
            structureKeysMatchingSearch = new ArrayList<ResourceLocation>(allowedStructureKeys);
            selectionList = new StructureSearchList(this, minecraft, width + 110, height, 40, height, 45);
            addRenderableWidget(selectionList);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, 65, 15, 0xffffff);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int par1, int par2, int par3) {
        boolean ret = super.keyPressed(par1, par2, par3);
        if (searchTextField.isFocused()) {
            processSearchTerm();
            return true;
        }
        return ret;
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        boolean ret = super.charTyped(typedChar, keyCode);
        if (searchTextField.isFocused()) {
            processSearchTerm();
            return true;
        }
        return ret;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    public void selectStructure(StructureSearchEntry entry) {
        boolean enable = entry != null;
        searchButton.active = enable;
        searchGroupButton.active = enable;
        teleportButton.active = enable && searchResults.containsKey(entry.getStructureKey());
    }

    // Modify searchForStructure method
    public void searchForStructure(ResourceLocation key) {
        // Don't search if already in progress
        if (activeSearches.contains(key)) {
            return;
        }

        // Don't search if already found
        if (searchResults.containsKey(key)) {
            return;
        }

        // Add to active searches
        activeSearches.add(key);

        // Update UI
        if (stopSearchButton != null) {
            stopSearchButton.active = !activeSearches.isEmpty();
        }

        // Send search request
        ExplorersCompass.network.sendToServer(new CompassSearchPacket(key, List.of(key), player.blockPosition()));
    }

    // Add method to remove from active searches
    public void finishSearch(ResourceLocation key) {
        activeSearches.remove(key);
        if (stopSearchButton != null) {
            stopSearchButton.active = !activeSearches.isEmpty();
        }
    }

    // Add stop searches method
    public void stopAllSearches() {
        if (!activeSearches.isEmpty()) {
            ExplorersCompass.network.sendToServer(new StopSearchesPacket());
            activeSearches.clear();
            if (stopSearchButton != null) {
                stopSearchButton.active = false;
            }
        }
    }

    public void searchForGroup(ResourceLocation key) {
        ExplorersCompass.network.sendToServer(new CompassSearchPacket(key, ExplorersCompass.typeKeysToStructureKeys.get(key), player.blockPosition()));
    }

    public void teleport() {
        if (selectionList.hasSelection() && searchResults.containsKey(selectionList.getSelected().getStructureKey())) {
            ExplorersCompass.network.sendToServer(new TeleportPacket(searchResults.get(selectionList.getSelected().getStructureKey())));
        }
    }

    public void processSearchTerm() {
        structureKeysMatchingSearch = new ArrayList<ResourceLocation>();
        for (ResourceLocation key : allowedStructureKeys) {
            if (StructureUtils.getPrettyStructureName(key).toLowerCase().contains(searchTextField.getValue().toLowerCase())) {
                structureKeysMatchingSearch.add(key);
            }
        }
        selectionList.refreshList();
    }

    public List<ResourceLocation> sortStructures() {
        final List<ResourceLocation> structures = structureKeysMatchingSearch;
        Collections.sort(structures, new NameSorting());
        Collections.sort(structures, sortingCategory);
        return structures;
    }

    public boolean hasSelectedStructureLocation() {
        return selectionList.hasSelection() && searchResults.containsKey(selectionList.getSelected().getStructureKey());
    }

    public SearchResult getSearchResult(ResourceLocation key) {
        return searchResults.get(key);
    }

    public void addSearchResult(ResourceLocation key, int x, int z) {
        ExplorersCompass.LOGGER.info("Adding search result to UI for " + key + " at X: " + x + ", Z: " + z);
        searchResults.put(key, new SearchResult(x, z));

        // Force a refresh of the selection list to update the UI
        if (selectionList != null) {
            ExplorersCompass.LOGGER.info("Refreshing selection list to show new result");
            selectionList.refreshList();

            // If the currently selected structure is the one we found, update teleport button
            if (selectionList.hasSelection() && selectionList.getSelected().getStructureKey().equals(key)) {
                teleportButton.active = true;
            }
        } else {
            ExplorersCompass.LOGGER.warn("Selection list is null, cannot refresh UI");
        }

        // Remove from active searches
        finishSearch(key);
    }

    private void setupWidgets() {
        clearWidgets();
        searchButton = addRenderableWidget(new TransparentButton(10, 40, 110, 20, Component.translatable("string.explorerscompass.search"), (onPress) -> {
            if (selectionList.hasSelection()) {
                selectionList.getSelected().searchForStructure();
            }
        }));
        searchGroupButton = addRenderableWidget(new TransparentButton(10, 65, 110, 20, Component.translatable("string.explorerscompass.searchForGroup"), (onPress) -> {
            if (selectionList.hasSelection()) {
                selectionList.getSelected().searchForGroup();
            }
        }));
        sortByButton = addRenderableWidget(new TransparentButton(10, 90, 110, 20, Component.translatable("string.explorerscompass.sortBy").append(Component.literal(": " + sortingCategory.getLocalizedName())), (onPress) -> {
            sortingCategory = sortingCategory.next();
            sortByButton.setMessage(Component.translatable("string.explorerscompass.sortBy").append(Component.literal(": " + sortingCategory.getLocalizedName())));
            selectionList.refreshList();
        }));

        // Add Clear Found Structures button above the Cancel button
        Button clearFoundButton = addRenderableWidget(new TransparentButton(10, height - 55, 110, 20,
                Component.translatable("string.explorerscompass.clearFound"), (onPress) -> {
            com.chaosthedude.explorerscompass.util.StructureDataManager.clearFoundStructures();
            searchResults.clear();
            selectionList.refreshList();
            teleportButton.active = false;
        }));

        cancelButton = addRenderableWidget(new TransparentButton(10, height - 30, 110, 20, Component.translatable("gui.cancel"), (onPress) -> {
            minecraft.setScreen(null);
        }));
        teleportButton = addRenderableWidget(new TransparentButton(width - 120, 10, 110, 20, Component.translatable("string.explorerscompass.teleport"), (onPress) -> {
            teleport();
        }));

        stopSearchButton = addRenderableWidget(new TransparentButton(10, 115, 110, 20,
                Component.translatable("string.explorerscompass.stopSearch"), (onPress) -> {
            stopAllSearches();
        }));

        stopSearchButton.active = false;
        searchButton.active = false;
        searchGroupButton.active = false;
        teleportButton.active = false;

        teleportButton.visible = ExplorersCompass.canTeleport;

        searchTextField = new TransparentTextField(font, width / 2 - 82, 10, 140, 20, Component.translatable("string.explorerscompass.search"));
        addRenderableWidget(searchTextField);

        if (selectionList == null) {
            selectionList = new StructureSearchList(this, minecraft, width + 110, height, 40, height, 45);
        }
        addRenderableWidget(selectionList);
    }

    public void refreshStructureList() {
        allowedStructureKeys = new ArrayList<>(ExplorersCompass.allowedStructureKeys);
        structureKeysMatchingSearch = new ArrayList<>(allowedStructureKeys);
        if (selectionList != null) {
            selectionList.refreshList();
        }
    }

    public static class SearchResult {
        private int x;
        private int z;

        public SearchResult(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }
    }
}