package com.emcaddons.scoreboard;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Reads nametag text from living custom names or {@code text_display} entities.
 */
public final class NameplateText {

    private NameplateText() {}

    public static Optional<String> of(Entity entity) {
        if (entity == null) return Optional.empty();
        Text customName = entity.getCustomName();
        if (customName != null) {
            return Optional.of(customName.getString());
        }
        if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {
            Text text = textDisplay.getText();
            if (text != null) {
                return Optional.of(text.getString());
            }
        }
        return Optional.empty();
    }
}
