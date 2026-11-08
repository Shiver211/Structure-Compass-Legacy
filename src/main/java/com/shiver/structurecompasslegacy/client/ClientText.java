package com.shiver.structurecompasslegacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

final class ClientText {

    private ClientText() {
    }

    static String name(String english, String chinese) {
        String language = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        return language.startsWith("zh") ? chinese : english;
    }

    static String translate(String suffix, Object... arguments) {
        return I18n.format("structurecompasslegacy." + suffix, arguments);
    }
}
