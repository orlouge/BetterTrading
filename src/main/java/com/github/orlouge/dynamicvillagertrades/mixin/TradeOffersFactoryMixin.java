package com.github.orlouge.dynamicvillagertrades.mixin;

import com.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import com.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryTypeHolder;
import com.github.orlouge.dynamicvillagertrades.trade_offers.VanillaTradeOfferFactories;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.village.TradeOffers;

@Mixin(TradeOffers.Factory.class)
public interface TradeOffersFactoryMixin extends TradeOfferFactoryTypeHolder {

    @Override
    default TradeOfferFactoryType<?> getType() {
        return VanillaTradeOfferFactories.getVanillaFactoryCodec((TradeOffers.Factory) this);
    }
}
