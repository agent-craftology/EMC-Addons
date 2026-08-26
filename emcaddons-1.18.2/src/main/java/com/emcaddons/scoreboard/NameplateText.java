package com.emcaddons.scoreboard;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Reads a dungeon nameplate from an entity's custom name.
 * 1.18.2 has no display entities, so there is no text-display branch.
 */
public final class NameplateText {
    private NameplateText() {}

    public static Optional<String> of(Entity entity) {
        if (entity == null) return Optional.empty();
        Text customName = entity.getCustomName();
        if (customName == null) return Optional.empty();
        String text = customName.getString();
        if (text == null || text.isEmpty()) return Optional.empty();
        return Optional.of(text);
    }
}
