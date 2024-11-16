package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;
import io.github.orlouge.dynamicvillagertrades.trade_offers.EnchantSpecificBookFactory;
import io.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.*;
import net.minecraft.enchantment.effect.AttributeEnchantmentEffect;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LibrarianGenerator extends VanillaLikeGenerator {
    @Override
    public void reset() {
        enchantmentAttributesGenerator = null;
    }

    @Override
    protected boolean professionFilter(String profession) {
        return profession.equals(VillagerProfession.LIBRARIAN.id());
    }

    @Override
    protected TradeGroup tradeGroupAtLevel(Integer level, String levelName, TradeOffers.Factory[] trades, DynamicRegistryManager registryManager) {
        if (enchantmentAttributesGenerator == null)  {
            enchantmentAttributesGenerator = new EnchantmentAttributesGenerator(registryManager);
        } else if (registryManager != null) {
            enchantmentAttributesGenerator.registryManager = registryManager;
        }
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
                        registryManager.get(RegistryKeys.ENCHANTMENT).getId(enchantment.getLeft()),
                        enchantment.getRight()
                );
                Optional<String> key = Optional.empty();
                if (DynamicVillagerTradesMod.NO_BOOK_DUPLICATES) {
                    key = Optional.of(registryManager.get(RegistryKeys.ENCHANTMENT).getId(enchantment.getLeft()) + "");
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
        public DynamicRegistryManager registryManager;

        public EnchantmentAttributesGenerator(DynamicRegistryManager registryManager) {
            this.registryManager = registryManager;
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
            registryManager.get(RegistryKeys.ENCHANTMENT).streamEntries().forEach(enchantment -> {
                if (!enchantment.isIn(EnchantmentTags.TRADEABLE)) return;
                Map<String, Double> enchantmentAttributes = new HashMap<>();
                String primaryAttribute = getPrimaryAttribute(enchantment);
                enchantmentAttributes.put(primaryAttribute, primary);
                attributes.put(enchantment.value(), enchantmentAttributes);
                candidateSecondaryAttributes.put(enchantment.value(), getCandidateSecondaryAttributes(primaryAttribute, enchantment));
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

        private Map<String, Integer> getCandidateSecondaryAttributes(String primaryAttribute, RegistryEntry<Enchantment> enchantment) {
            Map<String, Integer> attributes = new HashMap<>();
            ComponentMap effects = enchantment.value().effects();
            List<AttributeEnchantmentEffect> entityAttributeEffects = effects.getOrDefault(EnchantmentEffectComponentTypes.ATTRIBUTES, Collections.emptyList());
            List<RegistryEntry<EntityAttribute>> entityAttributes = entityAttributeEffects.stream().map(AttributeEnchantmentEffect::attribute).toList();
            if (enchantment.isIn(EnchantmentTags.CURSE)) attributes.put("curse", 10000);
            if (enchantment.isIn(EnchantmentTags.TREASURE)) attributes.put("treasure", 10000);
            if (effects.contains(EnchantmentEffectComponentTypes.DAMAGE_PROTECTION)) attributes.put("defense", 1000);
            if (effects.contains(EnchantmentEffectComponentTypes.FISHING_LUCK_BONUS) || enchantment == Enchantments.FORTUNE || enchantment == Enchantments.LOOTING) attributes.put("luck", 1000);
            if (primaryAttribute.equals("bow") || primaryAttribute.equals("crossbow") || primaryAttribute.equals("trident")) attributes.put("ranged", 100);
            if (primaryAttribute.equals("trident") || primaryAttribute.equals("fishing")) attributes.put("water", 100);
            if (primaryAttribute.equals("sword") || primaryAttribute.equals("mace")) attributes.put("melee", 100);
            //if (enchantment.isIn(EnchantmentTags.DAMAGE_EXCLUSIVE_SET) || effects.contains(EnchantmentEffectComponentTypes.DAMAGE) || effects.contains(EnchantmentEffectComponentTypes.POST_ATTACK)) attributes.put("offense", 10);
            //if (effects.contains(EnchantmentEffectComponentTypes.HIT_BLOCK) || effects.contains(EnchantmentEffectComponentTypes.PROJECTILE_COUNT)) attributes.put("offense", 10);
            if (enchantment.matchesKey(Enchantments.THORNS)) attributes.put("melee", 100);
            if (effects.contains(EnchantmentEffectComponentTypes.ITEM_DAMAGE) || effects.contains(EnchantmentEffectComponentTypes.REPAIR_WITH_XP) || effects.contains(EnchantmentEffectComponentTypes.BLOCK_EXPERIENCE) || effects.contains(EnchantmentEffectComponentTypes.AMMO_USE)) attributes.put("resource", 10);
            if (entityAttributes.contains(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED) || entityAttributes.contains(EntityAttributes.PLAYER_MINING_EFFICIENCY)  || entityAttributes.contains(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY) || effects.contains(EnchantmentEffectComponentTypes.FISHING_TIME_REDUCTION) || effects.contains(EnchantmentEffectComponentTypes.TRIDENT_SPIN_ATTACK_STRENGTH)) attributes.put("speed", 100);
            if (enchantment.matchesKey(Enchantments.FIRE_ASPECT) || enchantment.matchesKey(Enchantments.FLAME) || entityAttributes.contains(EntityAttributes.GENERIC_BURNING_TIME)) attributes.put("fire", 100);
            if (primaryAttribute.equals("armor")) {
                List<AttributeModifierSlot> slots = enchantment.value().definition().slots();
                if (slots.contains(AttributeModifierSlot.CHEST)) attributes.put("armor_chest", 2);
                if (slots.contains(AttributeModifierSlot.FEET)) attributes.put("armor_feet", 2);
                if (slots.contains(AttributeModifierSlot.LEGS)) attributes.put("armor_legs", 2);
                if (slots.contains(AttributeModifierSlot.HEAD)) attributes.put("armor_head", 2);
            }
            enchantment.getKey().map(RegistryKey::getValue).ifPresent(id -> {
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

        private String getPrimaryAttribute(RegistryEntry<Enchantment> enchantment) {
            List<AttributeModifierSlot> slots = enchantment.value().definition().slots();
            boolean armor_slots = false, nonarmor_slots = false;
            for (AttributeModifierSlot slot : slots) {
                if (slot.equals(AttributeModifierSlot.ARMOR) || slot.equals(AttributeModifierSlot.BODY) || slot.equals(AttributeModifierSlot.HEAD) || slot.equals(AttributeModifierSlot.CHEST) || slot.equals(AttributeModifierSlot.LEGS) || slot.equals(AttributeModifierSlot.FEET)) {
                    armor_slots = true;
                } else {
                    if (slot.equals(AttributeModifierSlot.ANY) && !enchantment.matchesKey(Enchantments.THORNS)) return "all";
                    nonarmor_slots = true;
                }
            }
            if (enchantment.isIn(EnchantmentTags.ARMOR_EXCLUSIVE_SET) || enchantment.isIn(EnchantmentTags.BOOTS_EXCLUSIVE_SET) || (armor_slots && !nonarmor_slots)) return "armor";
            if (enchantment.isIn(EnchantmentTags.BOW_EXCLUSIVE_SET)) return "bow";
            if (enchantment.isIn(EnchantmentTags.CROSSBOW_EXCLUSIVE_SET)) return "crossbow";
            if (enchantment.isIn(EnchantmentTags.MINING_EXCLUSIVE_SET)) return "tool";
            if (enchantment.isIn(EnchantmentTags.RIPTIDE_EXCLUSIVE_SET)) return "trident";
            boolean armor = false, mining = false, fishing = false, trident = false, sword = false, mace = false, bow = false, crossbow = false;
            if (Registries.ITEM.getEntryList(ItemTags.ARMOR_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) armor = true;
            if (Registries.ITEM.getEntryList(ItemTags.MINING_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) mining = true;
            if (Registries.ITEM.getEntryList(ItemTags.BOW_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) bow = true;
            if (Registries.ITEM.getEntryList(ItemTags.CROSSBOW_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) crossbow = true;
            if (Registries.ITEM.getEntryList(ItemTags.MACE_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) mace = true;
            if (Registries.ITEM.getEntryList(ItemTags.FISHING_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) fishing = true;
            if (Registries.ITEM.getEntryList(ItemTags.TRIDENT_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) trident = true;
            if (Registries.ITEM.getEntryList(ItemTags.SWORD_ENCHANTABLE).get().stream().allMatch(item -> enchantment.value().definition().supportedItems().contains(item))) sword = true;
            if (!mining && !bow && !crossbow && armor && !fishing && !trident && !sword && !mace) return "armor";
            if (mining && !bow && !crossbow && !armor && !fishing && !trident && !sword && !mace) return "tool";
            if (!mining && bow && !armor && !fishing && !trident && !sword && !mace) return "bow";
            if (!mining && !bow && crossbow && !armor && !fishing && !trident && !sword && !mace) return "crossbow";
            if (!mining && !bow && !crossbow && !armor && fishing && !trident && !sword && !mace) return "fishing";
            if (!mining && !bow && !crossbow && !armor && !fishing && trident && !sword && !mace) return "trident";
            if (!bow && !crossbow && !armor && !fishing && sword) return "sword";
            if (!bow && !crossbow && !armor && !fishing && mace) return "mace";
            return "all";
        }


        private static final Set<String> blacklistedKeywords = Set.of("of", "the", "curse");
        private static final Map<String, String> knownKeywords = Map.ofEntries(
                Map.entry("fire", "fire"),
                Map.entry("flame", "fire"),
                Map.entry("lava", "fire"),
                Map.entry("magma", "fire"),
                Map.entry("pyromania", "fire"),
                Map.entry("burn", "fire"),
                Map.entry("blaze", "fire"),
                Map.entry("burning", "fire"),
                Map.entry("smelt", "fire"),
                Map.entry("forge", "fire"),
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
