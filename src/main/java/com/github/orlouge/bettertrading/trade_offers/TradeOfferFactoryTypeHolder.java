package com.github.orlouge.bettertrading.trade_offers;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface TradeOfferFactoryTypeHolder {
    TradeOfferFactoryType<?> getType();
}
