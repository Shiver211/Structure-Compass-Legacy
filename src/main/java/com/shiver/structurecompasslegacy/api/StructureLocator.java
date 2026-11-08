package com.shiver.structurecompasslegacy.api;

import net.minecraft.world.WorldServer;

@FunctionalInterface
public interface StructureLocator {

    /** 只创建任务状态；若构造失败，工厂负责释放尚未交给调度器的资源。 */
    SearchTask createTask(SearchContext context) throws Exception;

    default String unavailableReason(WorldServer world) {
        return "";
    }
}
