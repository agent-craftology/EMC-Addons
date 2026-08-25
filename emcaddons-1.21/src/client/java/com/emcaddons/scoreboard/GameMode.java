package com.emcaddons.scoreboard;

public enum GameMode {
    DUNGEONS("Dungeons"),
    GENS("Gens"),
    FACTORIES("Factories"),
    SKYBLOCK("Skyblock"),
    PRISONS("Prisons");

    public final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public boolean isComingSoon() {
        return this != DUNGEONS;
    }
}
