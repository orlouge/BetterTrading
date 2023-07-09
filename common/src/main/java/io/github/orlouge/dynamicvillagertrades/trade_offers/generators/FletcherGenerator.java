package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;
import io.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import io.github.orlouge.dynamicvillagertrades.trade_offers.SellSpecificPotionHoldingItemFactory;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.*;

public class FletcherGenerator extends VanillaLikeGenerator
{
    @Override
    public void reset() {
        potionAttributesGenerator = null;
    }

    @Override
    protected boolean professionFilter(String profession) {
        return profession.equals(VillagerProfession.FLETCHER.id());
    }

    @Override
    protected TradeGroup tradeGroupAtLevel(Integer level, String levelName, TradeOffers.Factory[] trades) {
        if (potionAttributesGenerator == null) potionAttributesGenerator = new PotionAttributesGenerator();
        List<ExtendedTradeOffer.Factory> offers = new ArrayList<>();
        Map<String, TradeGroup> subgroups = new HashMap<>();
        List<TradeOffers.SellPotionHoldingItemFactory> potionHoldingTrades = new ArrayList<>();
        List<TradeOffers.Factory> regularTrades = new ArrayList<>();
        for (int i = 0; i < trades.length; i++) {
            if (trades[i] instanceof TradeOffers.SellPotionHoldingItemFactory potionHoldingItemFactory) {
                potionHoldingTrades.add(potionHoldingItemFactory);
            } else {
                regularTrades.add(trades[i]);
            }
        }
        for (int i = 0; i < regularTrades.size(); i++) {
            Map<String, Double> attributes;
            double tipped = AttributeUtils.getTradeItem(regularTrades.get(i)).map(item -> item instanceof ArrowItem ? 1.0 : -0.1).orElse(-0.1);
            attributes = Map.of("arrow", tipped);
            if (trades.length > 2) {
                String trade_name = AttributeUtils.generateTradeAttributeName(regularTrades.get(i), levelName + "_" + i);
                if (!trade_name.equals("arrow")) attributes = Map.of(trade_name, 0.3, "arrow", tipped);
            }
            offers.add(cachedOffer(regularTrades.get(i), i, level, levelName, attributes));
        }
        for (int i = 0; i < potionHoldingTrades.size(); i++) {
            TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor accessor = (TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) potionHoldingTrades.get(i);
            Map<String, Double> affinities = Map.of("arrow", 1.0);
            List<ExtendedTradeOffer.Factory> suboffers = new ArrayList<>();
            for (Potion potion : potionAttributesGenerator.getAllPotions()) {
                Map<String, Double> attributes = potionAttributesGenerator.getAttributes(potion);
                TradeOffers.Factory trade = new SellSpecificPotionHoldingItemFactory(
                        accessor.getSecondBuy(),
                        accessor.getSecondCount(),
                        accessor.getSell().getItem(),
                        accessor.getSellCount(),
                        accessor.getPrice(),
                        accessor.getMaxUses(),
                        accessor.getExperience(),
                        Registry.POTION.getId(potion)
                );
                suboffers.add(new ExtendedTradeOffer.Factory(trade, level, attributes, Optional.empty(), Optional.empty(), false));
            }
            subgroups.put("" + (i + 1) * level + "_potion_holding_" + levelName, new TradeGroup(false, 0, 1, 0.3, Optional.of(affinities), suboffers, Optional.empty(), Optional.empty()));
        }
        int min_trades = Math.min(trades.length, 2);
        return new TradeGroup(false, min_trades, min_trades, 1.0, Optional.empty(), offers, Optional.of(subgroups), Optional.empty());
    }

    private static PotionAttributesGenerator potionAttributesGenerator = null;

    private static class PotionAttributesGenerator {
        private final Map<Potion, Map<String, Double>> allPotionAttributes;

        private PotionAttributesGenerator() {
            this.allPotionAttributes = getAllAttributes(1, 3, 4);
        }

        public Collection<Potion> getAllPotions() {
            return allPotionAttributes.keySet();
        }

        public Map<String, Double> getAttributes(Potion potion) {
            return allPotionAttributes.getOrDefault(potion, Collections.emptyMap());
        }

