package com.emcaddons.gui;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiScale;
import com.emcaddons.gui.clickgui.GuiTheme;
import com.emcaddons.gui.clickgui.ThemeSelector;
import com.emcaddons.scoreboard.HudLayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HudEditScreen extends Screen {

    private final HudLayoutManager layoutManager;
    private final Runnable onPersist;
    private final ThemeSelector themeSelector = new ThemeSelector();

    public HudEditScreen(HudLayoutManager layoutManager, Runnable onPersist) {
        super(Component.literal("HUD Layout"));
        this.layoutManager = layoutManager;
        this.onPersist = onPersist;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GuiScale.begin(context, width, height);
        mouseX = GuiScale.mouseXi(mouseX, width);
        mouseY = GuiScale.mouseYi(mouseY, height);
        layoutManager.renderEditOverlay(context, mouseX, mouseY);
        GuiDraw.textCentered(context, font,
                "Drag cards to move  -  click B/A to toggle stats  -  Esc to save and exit",
                width / 2, 10, GuiTheme.TITLE);
        themeSelector.layoutRight(width - 10, 10, font);
        themeSelector.render(context, font, mouseX, mouseY);
        GuiScale.end(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = GuiScale.mouseX(event.x(), width);
        double mouseY = GuiScale.mouseY(event.y(), height);
        if (themeSelector.mouseClicked(mouseX, mouseY, event.button())) return true;
        if (event.button() == 0 && layoutManager.mouseClicked(mouseX, mouseY, Minecraft.getInstance())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = GuiScale.mouseX(event.x(), width);
        double mouseY = GuiScale.mouseY(event.y(), height);
        if (layoutManager.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = GuiScale.mouseX(event.x(), width);
        double mouseY = GuiScale.mouseY(event.y(), height);
        if (layoutManager.mouseReleased(mouseX, mouseY)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        onPersist.run();
        super.onClose();
    }
}
