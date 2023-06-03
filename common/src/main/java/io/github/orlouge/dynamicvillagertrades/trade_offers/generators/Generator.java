package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.village.VillagerProfession;

import java.util.*;

public abstract class Generator {
    public void reset() {}
    public abstract Optional<Map<String, TradeGroup>> generate(VillagerProfession profession);

    public static Optional<Map<String, TradeGroup>> generateAll(VillagerProfession profession) {
        for (Generator generator : generators) {
            Optional<Map<String, TradeGroup>> trades = generator.generate(profession);
            if (trades.isPresent()) {
                return trades;
            }
        }
        return Optional.empty();
    }

    public static void resetAll() {
        for (Generator generator : generators) generator.reset();
    }

    protected static void normalizeAttributes(Map<String, Double> attributes) {
        double sum = attributes.values().stream().map(Math::abs).reduce(0.0, Double::sum);
        double finalSum = sum > 0 ? sum : 1;
        attributes.replaceAll((attr, value) -> 2 * value / finalSum);
    }


    private static List<Generator> generators = List.of(
            new TieredItemGenerator(),
            new MasonGenerator(),
            new LibrarianGenerator(),
            new ShepherdGenerator(),
            new FletcherGenerator(),
            new OneByOneVanillaLikeGenerator()
    );
}
