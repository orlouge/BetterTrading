package com.github.orlouge.bettertrading.mixin;

import com.github.orlouge.bettertrading.trade_offers.TradeOfferFactoryType;
import com.github.orlouge.bettertrading.trade_offers.TradeOfferFactoryTypeHolder;
import com.github.orlouge.bettertrading.trade_offers.VanillaTradeOfferFactories;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.village.TradeOffers;

@Mixin(TradeOffers.Factory.class)
public interface TradeOffersFactoryMixin extends TradeOfferFactoryTypeHolder {

    @Override
    default TradeOfferFactoryType<?> getType() {
        return VanillaTradeOfferFactories.getVanillaFactoryCodec((TradeOffers.Factory) this);
    }
}
