package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.*;

public class ShepherdGenerator extends OneByOneVanillaLikeGenerator {
    @Override
    protected boolean professionFilter(String profession) {
        return profession.equals(VillagerProfession.SHEPHERD.id());
    }

    @Override
    protected Map<String, Double> getAttributes(TradeOffers.Factory trade, Map<String, Double> defaultAttributes) {
        Optional<Item> item = AttributeUtils.getTradeItem(trade);
        String primary = item.flatMap(ShepherdGenerator::getTradeType).orElse("other");
        Map<String, Double> attributes = new HashMap<>();
        if (primary.equals("dye") || primary.equals("bed") || primary.equals("banner") || primary.equals("carpet") || primary.equals("wool")) {
            item.flatMap(AttributeUtils::getColorAttributes).ifPresent(attributes::putAll);
        }
        item.flatMap(AttributeUtils::getMod).ifPresent(modname -> attributes.put(modname, 1.0));
        if (attributes.size() < 2) attributes.putAll(defaultAttributes);
        normalizeAttributes(attributes);
        attributes.put(primary, 1.0);
        return attributes;
    }

    private static Optional<String> getTradeType(Item item) {
        if (item instanceof DyeItem) return Optional.of("dye");
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BedBlock) return Optional.of("bed");
            if (block instanceof AbstractBannerBlock) return Optional.of("banner");
            if (block instanceof CarpetBlock) return Optional.of("carpet");
            if (block.getDefaultState().getMaterial() == Material.WOOL) return Optional.of("wool");
        }
        return Optional.empty();
    }
}
