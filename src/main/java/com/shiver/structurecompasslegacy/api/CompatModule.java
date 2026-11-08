package com.shiver.structurecompasslegacy.api;

public interface CompatModule {

    String modId();

    void register(StructureRegistry registry);
}
