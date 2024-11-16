package io.github.orlouge.dynamicvillagertrades.trade_offers.generators;

import io.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;
import io.github.orlouge.dynamicvillagertrades.trade_offers.EnchantSpecificBookFactory;
import io.github.orlouge.dynamicvillagertrades.trade_offers.SellSpecificPotionHoldingItemFactory;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;

public class AttributeUtils {
    public static <T> Map<T, Set<String>> generateUniqueAttributeSets(Map<T, Map<String, Integer>> candidateAttributes, int nSecondaryAttributes) {
        Map<String, Integer> attributePriority = new HashMap<>();
        Map<String, HashSet<T>> attributeEntries = new HashMap<>();
        Set<T> entriesNeedingAttributes = new HashSet<>();
        for (Map.Entry<T, Map<String, Integer>> entry : candidateAttributes.entrySet()) {
            for (Map.Entry<String, Integer> attribute : entry.getValue().entrySet()) {
                Integer previousPriority = attributePriority.getOrDefault(attribute.getKey(), 0);
                attributePriority.put(attribute.getKey(), previousPriority + attribute.getValue());
                Set<T> entriesSet = attributeEntries.computeIfAbsent(attribute.getKey(), s -> new HashSet<>());
                entriesSet.add(entry.getKey());
            }
            entriesNeedingAttributes.add(entry.getKey());
        }
        Map<Set<T>, Integer> entrieSetPriorities = new HashMap<>();
        for (Map.Entry<String, HashSet<T>> entry : attributeEntries.entrySet()) {
            Integer priority = attributePriority.getOrDefault(entry.getKey(), 0);
            Integer previousPriority = entrieSetPriorities.get(entry.getValue());
            if (previousPriority == null) {
                entrieSetPriorities.put(entry.getValue(), priority);
            } else if (priority > previousPriority) {
                entrieSetPriorities.put(entry.getValue(), priority);
                attributePriority.remove(entry.getKey());
            }
        }
        PriorityQueue<Map.Entry<String, Integer>> attributePriorityQueue = new PriorityQueue<>(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed());
        Map<T, Set<String>> entryAttributes = new HashMap<>();
        attributePriorityQueue.addAll(attributePriority.entrySet());
        nextAttribute:
        while (entriesNeedingAttributes.size() > 0 && attributePriorityQueue.size() > 0) {
            String attribute = attributePriorityQueue.poll().getKey();
            Set<Set<String>> attributeSets = new HashSet<>();
            for (T entry : attributeEntries.get(attribute)) {
                if (!entriesNeedingAttributes.contains(entry)) continue nextAttribute;
                Set<String> attributeSet = entryAttributes.getOrDefault(entry, new HashSet<>());
                if (attributeSet.size() >= nSecondaryAttributes - 1) {
                    if (attributeSets.contains(attributeSet)) continue nextAttribute;
                    attributeSets.add(attributeSet);
                }
            }
            for (T entry : attributeEntries.get(attribute)) {
                Set<String> attributeSet = entryAttributes.computeIfAbsent(entry, s -> new HashSet<>());
                attributeSet.add(attribute);
                if (attributeSet.size() == nSecondaryAttributes) entriesNeedingAttributes.remove(entry);
            }
        }
        return entryAttributes;
    }

    public static Optional<Item> getTradeItem(TradeOffers.Factory factory) {
        if (factory instanceof TradeOffers.BuyItemFactory buyfactory) {
            return Optional.of(((TradeOffersAccessor.BuyItemFactoryAccessor) buyfactory).getStack().itemStack().getItem());
        }
        if (factory instanceof TradeOffers.SellItemFactory sellfactory) {
            return Optional.of(((TradeOffersAccessor.SellItemFactoryAccessor) sellfactory).getSell().getItem());
        }
        if (factory instanceof TradeOffers.ProcessItemFactory processfactory) {
            return Optional.of(((TradeOffersAccessor.ProcessItemFactoryAccessor) processfactory).getProcessed().getItem());
        }
        if (factory instanceof TradeOffers.SellEnchantedToolFactory sellfactory) {
            return Optional.of(((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) sellfactory).getTool().getItem());
        }
        if (factory instanceof TradeOffers.TypeAwareBuyForOneEmeraldFactory buyfactory) {
            return ((TradeOffersAccessor.TypeAwareBuyForOneEmeraldFactoryAccessor) buyfactory).getMap().values().stream().findFirst();
        }
        if (factory instanceof TradeOffers.SellPotionHoldingItemFactory sellfactory) {
            return Optional.of(((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) sellfactory).getSell().getItem());
        }
        if (factory instanceof TradeOffers.SellDyedArmorFactory sellfactory) {
            return Optional.of(((TradeOffersAccessor.SellDyedArmorFactoryAccessor) sellfactory).getSell());
        }
        return Optional.empty();
    }

