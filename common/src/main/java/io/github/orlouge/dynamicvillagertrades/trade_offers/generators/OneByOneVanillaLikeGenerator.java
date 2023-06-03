package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.village.TradeOffers;

import java.util.*;

public class OneByOneVanillaLikeGenerator extends VanillaLikeGenerator {
    @Override
    protected boolean professionFilter(String profession) {
        return true;
    }

    @Override
    protected TradeGroup tradeGroupAtLevel(Integer level, String levelName, TradeOffers.Factory[] trades) {
        List<ExtendedTradeOffer.Factory> offers = new ArrayList<>(trades.length);
        for (int i = 0; i < trades.length; i++) {
            Map<String, Double> attributes = Map.of();
            if (trades.length > 2) {
                attributes = Map.of(AttributeUtils.generateTradeAttributeName(trades[i], levelName + "_" + i), 1.0);
            }
            attributes = getAttributes(trades[i], attributes);
            offers.add(cachedOffer(trades[i], i, level, levelName, attributes));
        }
        int min_trades = Math.min(trades.length, 2);
        return new TradeGroup(false, min_trades, min_trades, 1.0, Optional.empty(), offers, Optional.empty());
    }

    protected Map<String, Double> getAttributes(TradeOffers.Factory trade, Map<String, Double> defaultAttributes) {
        return defaultAttributes;
    }
}
