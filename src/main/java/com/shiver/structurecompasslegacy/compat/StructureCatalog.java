package com.shiver.structurecompasslegacy.compat;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import com.google.gson.Gson;
import com.shiver.structurecompasslegacy.api.SearchMethod;
import com.shiver.structurecompasslegacy.api.StructureDefinition;
import com.shiver.structurecompasslegacy.api.StructureRegistry;

/** 先声明全部条目，再绑定定位器；可选接口失败不会让结构从列表中消失。 */
public final class StructureCatalog {

    private StructureCatalog() {
    }

    public static void register(StructureRegistry registry, Predicate<String> installed) {
        Module[] modules;
        try (Reader reader = new InputStreamReader(
                StructureCatalog.class.getResourceAsStream("/compat/structures.json"), StandardCharsets.UTF_8)) {
            modules = new Gson().fromJson(reader, Module[].class);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read the structure catalog", exception);
        }

        for (Module module : modules) {
            if (!module.modId.equals("minecraft") && !installed.test(module.modId)) {
                continue;
            }

            for (Entry entry : module.structures) {
                String reason = entry.method.equals("UNSUPPORTED") ? entry.limitation : "binding_failed";
                registry.register(new StructureDefinition(
                        module.modId + ":" + entry.id, module.modName, entry.english, entry.chinese,
                        SearchMethod.UNSUPPORTED, world -> false, null,
                        "structurecompasslegacy.limitation." + reason));
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
        private String method;
        private String limitation;
    }
}
