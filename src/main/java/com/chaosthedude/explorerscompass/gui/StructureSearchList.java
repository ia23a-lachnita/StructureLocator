package com.chaosthedude.explorerscompass.gui;

import java.util.Objects;

import com.chaosthedude.explorerscompass.ExplorersCompass;
import com.chaosthedude.explorerscompass.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureSearchList extends ObjectSelectionList<StructureSearchEntry> {

	private final StructureFinderScreen parentScreen;

	public StructureSearchList(StructureFinderScreen parentScreen, Minecraft mc, int width, int height, int top, int bottom, int slotHeight) {
		super(mc, width, height, top, bottom, slotHeight);
		this.parentScreen = parentScreen;
		refreshList();
	}

	@Override
	protected int getScrollbarPosition() {
		return super.getScrollbarPosition() + 20;
	}

	@Override
	public int getRowWidth() {
		return super.getRowWidth() + 50;
	}

	@Override
	protected boolean isSelectedItem(int slotIndex) {
		return slotIndex >= 0 && slotIndex < children().size() ? children().get(slotIndex).equals(getSelected()) : false;
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		renderList(poseStack, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderList(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		for (int j = 0; j < getItemCount(); ++j) {
			int rowTop = getRowTop(j);
			int rowBottom = getRowBottom(j);
			if (rowBottom >= y0 && rowTop <= y1) {
				int j1 = itemHeight - 4;
				StructureSearchEntry entry = getEntry(j);
				if (/*renderSelection*/ true && isSelectedItem(j)) {
					final int insideLeft = x0 + width / 2 - getRowWidth() / 2 + 2;
					RenderUtils.drawRect(insideLeft - 4, rowTop - 4, insideLeft + getRowWidth() + 4, rowTop + itemHeight, 255 / 2 << 24);
				}
				entry.render(poseStack, j, rowTop, getRowLeft(), getRowWidth(), j1, mouseX, mouseY, isMouseOver((double) mouseX, (double) mouseY) && Objects.equals(getEntryAtPosition((double) mouseX, (double) mouseY), entry), partialTicks);
			}
		}

		if (getMaxScroll() > 0) {
			int left = getScrollbarPosition();
			int right = left + 6;
			int height = (int) ((float) ((y1 - y0) * (y1 - y0)) / (float) getMaxPosition());
			height = Mth.clamp(height, 32, y1 - y0 - 8);
			int top = (int) getScrollAmount() * (y1 - y0 - height) / getMaxScroll() + y0;
			if (top < y0) {
				top = y0;
			}

			RenderUtils.drawRect(left, y0, right, y1, (int) (2.35F * 255.0F) / 2 << 24);
			RenderUtils.drawRect(left, top, right, top + height, (int) (1.9F * 255.0F) / 2 << 24);
		}
	}

	private int getRowBottom(int index) {
		return getRowTop(index) + itemHeight;
	}

	public void refreshList() {
		ExplorersCompass.LOGGER.info("Refreshing structure list - current entries: " + children().size());

		// Get previously selected entry if any
		StructureSearchEntry selectedEntry = getSelected();
		ResourceLocation selectedKey = selectedEntry != null ? selectedEntry.getStructureKey() : null;

		clearEntries();
		for (ResourceLocation key : parentScreen.sortStructures()) {
			addEntry(new StructureSearchEntry(this, key));

			// Log if this structure has a search result
			if (parentScreen.getSearchResult(key) != null) {
				ExplorersCompass.LOGGER.info("  - Added entry for " + key + " with found coordinates");
			} else {
				ExplorersCompass.LOGGER.info("  - Added entry for " + key + " (no coordinates yet)");
			}
		}

		// Restore selection if possible
		if (selectedKey != null) {
			for (StructureSearchEntry entry : children()) {
				if (entry.getStructureKey().equals(selectedKey)) {
					setSelected(entry);
					parentScreen.selectStructure(entry);
					break;
				}
			}
		} else {
			setSelected(null);
			parentScreen.selectStructure(null);
		}

		ExplorersCompass.LOGGER.info("Refreshed structure list - new entries: " + children().size());
	}

	public void selectStructure(StructureSearchEntry entry) {
		setSelected(entry);
		parentScreen.selectStructure(entry);
	}

	public boolean hasSelection() {
		return getSelected() != null;
	}

	public StructureFinderScreen getParentScreen() {
		return parentScreen;
	}
}