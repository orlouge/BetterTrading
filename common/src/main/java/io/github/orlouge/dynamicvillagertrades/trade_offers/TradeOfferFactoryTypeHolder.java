package io.github.orlouge.dynamicvillagertrades.trade_offers;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public interface TradeOfferFactoryTypeHolder {
    Supplier<TradeOfferFactoryType<?>> getType();
}
