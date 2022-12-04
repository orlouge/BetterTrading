package com.github.orlouge.bettertrading.api;

import com.github.orlouge.bettertrading.trade_offers.TradeOfferFactoryTypeHolder;

import net.minecraft.village.TradeOffers;

/**
 * The interface that custom trade offers should implement.
 */
public interface SerializableTradeOfferFactory extends TradeOffers.Factory, TradeOfferFactoryTypeHolder {
}
