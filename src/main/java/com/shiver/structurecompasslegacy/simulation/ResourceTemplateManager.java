package com.shiver.structurecompasslegacy.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

import javax.annotation.Nonnull;

/** 会话内模板缓存；读取 JAR 资源，不访问存档中的结构文件。 */
public final class ResourceTemplateManager extends TemplateManager {

    private final Map<ResourceLocation, Template> templates = new HashMap<>();

    public ResourceTemplateManager() {
        super("", null);
    }

    @Override
    @MethodsReturnNonnullByDefault
    public Template getTemplate(MinecraftServer server, @Nonnull ResourceLocation id) {
        Template template = get(server, id);
        if (template == null) {
            throw new IllegalStateException("Missing structure template: " + id);
        }

        return template;
    }

    @Override
    public Template get(MinecraftServer server, @Nonnull ResourceLocation id) {
        if (!templates.containsKey(id)) {
            readTemplate(id);
        }

        return templates.get(id);
    }

    @Override
    public boolean readTemplate(ResourceLocation id) {
        String path = "/assets/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt";
        try (InputStream stream = ResourceTemplateManager.class.getResourceAsStream(path)) {
            if (stream == null) {
                return false;
            }

            Template template = new Template();
            template.read(CompressedStreamTools.readCompressed(stream));
            templates.put(id, template);
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read structure template: " + id, exception);
        }
    }

    @Override
    public boolean writeTemplate(MinecraftServer server, @Nonnull ResourceLocation id) {
        throw new UnsupportedOperationException("Simulation cannot save templates");
    }
}
