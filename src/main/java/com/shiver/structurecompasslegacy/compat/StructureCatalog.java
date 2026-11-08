package com.shiver.structurecompasslegacy.compat;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.api.StructureRegistry;

import net.minecraftforge.fml.common.Loader;

public final class StructureCatalog {

    private StructureCatalog() {
    }

    public static void register(StructureRegistry registry) {
        Module[] modules;
        try (Reader reader = new InputStreamReader(
                StructureCatalog.class.getResourceAsStream("/compat/structures.json"), StandardCharsets.UTF_8)) {
            modules = new Gson().fromJson(reader, Module[].class);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read the structure catalog", exception);
        }

        for (Module module : modules) {
            if (!module.modId.equals("minecraft") && !Loader.isModLoaded(module.modId)) {
                continue;
            }

            for (Entry entry : module.structures) {
                registry.register(new StructureDefinition(
                        module.modId + ":" + entry.id, module.modName, entry.english, entry.chinese,
                        world -> false, null, "", () -> true));
            }
        }
    }

    private static final class Module {

        private String modId;
        private String modName;
        private List<Entry> structures;
    }

    private static final class Entry {

        private String id;
        private String english;
        private String chinese;
    }
}
