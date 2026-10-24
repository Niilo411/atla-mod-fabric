package com.minecraft.atlamod.platform.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** NeoForge's BasicItemListing: a fixed price for a fixed item. */
public record BasicItemListing(ItemStack price, ItemStack forSale, int maxTrades, int xp, float priceMult)
        implements VillagerTrades.ItemListing {
    @Override
    public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
        return new MerchantOffer(new ItemCost(price.getItem(), price.getCount()), forSale.copy(),
                maxTrades, xp, priceMult);
    }
}
