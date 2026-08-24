package com.emcaddons.gui;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiScale;
import com.emcaddons.gui.clickgui.GuiTheme;
import com.emcaddons.gui.clickgui.ThemeSelector;
import com.emcaddons.scoreboard.HudLayoutManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudEditScreen extends Screen {

    private final HudLayoutManager layoutManager;
    private final Runnable onPersist;
    private final ThemeSelector themeSelector = new ThemeSelector();

    public HudEditScreen(HudLayoutManager layoutManager, Runnable onPersist) {
        super(Text.literal("HUD Layout"));
        this.layoutManager = layoutManager;
        this.onPersist = onPersist;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GuiScale.begin(context, width, height);
        mouseX = GuiScale.mouseXi(mouseX, width);
        mouseY = GuiScale.mouseYi(mouseY, height);
        layoutManager.renderEditOverlay(context, mouseX, mouseY);
        GuiDraw.textCentered(context, textRenderer,
                "Drag cards to move  -  click B/A to toggle stats  -  Esc to save and exit",
                width / 2, 10, GuiTheme.TITLE);
        themeSelector.layoutRight(width - 10, 10, textRenderer);
        themeSelector.render(context, textRenderer, mouseX, mouseY);
        GuiScale.end(context);
    }

    private Click scaledClick(Click click) {
        return new Click(GuiScale.mouseX(click.x(), width), GuiScale.mouseY(click.y(), height), click.buttonInfo());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Click scaled = scaledClick(click);
        double mouseX = scaled.x();
        double mouseY = scaled.y();
        int button = scaled.button();
        if (themeSelector.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && layoutManager.mouseClicked(mouseX, mouseY, MinecraftClient.getInstance())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Click scaled = scaledClick(click);
        double mouseX = scaled.x();
        double mouseY = scaled.y();
        if (layoutManager.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click scaled = scaledClick(click);
        double mouseX = scaled.x();
        double mouseY = scaled.y();
        if (layoutManager.mouseReleased(mouseX, mouseY)) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        onPersist.run();
        super.close();
    }
}
