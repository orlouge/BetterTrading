package io.github.orlouge.dynamicvillagertrades.mixin;

import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryTypeHolder;
import io.github.orlouge.dynamicvillagertrades.trade_offers.VanillaTradeOfferFactories;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.village.TradeOffers;

import java.util.function.Supplier;

@Mixin(TradeOffers.Factory.class)
public interface TradeOffersFactoryMixin extends TradeOfferFactoryTypeHolder {

    @Override
    default Supplier<TradeOfferFactoryType<?>> getType() {
        return VanillaTradeOfferFactories.getVanillaFactoryCodec((TradeOffers.Factory) this);
    }
}
