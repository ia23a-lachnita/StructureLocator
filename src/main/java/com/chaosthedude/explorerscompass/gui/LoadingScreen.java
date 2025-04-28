package com.chaosthedude.explorerscompass.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LoadingScreen extends Screen {
    private final Runnable onLoad;
    private boolean hasSentRequest = false;

    public LoadingScreen(Runnable onLoad) {
        super(Component.translatable("string.explorerscompass.loading"));
        this.onLoad = onLoad;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, Component.translatable("string.explorerscompass.loading"), width / 2, height / 2, 0xFFFFFF);

        if (!hasSentRequest) {
            onLoad.run();
            hasSentRequest = true;
        }
    }
}