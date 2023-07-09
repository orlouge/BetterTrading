package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;
import io.github.orlouge.dynamicvillagertrades.trade_offers.EnchantSpecificBookFactory;
import io.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.enchantment.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LibrarianGenerator extends VanillaLikeGenerator {
    @Override
    public void reset() {
        this.enchantmentAttributesGenerator = null;
    }

    @Override
    protected boolean professionFilter(String profession) {
        return profession.equals(VillagerProfession.LIBRARIAN.id());
    }

    @Override
    protected TradeGroup tradeGroupAtLevel(Integer level, String levelName, TradeOffers.Factory[] trades) {
        if (enchantmentAttributesGenerator == null) enchantmentAttributesGenerator = new EnchantmentAttributesGenerator();
        List<ExtendedTradeOffer.Factory> offers = new ArrayList<>();
        Map<String, TradeGroup> subgroups = new HashMap<>();
        List<TradeOffers.EnchantBookFactory> enchantedTrades = new ArrayList<>();
        List<TradeOffers.Factory> regularTrades = new ArrayList<>();
        for (int i = 0; i < trades.length; i++) {
            if (trades[i] instanceof TradeOffers.EnchantBookFactory enchantBookFactory) {
                enchantedTrades.add(enchantBookFactory);
            } else {
                regularTrades.add(trades[i]);
            }
        }
        for (int i = 0; i < regularTrades.size(); i++) {
            Map<String, Double> attributes;
            if (trades.length > 2) {
                attributes = Map.of(AttributeUtils.generateTradeAttributeName(regularTrades.get(i), levelName + "_" + i), 0.3, "enchanter", -0.5);
            } else {
                attributes = Map.of("enchanter", -0.5);
            }
            offers.add(cachedOffer(regularTrades.get(i), i, level, levelName, attributes));
        }
        for (int i = 0; i < enchantedTrades.size(); i++) {
            int experience = ((TradeOffersAccessor.EnchantBookFactoryAccessor) enchantedTrades.get(i)).getExperience();
            Map<String, Double> affinities = Map.of("enchanter", 1.0);
            List<ExtendedTradeOffer.Factory> suboffers = new ArrayList<>();
            for (Pair<Enchantment, Integer> enchantment : enchantmentAttributesGenerator.getAllEnchantments()) {
                Map<String, Double> attributes = enchantmentAttributesGenerator.getAttributes(enchantment.getLeft(), enchantment.getRight());
                TradeOffers.Factory trade = new EnchantSpecificBookFactory(
                        experience,
                        Registry.ENCHANTMENT.getId(enchantment.getLeft()),
                        enchantment.getRight()
                );
                Optional<String> key = Optional.empty();
                if (DynamicVillagerTradesMod.NO_BOOK_DUPLICATES) {
                    key = Optional.of(Registry.ENCHANTMENT.getId(enchantment.getLeft()) + "");
                }
                suboffers.add(new ExtendedTradeOffer.Factory(trade, level, attributes, Optional.empty(), key, false));
            }
            subgroups.put("" + (i + 1) * level + "_enchanted_books_" + levelName, new TradeGroup(false, 0, 1, 0.1, Optional.of(affinities), suboffers, Optional.empty(), Optional.empty()));
        }
        int min_trades = Math.min(trades.length, 2);
        return new TradeGroup(false, min_trades, min_trades, 1.0, Optional.empty(), offers, Optional.of(subgroups), Optional.of("enchantments"));
    }

    private static EnchantmentAttributesGenerator enchantmentAttributesGenerator = null;

    private static class EnchantmentAttributesGenerator {
        private final Map<Enchantment, Map<String, Double>> levelIndependentAttributes;

        public EnchantmentAttributesGenerator() {
            this.levelIndependentAttributes = getLevelIndependentAttributes(1.0, 1.0, 3);
        }

        public Collection<Pair<Enchantment, Integer>> getAllEnchantments() {
            return this.levelIndependentAttributes.keySet().stream().flatMap(
                    ench -> IntStream.range(ench.getMinLevel(), ench.getMaxLevel() + 1).mapToObj(
                            level -> new Pair<>(ench, level)
                    )
            ).collect(Collectors.toList());
        }

        public Map<String, Double> getAttributes(Enchantment enchantment, int level) {
            Map<String, Double> attributes = new HashMap<>(levelIndependentAttributes.getOrDefault(enchantment, new HashMap<>()));
            double levelRatio = ((double) enchantment.getMaxLevel() - level) / enchantment.getMaxLevel();
            attributes.put("low_level", levelRatio * 2 - 1);
            attributes.put("enchanter", 1.0);
            return attributes;
        }

        private Map<Enchantment, Map<String, Double>> getLevelIndependentAttributes(double primary, double secondary, int nSecondaryAttributes) {
            Map<Enchantment, Map<String, Double>> attributes = new HashMap<>();
            Map<Enchantment, Map<String, Integer>> candidateSecondaryAttributes = new HashMap<>();
            Registry.ENCHANTMENT.forEach(enchantment -> {
                if (!enchantment.isAvailableForEnchantedBookOffer()) return;
                Map<String, Double> enchantmentAttributes = new HashMap<>();
                enchantmentAttributes.put(getPrimaryAttribute(enchantment), primary);
                attributes.put(enchantment, enchantmentAttributes);
                candidateSecondaryAttributes.put(enchantment, getCandidateSecondaryAttributes(enchantment));
            });
            for (Map.Entry<Enchantment, Set<String>> secondaryAttributes : AttributeUtils.generateUniqueAttributeSets(candidateSecondaryAttributes, nSecondaryAttributes).entrySet()) {
                Map<String, Double> enchantmentAttributes = attributes.get(secondaryAttributes.getKey());
                if (enchantmentAttributes != null) {
                    for (String secondaryAttribute : secondaryAttributes.getValue()) {
                        double attributeValue = secondary / secondaryAttributes.getValue().size();
                        if (negativeAttributes.containsKey(secondaryAttribute)) {
                            enchantmentAttributes.putIfAbsent(negativeAttributes.get(secondaryAttribute), -attributeValue);
                        } else {
                            enchantmentAttributes.putIfAbsent(secondaryAttribute, attributeValue);
                        }
                    }
                }
            }
            return attributes;
        }

        private Map<String, Integer> getCandidateSecondaryAttributes(Enchantment enchantment) {
            Map<String, Integer> attributes = new HashMap<>();
            if (enchantment.isCursed()) attributes.put("curse", 10000);
            if (enchantment.isTreasure()) attributes.put("treasure", 10000);
            if (enchantment instanceof ProtectionEnchantment) attributes.put("defense", 1000);
            if (enchantment instanceof LuckEnchantment) attributes.put("luck", 1000);
            if (enchantment.type == EnchantmentTarget.BOW || enchantment.type == EnchantmentTarget.CROSSBOW || enchantment.type == EnchantmentTarget.TRIDENT) attributes.put("ranged", 100);
            if (enchantment.type == EnchantmentTarget.TRIDENT || enchantment.type == EnchantmentTarget.FISHING_ROD) attributes.put("water", 100);
            if (enchantment.type == EnchantmentTarget.WEAPON) attributes.put("melee", 100);
            if (enchantment instanceof DamageEnchantment || enchantment instanceof PowerEnchantment || enchantment instanceof ImpalingEnchantment) attributes.put("offense", 10);
            if (enchantment instanceof FireAspectEnchantment || enchantment instanceof ChannelingEnchantment || enchantment instanceof MultishotEnchantment || enchantment instanceof ThornsEnchantment) attributes.put("offense", 10);
            if (enchantment instanceof ThornsEnchantment) attributes.put("melee", 100);
            if (enchantment instanceof UnbreakingEnchantment || enchantment instanceof MendingEnchantment || enchantment instanceof SilkTouchEnchantment || enchantment instanceof InfinityEnchantment) attributes.put("resource", 10);
            if (enchantment instanceof AquaAffinityEnchantment || enchantment instanceof EfficiencyEnchantment || enchantment instanceof DepthStriderEnchantment || enchantment instanceof LureEnchantment || enchantment instanceof RiptideEnchantment) attributes.put("speed", 100);
            if (isFire(enchantment)) attributes.put("fire", 100);
            if (enchantment.type == EnchantmentTarget.ARMOR_CHEST) attributes.put("armor_chest", 2);
            if (enchantment.type == EnchantmentTarget.ARMOR_FEET) attributes.put("armor_feet", 2);
            if (enchantment.type == EnchantmentTarget.ARMOR_LEGS) attributes.put("armor_legs", 2);
            if (enchantment.type == EnchantmentTarget.ARMOR_HEAD) attributes.put("armor_head", 2);
            Optional.ofNullable(Registry.ENCHANTMENT.getId(enchantment)).ifPresent(id -> {
                List<String> keywords = List.of(id.getPath().split("_"));
                for (String keyword : keywords) {
                    String attribute = knownKeywords.get(keyword);
                    if (attribute != null) {
                        attributes.put(attribute, 10);
                    } else {
                        if (blacklistedKeywords.contains(keyword)) continue;
                        attributes.put(keyword, 2);
                    }
                }
                attributes.put(id.getPath(), 3);
                if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) attributes.put(id.getNamespace(), 1000);
            });
            return attributes;
        }

        private boolean isFire(Enchantment enchantment) {
            return enchantment instanceof FireAspectEnchantment || enchantment instanceof FlameEnchantment ||
                    (enchantment instanceof ProtectionEnchantment prot && prot.protectionType == ProtectionEnchantment.Type.FIRE);
        }

        private String getPrimaryAttribute(Enchantment enchantment) {
            return switch (enchantment.type) {
                case ARMOR, ARMOR_HEAD, ARMOR_LEGS, ARMOR_CHEST, ARMOR_FEET, WEARABLE -> "armor";
                case BOW -> "bow";
                case CROSSBOW -> "crossbow";
                case DIGGER -> "tool";
                case FISHING_ROD -> "fishing";
                case TRIDENT -> "trident";
                case WEAPON -> "weapon";
                default -> "all";
            };
        }

        private static final Set<String> blacklistedKeywords = Set.of("of", "the", "curse");
        private static final Map<String, String> knownKeywords = Map.ofEntries(
                Map.entry("fire", "fire"),
                Map.entry("flame", "fire"),
                Map.entry("lava", "fire"),
                Map.entry("magma", "fire"),
                Map.entry("pyromania", "fire"),
                Map.entry("burn", "fire"),
                Map.entry("burning", "fire"),
                Map.entry("blast", "explosion"),
                Map.entry("explosion", "explosion"),
                Map.entry("exploding", "explosion"),
                Map.entry("arrow", "ranged"),
                Map.entry("projectile", "ranged"),
                Map.entry("shot", "ranged"),
                Map.entry("ranged", "ranged"),
                Map.entry("shotgun", "ranged"),
                Map.entry("sniper", "ranged"),
                Map.entry("melee", "melee"),
                Map.entry("hit", "melee"),
                Map.entry("critical", "melee"),
                Map.entry("swift", "speed"),
                Map.entry("efficiency", "speed"),
                Map.entry("quick", "speed"),
                Map.entry("speed", "speed"),
                Map.entry("looting", "luck"),
                Map.entry("luck", "luck"),
                Map.entry("lucky", "luck"),
                Map.entry("fortune", "luck"),
                Map.entry("aspect", "aspect"),
                Map.entry("protection", "defense"),
                Map.entry("defense", "defense"),
                Map.entry("tank", "defense"),
                Map.entry("resistance", "defense"),
                Map.entry("water", "water"),
                Map.entry("sea", "water"),
                Map.entry("ocean", "water"),
                Map.entry("aqua", "water"),
                Map.entry("frost", "water"),
                Map.entry("ice", "water"),
                Map.entry("freezing", "water"),
                Map.entry("glacial", "water"),
                Map.entry("respiration", "water"),
                Map.entry("channeling", "thunder"),
                Map.entry("thunder", "thunder"),
                Map.entry("poison", "poison")
        );
        private static final Map<String, String> negativeAttributes = Map.of("water", "fire", "ranged", "melee");
    }
}
