package com.minecraft.atlamod.platform.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;

import java.util.ArrayList;
import java.util.List;

/** One profession's trade book, levels 1 to 5, for handlers to add to. */
public final class VillagerTradesEvent {
    private final ResourceKey<VillagerProfession> type;
    private final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();

    public VillagerTradesEvent(ResourceKey<VillagerProfession> type) {
        this.type = type;
        for (int level = 1; level <= 5; level++) trades.put(level, new ArrayList<>());
    }

    public ResourceKey<VillagerProfession> getType() { return type; }
    public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() { return trades; }
}
