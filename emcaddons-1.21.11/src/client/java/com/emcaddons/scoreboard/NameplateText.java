package com.emcaddons.scoreboard;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Resolves visible nameplate text from a custom name or a text_display entity.
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
            DisplayEntity.TextDisplayEntity.Data data = textDisplay.getData();
            if (data != null && data.text() != null) {
                return Optional.of(data.text().getString());
            }
        }
        return Optional.empty();
    }
}
