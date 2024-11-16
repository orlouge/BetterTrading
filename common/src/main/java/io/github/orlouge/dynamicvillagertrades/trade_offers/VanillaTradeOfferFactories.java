package io.github.orlouge.dynamicvillagertrades.trade_offers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.orlouge.dynamicvillagertrades.api.CodecHelper;
import io.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;

import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;

import java.util.Optional;
import java.util.function.Supplier;

public class VanillaTradeOfferFactories {
    public static final MapCodec<TradeOffers.BuyItemFactory> BUY_FOR_ONE_EMERALD = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TradedItem.CODEC.fieldOf("item").forGetter(factory -> ((TradeOffersAccessor.BuyItemFactoryAccessor) factory).getStack()),
            Codec.INT.fieldOf("price").forGetter(factory -> ((TradeOffersAccessor.BuyItemFactoryAccessor) factory).getPrice()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.BuyItemFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.BuyItemFactoryAccessor) factory).getExperience())
    ).apply(instance, TradeOffers.BuyItemFactory::new));

    public static final MapCodec<TradeOffers.SellItemFactory> SELL_ITEM = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(factory -> ((TradeOffersAccessor.SellItemFactoryAccessor) factory).getSell().getItem()),
        Codec.INT.optionalFieldOf("price", 1).forGetter(factory -> ((TradeOffersAccessor.SellItemFactoryAccessor) factory).getPrice()),
            Codec.INT.optionalFieldOf("count", 1).forGetter(factory -> ((TradeOffersAccessor.SellItemFactoryAccessor) factory).getSell().getCount()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.SellItemFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellItemFactoryAccessor) factory).getExperience())
    ).apply(instance, TradeOffers.SellItemFactory::new));

    public static final MapCodec<TradeOffers.SellSuspiciousStewFactory> SELL_SUSPICIOUS_STEW = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SuspiciousStewEffectsComponent.CODEC.fieldOf("effects").forGetter(factory -> ((TradeOffersAccessor.SellSuspiciousStewFactoryAccessor) factory).getStewEffects()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellSuspiciousStewFactoryAccessor) factory).getExperience()),
            Codec.FLOAT.optionalFieldOf("price_multiplier", 0.05F).forGetter(factory -> ((TradeOffersAccessor.SellSuspiciousStewFactoryAccessor) factory).getMultiplier())
    ).apply(instance, TradeOffers.SellSuspiciousStewFactory::new));

    private static TradeOffers.ProcessItemFactory processItemFactory(TradedItem toBeProcessed, int count, Item processed, int sellCount, int maxUses, int experience, float multiplier, Optional<RegistryKey<EnchantmentProvider>> enchantmentProviderKey) {
        return new TradeOffers.ProcessItemFactory(toBeProcessed, count, new ItemStack(processed, sellCount), maxUses, experience, multiplier, enchantmentProviderKey);
    }

    public static final MapCodec<TradeOffers.ProcessItemFactory> PROCESS_ITEM = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TradedItem.CODEC.fieldOf("item").forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getToBeProcessed()),
            Codec.INT.optionalFieldOf("price", 1).forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getPrice()),
            Registries.ITEM.getCodec().fieldOf("sell_item").forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getProcessed().getItem()),
            Codec.INT.optionalFieldOf("sell_count", 1).forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getProcessed().getCount()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getExperience()),
           Codec.FLOAT.optionalFieldOf("price_multiplier", 0.05F).forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getMultiplier()),
            RegistryKey.createCodec(RegistryKeys.ENCHANTMENT_PROVIDER).optionalFieldOf("enchantment_provider").forGetter(factory -> ((TradeOffersAccessor.ProcessItemFactoryAccessor) factory).getEnchantmentProviderKey())
    ).apply(instance, VanillaTradeOfferFactories::processItemFactory));

    public static final MapCodec<TradeOffers.SellEnchantedToolFactory> SELL_ENCHANTED_TOOL = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(factory -> ((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) factory).getTool().getItem()),
            Codec.INT.optionalFieldOf("base_price", 1).forGetter(factory -> ((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) factory).getBasePrice()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) factory).getExperience()),
            Codec.FLOAT.optionalFieldOf("price_multiplier", 0.05F).forGetter(factory -> ((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) factory).getMultiplier())
    ).apply(instance, TradeOffers.SellEnchantedToolFactory::new));

    public static final MapCodec<TradeOffers.TypeAwareBuyForOneEmeraldFactory> TYPE_AWARE_BUY_FOR_ONE_EMERALD = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("count", 1).forGetter(factory -> ((TradeOffersAccessor.TypeAwareBuyForOneEmeraldFactoryAccessor) factory).getCount()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.TypeAwareBuyForOneEmeraldFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.TypeAwareBuyForOneEmeraldFactoryAccessor) factory).getExperience()),
            CodecHelper.villagerTypeMap(Registries.ITEM.getCodec()).fieldOf("items").forGetter(factory -> ((TradeOffersAccessor.TypeAwareBuyForOneEmeraldFactoryAccessor) factory).getMap())
    ).apply(instance, TradeOffers.TypeAwareBuyForOneEmeraldFactory::new));

    public static final MapCodec<TradeOffers.SellPotionHoldingItemFactory> SELL_POTION_HOLDING_ITEM = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Registries.ITEM.getCodec().optionalFieldOf("arrow", Items.ARROW).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSecondBuy()),
            Codec.INT.optionalFieldOf("second_count", 1).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSecondCount()),
            Registries.ITEM.getCodec().optionalFieldOf("tipped_arrow", Items.TIPPED_ARROW).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSell().getItem()),
            Codec.INT.fieldOf("price").forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getPrice()),
            Codec.INT.optionalFieldOf("sell_count", 1).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSellCount()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getExperience())
    ).apply(instance, TradeOffers.SellPotionHoldingItemFactory::new));

    public static final MapCodec<TradeOffers.EnchantBookFactory> ENCHANT_BOOK = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.EnchantBookFactoryAccessor) factory).getExperience()),
        Codec.INT.optionalFieldOf("min_level", 0).forGetter(factory -> ((TradeOffersAccessor.EnchantBookFactoryAccessor) factory).getMinLevel()),
        Codec.INT.optionalFieldOf("max_level", Integer.MAX_VALUE).forGetter(factory -> ((TradeOffersAccessor.EnchantBookFactoryAccessor) factory).getMaxLevel()),
        Identifier.CODEC.fieldOf("possible_enchantments").xmap(identifier -> TagKey.of(RegistryKeys.ENCHANTMENT, identifier), TagKey::id).forGetter(factory -> ((TradeOffersAccessor.EnchantBookFactoryAccessor) factory).getPossibleEnchantments())
    ).apply(instance, TradeOffers.EnchantBookFactory::new));

    public static final MapCodec<TradeOffers.SellMapFactory> SELL_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("price", 1).forGetter(factory -> ((TradeOffersAccessor.SellMapFactoryAccessor) factory).getPrice()),
            Identifier.CODEC.fieldOf("feature_tag").xmap(identifier -> TagKey.of(RegistryKeys.STRUCTURE, identifier), TagKey::id).forGetter(factory -> ((TradeOffersAccessor.SellMapFactoryAccessor) factory).getStructure()),
            Codec.STRING.fieldOf("name_key").forGetter(factory -> ((TradeOffersAccessor.SellMapFactoryAccessor) factory).getNameKey()),
            MapDecorationType.CODEC.fieldOf("decoration").forGetter(factory -> ((TradeOffersAccessor.SellMapFactoryAccessor) factory).getDecoration()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.SellMapFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellMapFactoryAccessor) factory).getExperience())
    ).apply(instance, TradeOffers.SellMapFactory::new));

    public static final MapCodec<TradeOffers.SellDyedArmorFactory> SELL_DYED_ARMOR = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Registries.ITEM.getCodec().fieldOf("item").forGetter(factory -> ((TradeOffersAccessor.SellDyedArmorFactoryAccessor) factory).getSell()),
            Codec.INT.optionalFieldOf("price", 1).forGetter(factory -> ((TradeOffersAccessor.SellDyedArmorFactoryAccessor) factory).getPrice()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.SellDyedArmorFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellDyedArmorFactoryAccessor) factory).getExperience())
    ).apply(instance, TradeOffers.SellDyedArmorFactory::new));

    public static Supplier<TradeOfferFactoryType<?>> getVanillaFactoryCodec(TradeOffers.Factory factory) {
        if (factory instanceof TradeOffers.BuyItemFactory) {
            return TradeOfferFactoryType.BUY_FOR_ONE_EMERALD::get;
        }
        if (factory instanceof TradeOffers.SellItemFactory) {
            return TradeOfferFactoryType.SELL_ITEM::get;
        }
        if (factory instanceof TradeOffers.SellSuspiciousStewFactory) {
            return TradeOfferFactoryType.SELL_SUSPICIOUS_STEW::get;
        }
        if (factory instanceof TradeOffers.ProcessItemFactory) {
            return TradeOfferFactoryType.PROCESS_ITEM::get;
        }
        if (factory instanceof TradeOffers.SellEnchantedToolFactory) {
            return TradeOfferFactoryType.SELL_ENCHANTED_TOOL::get;
        }
        if (factory instanceof TradeOffers.TypeAwareBuyForOneEmeraldFactory) {
            return TradeOfferFactoryType.TYPE_AWARE_BUY_FOR_ONE_EMERALD::get;
        }
        if (factory instanceof TradeOffers.SellPotionHoldingItemFactory) {
            return TradeOfferFactoryType.SELL_POTION_HOLDING_ITEM::get;
        }
        if (factory instanceof TradeOffers.EnchantBookFactory) {
            return TradeOfferFactoryType.ENCHANT_BOOK::get;
        }
        if (factory instanceof TradeOffers.SellMapFactory) {
            return TradeOfferFactoryType.SELL_MAP::get;
        }
        if (factory instanceof TradeOffers.SellDyedArmorFactory) {
            return TradeOfferFactoryType.SELL_DYED_ARMOR::get;
        }

        throw new IllegalStateException("Could not find codec for factory " + factory.getClass());
    }
}
