package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.TradeOfferManager;
import io.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.util.Pair;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public abstract class VanillaLikeGenerator extends Generator {
    @Override
    public Optional<Map<String, TradeGroup>> generate(VillagerProfession profession) {
        if (!professionFilter(profession.id())) return Optional.empty();
        return Optional.ofNullable(TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession)).map(tradeMap ->
                tradeMap.keySet().stream().sorted().map(level -> {
                    String levelName = TradeOfferManager.MerchantLevel.fromId(level).result().map(Enum::name).orElse("" + level).toLowerCase();
                    String group_name = "" + level + "_" + levelName;
                    TradeOffers.Factory[] trades = tradeMap.get(level);
                    return new Pair<>(group_name, tradeGroupAtLevel(level, levelName, trades));
                }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, TradeGroup::merge, TreeMap::new)));
    }

    protected ExtendedTradeOffer.Factory cachedOffer(TradeOffers.Factory trade, int idx, Integer level, String levelName, Map<String, Double> attributes) {
        boolean cache = trade instanceof TradeOffers.SellMapFactory;
        Optional<String> key = cache ? Optional.of(levelName + "_" + AttributeUtils.generateTradeAttributeName(trade, "" + idx)) : Optional.empty();
        return new ExtendedTradeOffer.Factory(
                trade, level, attributes, Optional.empty(), key, cache
        );
    }

    protected abstract boolean professionFilter(String profession);

    protected abstract TradeGroup tradeGroupAtLevel(Integer level, String levelName, TradeOffers.Factory[] trades);
}
