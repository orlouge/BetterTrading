package io.github.orlouge.dynamicvillagertrades.trade_offers;

import io.github.orlouge.dynamicvillagertrades.WeightedRandomList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public record TradeGroup(boolean replace, int minTrades, int maxTrades, double randomness,
                         Optional<Map<String, Double>> affinity,
                         List<ExtendedTradeOffer.Factory> offers,
                         Optional<Map<String, TradeGroup>> subGroups) {
    public static final Codec<TradeGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("replace").forGetter(TradeGroup::replace),
            Codec.INT.fieldOf("min_trades").forGetter(TradeGroup::minTrades),
            Codec.INT.fieldOf("max_trades").forGetter(TradeGroup::maxTrades),
            Codec.DOUBLE.optionalFieldOf("randomness", 1.0).forGetter(TradeGroup::randomness),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("affinity").forGetter(TradeGroup::affinity),
            ExtendedTradeOffer.Factory.CODEC.listOf().fieldOf("trades").forGetter(TradeGroup::offers),
            Codecs.createLazy(() -> Codec.unboundedMap(Codec.STRING, TradeGroup.CODEC)).optionalFieldOf("subgroups").forGetter(TradeGroup::subGroups)
    ).apply(instance, TradeGroup::new));

    public static TradeGroup merge(TradeGroup first, TradeGroup second) {
        if (second.replace) {
            return second;
        } else {
            Map<String, TradeGroup> subgroups = new HashMap<>(first.subGroups.orElse(Collections.emptyMap()));
            second.subGroups().ifPresent(subgroups2 -> subgroups2.forEach((name, group) -> {
                subgroups.computeIfPresent(name, (_name, oldGroup) -> TradeGroup.merge(oldGroup, group));
                subgroups.putIfAbsent(name, group);
            }));
            return new TradeGroup(
                    first.replace,
                    first.minTrades + second.minTrades,
                    first.maxTrades + second.maxTrades,
                    first.randomness * second.randomness,
                    first.affinity,
                    Stream.concat(first.offers.stream(), second.offers.stream()).collect(Collectors.toList()),
                    Optional.of(subgroups)
            );
        }
    }

    public static abstract class TradeSelector {
        public abstract double getWeight();
        public abstract List<ExtendedTradeOffer.Factory> getSelectedTrades();
        public abstract boolean canSelect();
        public abstract void selectOne();

        public static double weightFunction(Map<String, Double> merchantAttributes, Map<String, Double> affinities, double normalization, double exp) {
            final Double[] affinity = new Double[1];
            affinity[0] = 0.0;
            affinities.forEach((name, value) -> {
                affinity[0] += (value / normalization) * (merchantAttributes.getOrDefault(name, 0.0));
            });
            affinity[0] = affinity[0] + 0.0 >= 0.0 ? (1 + Math.pow(affinity[0], exp)) : (1 / Math.pow(-affinity[0], exp));
            return affinity[0];
        }
    }

    public static class TradeGroupSelector extends TradeSelector {
        private final Collection<TradeGroup.TradeSelector> allSelectors;
        private final WeightedRandomList<TradeGroup.TradeSelector> randomSelectors = new WeightedRandomList<>();
        private final int maxTrades;
        private final Optional<Double> weight;
        private final Random random;
        private static final double base_rate = 0.5;

        public TradeGroupSelector(Collection<TradeSelector> allSelectors, int minTrades, int maxTrades, Optional<Double> weight, int merchantLevel, Map<String, Double> merchantAttributes, Random random) {
            this.allSelectors = allSelectors;
            this.random = random;
            this.weight = weight;
            this.maxTrades = maxTrades;
            allSelectors.forEach(selector -> { if (selector.canSelect()) this.randomSelectors.add(selector.getWeight(), selector);});
            selectN(minTrades);
        }

        public TradeGroupSelector(TradeGroup group, int merchantLevel, Map<String, Double> merchantAttributes, Random random) {
            this(
                    Stream.concat(
                        group.offers().stream().map(offer -> new SingleTradeOfferSelector(offer, base_rate, group.randomness(), merchantLevel, merchantAttributes)),
                        new TreeMap<>(group.subGroups().orElse(Collections.emptyMap())).values().stream().map(subgroup -> new TradeGroupSelector(subgroup, merchantLevel, merchantAttributes, random))
                    ).collect(Collectors.toList()),
                    group.minTrades, group.maxTrades,
                    group.affinity().map(affinities -> weightFunction(merchantAttributes, affinities, 1, base_rate)),
                    merchantLevel, merchantAttributes, random
            );
        }

        public TradeGroupSelector(Collection<TradeGroup> groups, Optional<Double> weight, int minTrades, int maxTrades, int merchantLevel, Map<String, Double> merchantAttributes, Random random) {
            this(
                    groups.stream().map(group -> new TradeGroupSelector(group, merchantLevel, merchantAttributes, random)).collect(Collectors.toList()),
                    minTrades, maxTrades, weight, merchantLevel, merchantAttributes, random
            );
        }

        public TradeGroupSelector(Collection<TradeGroup> groups, int minTrades, int maxTrades, int merchantLevel, Map<String, Double> merchantAttributes, Random random) {
            this(groups, Optional.empty(), minTrades, maxTrades, merchantLevel, merchantAttributes, random);
        }

        @Override
        public double getWeight() {
            return this.weight.orElse(randomSelectors.getTotalWeight());
        }

        @Override
        public List<ExtendedTradeOffer.Factory> getSelectedTrades() {
            return this.allSelectors.stream().flatMap(sel -> sel.getSelectedTrades().stream()).collect(Collectors.toList());
        }

        @Override
        public boolean canSelect() {
            return this.randomSelectors.size() > 0 && this.getSelectedTrades().size() < this.maxTrades;
        }

        @Override
        public void selectOne() {
            TradeGroup.TradeSelector selector = randomSelectors.popSample(this.random);
            selector.selectOne();
            if (selector.canSelect()) {
                randomSelectors.add(selector.getWeight(), selector);
            }
        }

        private void selectN(int trades) {
            for (int i = 0; this.canSelect() && i < trades; i++) {
                this.selectOne();
            }
        }
    }

    public static class SingleTradeOfferSelector extends TradeSelector {
        private final ExtendedTradeOffer.Factory offer;
        private final Double weight;
        private final boolean invalid;
        private boolean selected = false;

        public SingleTradeOfferSelector(ExtendedTradeOffer.Factory offer, double base_rate, double randomness, int merchantLevel, Map<String, Double> merchantAttributes) {
            this.offer = offer;
            final Double[] maxValue = new Double[1];
            offer.affinity().ifPresentOrElse(
                    aff -> {
                        maxValue[0] = 1.0;
                    },
                    () -> {
                        maxValue[0] = 0.0;
                        offer.attributes().forEach((name, value) -> {
                            maxValue[0] = Math.max(maxValue[0], Math.abs(value));
                        });
                        if (maxValue[0] == 0.0) maxValue[0] = 1.0;
                    }
            );
            this.weight = weightFunction(merchantAttributes, offer.affinity().orElse(offer.attributes()), maxValue[0], base_rate / randomness);
            this.invalid = offer.level() > merchantLevel;
        }

        @Override
        public double getWeight() {
            return this.weight;
        }

        @Override
        public List<ExtendedTradeOffer.Factory> getSelectedTrades() {
            return this.selected && !this.invalid ? List.of(this.offer) : Collections.emptyList();
        }

        @Override
        public boolean canSelect() {
            return !this.selected && !this.invalid;
        }

        @Override
        public void selectOne() {
            this.selected = !this.invalid;
        }
    }
}
