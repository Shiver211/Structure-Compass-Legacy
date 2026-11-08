package com.shiver.structurecompasslegacy.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public final class SimulationBlockTicks {

    private final Set<NextTickListEntry> pending = new HashSet<>();
    private final TreeSet<NextTickListEntry> ordered = new TreeSet<>();

    public void schedule(BlockPos position, Block block, long time, int priority) {
        NextTickListEntry entry = new NextTickListEntry(position.toImmutable(), block);
        entry.setScheduledTime(time);
        entry.setPriority(priority);
        if (pending.add(entry)) {
            ordered.add(entry);
        }
    }

    public boolean contains(BlockPos position, Block block) {
        return pending.contains(new NextTickListEntry(position, block));
    }

    public NextTickListEntry poll() {
        NextTickListEntry entry = ordered.pollFirst();
        pending.remove(entry);
        return entry;
    }

    public List<NextTickListEntry> within(StructureBoundingBox bounds, boolean remove) {
        List<NextTickListEntry> result = new ArrayList<>();
        Iterator<NextTickListEntry> iterator = ordered.iterator();
        while (iterator.hasNext()) {
            NextTickListEntry entry = iterator.next();
            if (bounds.isVecInside(entry.position)) {
                result.add(entry);
                if (remove) {
                    iterator.remove();
                    pending.remove(entry);
                }
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }

    public void clear() {
        pending.clear();
        ordered.clear();
    }
}
