package com.emcaddons.scoreboard;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * Resolves visible nameplate text from a custom name or a text_display entity.
 */
public final class NameplateText {
    private NameplateText() {}

    public static Optional<String> of(Entity entity) {
        if (entity == null) return Optional.empty();
        Component customName = entity.getCustomName();
        if (customName != null) {
            return Optional.of(customName.getString());
        }
        if (entity instanceof Display.TextDisplay textDisplay) {
            var state = textDisplay.textRenderState();
            if (state != null && state.text() != null) {
                return Optional.of(state.text().getString());
            }
        }
        return Optional.empty();
    }
}
