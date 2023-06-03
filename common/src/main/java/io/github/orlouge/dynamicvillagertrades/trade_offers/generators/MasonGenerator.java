package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import net.minecraft.block.Material;
import net.minecraft.item.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MasonGenerator extends OneByOneVanillaLikeGenerator {
    @Override
    protected boolean professionFilter(String profession) {
        return profession.equals(VillagerProfession.MASON.id());
    }

    @Override
    protected Map<String, Double> getAttributes(TradeOffers.Factory trade, Map<String, Double> defaultAttributes) {
        Optional<Item> item = AttributeUtils.getTradeItem(trade);
        Map<String, Double> attributes = new HashMap<>();

        item.flatMap(AttributeUtils::getBlockMaterial)
                .flatMap(m -> m == Material.STONE ? item.flatMap(AttributeUtils::getColorAttributes) : Optional.empty())
                .map(colors -> {attributes.put("stone", 1.0); attributes.putAll(colors); return true;})
                .map(x -> item.map(MasonGenerator::stoneModifier).map(s -> {attributes.put("raw", s.equals("raw") ? 1.0 : -1.0); return true;}).isPresent())
                .or(() -> {attributes.put("stone", -1.0); return Optional.of(true);});

        item.flatMap(AttributeUtils::getMod).ifPresent(modname -> attributes.put(modname, 1.0));
        if (attributes.size() < 3) attributes.putAll(defaultAttributes);
        normalizeAttributes(attributes);
        return attributes;
    }

    private static String stoneModifier(Item item) {
        String s[] = Registry.ITEM.getId(item).getPath().split("_");
        for (int i = 0; i < s.length; i++) {
            if (s[i].equals("polished")) return "polished";
            if (s[i].equals("chiseled")) return "chiseled";
            if (s[i].equals("cut")) return "cut";
            if (s[i].equals("smooth")) return "smooth";
            if (s[i].equals("glazed")) return "glazed";
            if (s[i].equals("bricks")) return "bricks";
            if (s[i].equals("brick")) return "brick";
            if (s[i].equals("tiles")) return "tiles";
            if (s[i].equals("tile")) return "tile";
            if (s[i].equals("pillar")) return "pillar";
        }
        return "raw";
    }
}