        private Map<Potion, Map<String, Double>> getAllAttributes(double primary, double secondary, int nSecondaryAttributes) {
            Map<Potion, Map<String, Double>> attributes = new HashMap<>();
            Map<Potion, Map<String, Integer>> candidateSecondaryAttributes = new HashMap<>();
            Registry.POTION.forEach(potion -> {
                if (potion.getEffects().isEmpty() || !BrewingRecipeRegistry.isBrewable(potion)) return;
                Map<String, Double> potionAttributes = new HashMap<>(getPrimaryAttributes(potion));
                attributes.put(potion, potionAttributes);
                candidateSecondaryAttributes.put(potion, getSecondaryAttributes(potion));
            });
            for (Map.Entry<Potion, Set<String>> secondaryAttributes : AttributeUtils.generateUniqueAttributeSets(candidateSecondaryAttributes, nSecondaryAttributes).entrySet()) {
                Map<String, Double> potionAttributes = attributes.get(secondaryAttributes.getKey());
                if (potionAttributes != null) {
                    for (String secondaryAttribute : secondaryAttributes.getValue()) {
                        double attributeValue = secondary / secondaryAttributes.getValue().size();
                        potionAttributes.putIfAbsent(secondaryAttribute, attributeValue);
                    }
                }
            }
            return attributes;
        }

        private Map<String, Double> getPrimaryAttributes(Potion potion) {
            String name = Registry.POTION.getId(potion).getPath();
            if (name.startsWith("long_")) return Map.of("long", 0.5, "strong", -0.1);
            else if (name.startsWith("strong_")) return Map.of("long", -0.1, "strong", 0.5);
            else return Map.of("long", -0.01, "strong", -0.01);
        }

        private Map<String, Integer> getSecondaryAttributes(Potion potion) {
            Map<String, Integer> attributes = new HashMap<>();
            Identifier id = Registry.POTION.getId(potion);
            for (StatusEffectInstance effect : potion.getEffects()) {
                for (String attribute : getStatusEffectAttributes(effect.getEffectType())) {
                    attributes.put(attribute, 10);
                }
            }
            if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) attributes.put(id.getNamespace(), 100);
            attributes.put(id.getPath().replace("long_", "").replace("strong_", ""), 1);
            attributes.put(id.getPath(), 0);
            return attributes;
        }

        private static Set<String> getStatusEffectAttributes(StatusEffect statusEffect) {
            Set<String> attributes = new HashSet<>();
            if (statusEffect.getCategory() == StatusEffectCategory.BENEFICIAL) attributes.add("beneficial");
            else if (statusEffect.getCategory() == StatusEffectCategory.NEUTRAL) attributes.add("neutral");
            else if (statusEffect.getCategory() == StatusEffectCategory.HARMFUL) attributes.add("harmful");
            if (statusEffect.isInstant()) attributes.add("instant");
            else attributes.add("continuous");
            Identifier id = Registry.STATUS_EFFECT.getId(statusEffect);
            if (id != null) {
                if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) attributes.add(id.getNamespace());
                String name = id.getPath();
                if (name.equals("regeneration") || name.equals("instant_damage") || name.equals("wither") ||
                    name.equals("absorption") || name.equals("instant_health") || name.equals("health_boost") ||
                    name.equals("poison")) attributes.add("health");
                if (name.contains("resistance")) attributes.add("resistance");
                if (name.equals("strength") || name.equals("weakness")) attributes.add("strength");
                if (name.equals("luck") || name.equals("unluck") || name.equals("bad_omen")) attributes.add("luck");
                if (name.equals("saturation") || name.equals("hunger")) attributes.add("hunger");
                if (name.equals("water_breathing") || name.equals("conduit_power") || name.equals("dolphins_grace")) attributes.add("water");
                if (name.equals("saturation") || name.equals("hunger") || name.equals("nausea")) attributes.add("food");
                if (name.equals("night_vision") || name.equals("invisibility") || name.equals("nausea") || name.equals("blindness") || name.equals("glowing")) attributes.add("vision");
                if (name.equals("speed") || name.equals("slowness") || name.equals("conduit_power") || name.equals("dolphins_grace") ||
                    name.equals("haste") || name.equals("mining_fatigue") ) attributes.add("speed");
                if (name.equals("speed") || name.equals("slowness") || name.equals("conduit_power") || name.equals("dolphins_grace") ||
                    name.equals("slow_falling") || name.equals("jump_boost") || name.equals("levitation")) attributes.add("movement");
            }
            return attributes;
        }
    }
}
