package com.shiver.structurecompasslegacy.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

final class ClientText {

    private static final Map<String, String> PROVIDER_KEYS = createProviderKeys();

    private ClientText() {
    }

    private static Map<String, String> createProviderKeys() {
        Map<String, String> keys = new HashMap<>();
        keys.put("net.minecraft.world.WorldProviderSurface", "overworld");
        keys.put("net.minecraft.world.WorldProviderHell", "the_nether");
        keys.put("net.minecraft.world.WorldProviderEnd", "the_end");
        keys.put("com.gildedgames.the_aether.world.AetherWorldProvider", "aether");
        keys.put("com.teammetallurgy.atum.world.WorldProviderAtum", "atum");
        keys.put("cn.mcmod.tofucraft.world.WorldProviderTofu", "tofu");
        keys.put("twilightforest.world.WorldProviderTwilightForest", "twilight_forest");
        keys.put("thecodex6824.thaumicaugmentation.common.world.WorldProviderEmptiness", "emptiness");
        keys.put("com.shiroroku.theaurorian.World.AurorianWorldProvider", "aurorian");
        keys.put("com.shinoow.abyssalcraft.common.world.WorldProviderAbyss", "abyssal_wasteland");
        keys.put("com.shinoow.abyssalcraft.common.world.WorldProviderDreadlands", "dreadlands");
        keys.put("com.shinoow.abyssalcraft.common.world.WorldProviderOmothol", "omothol");
        keys.put("divinerpg.dimensions.iceika.WorldProviderIceika", "iceika");
        keys.put("divinerpg.dimensions.vethea.VetheaWorldProvider", "vethea");
        return Collections.unmodifiableMap(keys);
    }

    static String name(String english, String chinese) {
        String language = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        return language.startsWith("zh") ? chinese : english;
    }

    static String translate(String suffix, Object... arguments) {
        return I18n.format("structurecompasslegacy." + suffix, arguments);
    }

    static String formatDimensions(NBTTagCompound entry) {
        NBTTagList list = entry.getTagList("dimensions", 10);
        if (list.tagCount() == 0) {
            return translate("dimension.none");
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < list.tagCount(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(formatDimension(list.getCompoundTagAt(index)));
        }
        return builder.toString();
    }

    static String dimensionSearchTerms(NBTTagCompound entry) {
        NBTTagList list = entry.getTagList("dimensions", 10);
        StringBuilder builder = new StringBuilder(formatDimensions(entry));
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound dim = list.getCompoundTagAt(index);
            String typeName = dim.getString("typeName");
            if (!typeName.isEmpty()) {
                builder.append(' ').append(typeName);
            }
            if (dim.getBoolean("hasId")) {
                builder.append(' ').append(dim.getInteger("id"));
            }
        }
        return builder.toString();
    }

    static boolean matchesDimension(NBTTagCompound entry, int playerDimension) {
        NBTTagList list = entry.getTagList("dimensions", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound dim = list.getCompoundTagAt(index);
            if (dim.getBoolean("hasId") && dim.getInteger("id") == playerDimension) {
                return true;
            }
        }
        return false;
    }

    private static String formatDimension(NBTTagCompound dim) {
        boolean hasId = dim.getBoolean("hasId");
        int id = dim.getInteger("id");
        String typeName = dim.getString("typeName");
        String provider = dim.getString("provider");

        String key = PROVIDER_KEYS.get(provider);
        if (key == null && hasId) {
            if (id == 0) {
                key = "overworld";
            } else if (id == -1) {
                key = "the_nether";
            } else if (id == 1) {
                key = "the_end";
            }
        }
        if (key == null && !typeName.isEmpty()) {
            String normalized = typeName.toLowerCase(Locale.ROOT).replace(' ', '_');
            if (I18n.hasKey("structurecompasslegacy.dimension." + normalized)) {
                key = normalized;
            }
        }

        String dimName;
        if (key != null) {
            dimName = translate("dimension." + key);
        } else if (!typeName.isEmpty()) {
            dimName = prettifyTypeName(typeName);
        } else if (hasId) {
            return translate("dimension_id", id);
        } else {
            return translate("dimension.none");
        }

        return hasId ? translate("dimension_short", dimName, id) : dimName;
    }

    private static String prettifyTypeName(String raw) {
        String spaced = raw.replace('_', ' ').trim();
        if (spaced.isEmpty()) {
            return raw;
        }
        if (!spaced.equals(spaced.toLowerCase(Locale.ROOT))) {
            return spaced;
        }
        StringBuilder result = new StringBuilder(spaced.length());
        boolean capitalize = true;
        for (int index = 0; index < spaced.length(); index++) {
            char ch = spaced.charAt(index);
            if (ch == ' ') {
                capitalize = true;
                result.append(ch);
            } else if (capitalize) {
                result.append(Character.toUpperCase(ch));
                capitalize = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
