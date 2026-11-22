package com.shiver.structurecompasslegacy.compat.iceandfire;

import java.util.Locale;

enum IceLandmark {
    FIRE_ROOST("generateDragonRoosts", "lastDragonRoost", "chared"),
    ICE_ROOST("generateDragonRoosts", "lastDragonRoost", "frozen"),
    LIGHTNING_ROOST("generateDragonRoosts", "lastDragonRoost", "crackled"),
    FIRE_CAVE("generateDragonDens", "lastDragonCave", "chared"),
    ICE_CAVE("generateDragonDens", "lastDragonCave", "frozen"),
    LIGHTNING_CAVE("generateDragonDens", "lastDragonCave", "crackled"),
    SIREN_ISLAND("generateSirenIslands", "lastSirenIsland", null),
    MAUSOLEUM("generateMausoleums", "lastMausoleum", null);

    final String id = name().toLowerCase(Locale.ROOT);
    final String setting;
    final String last;
    final String material;

    IceLandmark(String setting, String last, String material) {
        this.setting = setting;
        this.last = last;
        this.material = material;
    }

    boolean roost() {
        return last.equals("lastDragonRoost");
    }
}
