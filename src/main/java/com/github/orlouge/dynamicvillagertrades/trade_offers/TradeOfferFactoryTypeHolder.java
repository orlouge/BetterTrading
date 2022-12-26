package com.github.orlouge.dynamicvillagertrades.trade_offers;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface TradeOfferFactoryTypeHolder {
    TradeOfferFactoryType<?> getType();
}