    public static String generateTradeAttributeName(TradeOffers.Factory factory, String fallback) {
        if (factory instanceof TradeOffers.SellSuspiciousStewFactory stewfactory) {
            Optional<RegistryEntry<StatusEffect>> entry = ((TradeOffersAccessor.SellSuspiciousStewFactoryAccessor) stewfactory).getStewEffects().effects().stream().findFirst().map(e -> e.effect());
            if (entry.isPresent() && entry.get().getKey().isPresent()) return entry.get().getKey().get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.EnchantBookFactory) {
            return "enchanted_book";
        }
        if (factory instanceof TradeOffers.SellMapFactory sellfactory) {
            RegistryKey<? extends Registry<Structure>> key = ((TradeOffersAccessor.SellMapFactoryAccessor) sellfactory).getStructure().registry();
            return key.getValue().getPath();
        }
        if (factory instanceof EnchantSpecificBookFactory enchantfactory) {
            return enchantfactory.getEnchantmentType().getPath();
        }
        if (factory instanceof SellSpecificPotionHoldingItemFactory sellpotionfactory) {
            return sellpotionfactory.getPotion().getPath();
        }
        return getTradeItem(factory).map(i -> Registries.ITEM.getId(i).getPath()).orElse(fallback);
    }


    public static Optional<NoteBlockInstrument> getBlockInstrument(Item item) {
        if (item instanceof BlockItem blockItem) {
            return Optional.of(blockItem.getBlock().getDefaultState().getInstrument());
        }
        return Optional.empty();
    }

    public static Optional<MapColor> getColor(Item item) {
        if (item instanceof DyeItem dyeItem) {
            return Optional.of(dyeItem.getColor().getMapColor());
        } else if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BedBlock bed) return Optional.of(bed.getColor().getMapColor());
            if (block instanceof BannerBlock banner) return Optional.of(banner.getColor().getMapColor());
            return Optional.of(block.getDefaultMapColor());
        }
        return Optional.empty();
    }

    public static Optional<Map<String, Double>> getColorAttributes(Item item) {
        return getColor(item).map(mapColor -> {
            int red = (mapColor.color >> 16) & 0xFF, green = (mapColor.color >> 8) & 0xFF, blue = mapColor.color & 0xFF;
            int max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
            double redness = ((double) red - min) / 255, greenness = ((double) green - min) / 255, blueness = ((double) blue - min) / 255;
            double brightness = ((double) red + blue + green) / 382 - 1, saturation = ((double) max - min) / 127 - 1;
            redness = redness > 0.25 ? redness : 0;
            greenness = greenness > 0.25 ? greenness : 0;
            blueness = blueness > 0.25 ? blueness : 0;
            brightness = Math.abs(brightness) > 0.25 ? brightness : 0;
            saturation = Math.abs(saturation) > 0.25 ? saturation : 0;
            double sum = redness + greenness + blueness + Math.abs(brightness) + Math.abs(saturation);
            sum = sum > 0 ? sum : 1;
            return Map.of(
                    "red", redness / sum,
                    "green", greenness / sum,
                    "blue", blueness / sum,
                    "saturation", saturation / sum,
                    "brightness", brightness / sum
            );
        });
    }

    public static Optional<ArmorMaterial> getArmorMaterial(Item item) {
        if (item instanceof ArmorItem armorItem) return Optional.of(armorItem.getMaterial().value());
        return Optional.empty();
    }
    public static Optional<ToolMaterial> getToolMaterial(Item item) {
        if (item instanceof ToolItem toolItem) return Optional.of(toolItem.getMaterial());
        return Optional.empty();
    }

    public static Optional<Item> getRepairItem(Ingredient ingredient) {
        ItemStack[] matchingStacks = ingredient.getMatchingStacks();
        if (matchingStacks.length > 0) {
            return Optional.of(matchingStacks[0].getItem());
        }
        return Optional.empty();
    }
    public static Optional<String> getMod(Item item) {
        String namespace = Registries.ITEM.getId(item).getNamespace();
        return namespace == Identifier.DEFAULT_NAMESPACE ? Optional.empty() : Optional.of(namespace);
    }

    public static Optional<String> getIngotMaterialName(Item item, boolean alwaysReturn) {
        String name = Registries.ITEM.getId(item).getPath();
        if (name.endsWith("_ingot")) return Optional.of(name.replace("_ingot", ""));
        if (name.equals("diamond")) return Optional.of("diamond");
        if (alwaysReturn) {
            return Optional.of(name.split("_")[0]);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<String> getToolOrArmorMaterialName(Item item) {
        return getArmorMaterial(item).map(Registries.ARMOR_MATERIAL::getEntry).flatMap(e -> e.getKey().map(k -> k.getValue().getPath())).or(() ->
                getToolMaterial(item).map(ToolMaterial::getRepairIngredient).or(() ->
                        getArmorMaterial(item).map(m -> m.repairIngredient().get())
                ).flatMap(AttributeUtils::getRepairItem).flatMap(m -> getIngotMaterialName(m, true))
        );
    }

    public static Optional<EquipmentSlot> getArmorType(Item item) {
        if (item instanceof ArmorItem armorItem) return Optional.of(armorItem.getSlotType());
        return Optional.empty();
    }
}
