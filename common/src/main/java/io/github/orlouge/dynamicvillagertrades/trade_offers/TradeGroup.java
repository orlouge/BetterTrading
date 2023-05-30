package io.github.orlouge.dynamicvillagertrades.trade_offers;

import io.github.orlouge.dynamicvillagertrades.WeightedRandomList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TradeGroup(boolean replace, int minTrades, int maxTrades, double randomness,
                         Optional<Map<String, Double>> affinity,
                         List<ExtendedTradeOffer.Factory> offers) {
    public static final Codec<TradeGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("replace").forGetter(TradeGroup::replace),
            Codec.INT.fieldOf("min_trades").forGetter(TradeGroup::minTrades),
            Codec.INT.fieldOf("max_trades").forGetter(TradeGroup::maxTrades),
            Codec.DOUBLE.optionalFieldOf("randomness", 1.0).forGetter(TradeGroup::randomness),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("affinity").forGetter(TradeGroup::affinity),
            ExtendedTradeOffer.Factory.CODEC.listOf().fieldOf("trades").forGetter(TradeGroup::offers)
    ).apply(instance, TradeGroup::new));

    public static TradeGroup merge(TradeGroup first, TradeGroup second) {
        if (second.replace) {
            return second;
        } else {
            return new TradeGroup(
                    first.replace,
                    Math.max(first.minTrades, second.minTrades),
                    Math.min(first.maxTrades, second.maxTrades),
                    first.randomness,
                    first.affinity,
                    Stream.concat(first.offers.stream(), second.offers.stream()).collect(Collectors.toList())
            );
        }
    }

    public static class TradeSelector {
        private final TradeGroup group;
        private final Random random;
        private final List<ExtendedTradeOffer.Factory> selectedTrades = new LinkedList<>();
        private final WeightedRandomList<ExtendedTradeOffer.Factory> randomTrades = new WeightedRandomList<>();
        private final Optional<Double> weight;
        private static final double base_rate = 0.5;

        public TradeSelector(TradeGroup group, int merchantLevel, Map<String, Double> merchantAttributes, Random random) {
            this.group = group;
            this.random = random;

            group.offers().forEach(offer -> {
                if (offer.level() <= merchantLevel) {
                    final Double[] maxValue = new Double[1];
                    offer.affinity().ifPresentOrElse(
                            aff -> {maxValue[0] = 1.0;},
                            () -> {
                                maxValue[0] = 0.0;
                                offer.attributes().forEach((name, value) -> { maxValue[0] = Math.max(maxValue[0], Math.abs(value)); });
                                if (maxValue[0] == 0.0) maxValue[0] = 1.0;
                            }
                    );
                    double weight = weightFunction(merchantAttributes, offer.affinity().orElse(offer.attributes()), maxValue[0], base_rate / group.randomness());
                    this.randomTrades.add(weight, offer);
                    // BetterTradingMod.LOGGER.info("Weight for {}: {}", offer.toString(), weight);
                }
            });
            this.weight = group.affinity.map(affinities -> weightFunction(merchantAttributes, affinities, 1, base_rate));
        }

        public static double weightFunction(Map<String, Double> merchantAttributes, Map<String, Double> affinities, double normalization, double exp) {
            final Double[] affinity = new Double[1];
            affinity[0] = 0.0;
            affinities.forEach((name, value) -> {
                affinity[0] += (value / normalization) * (merchantAttributes.getOrDefault(name, 0.0));
            });
            affinity[0] = affinity[0] + 0.0 >= 0.0 ? (1 + Math.pow(affinity[0], exp)) : (1 / Math.pow(-affinity[0], exp));
            //affinity[0] = affinity[0] >= 0 ? Math.sqrt(affinity[0]) : -Math.sqrt(-affinity[0]);
            //return 1 / (1 + Math.exp(-beta * affinity[0]));
            return affinity[0];
        }

        public List<ExtendedTradeOffer.Factory> getSelectedTrades() {
            return selectedTrades;
        }

        public boolean canSelect() {
            return randomTrades.size() > 0 && selectedTrades.size() < group.maxTrades;
        }

        public void selectOne() {
            selectedTrades.add(randomTrades.popSample(random));
        }

        public void selectMinimal() {
            for (int i = 0; canSelect() && i < group.minTrades; i++) {
                selectOne();
            }
        }

        public double getWeight() {
            return this.weight.orElse(randomTrades.getTotalWeight());
        }
    }
}
