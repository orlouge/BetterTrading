package io.github.orlouge.dynamicvillagertrades.api;

import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryTypeHolder;

import net.minecraft.village.TradeOffers;

/**
 * The interface that custom trade offers should implement.
 */
public interface SerializableTradeOfferFactory extends TradeOffers.Factory, TradeOfferFactoryTypeHolder {
}
