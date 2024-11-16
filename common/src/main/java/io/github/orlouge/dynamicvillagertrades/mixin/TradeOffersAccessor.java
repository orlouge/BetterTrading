package io.github.orlouge.dynamicvillagertrades.mixin;

import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.TradedItem;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerType;

import java.util.Map;
import java.util.Optional;

@Mixin(TradeOffers.class)
public interface TradeOffersAccessor {

    @Mixin(TradeOffers.BuyItemFactory.class)
    interface BuyItemFactoryAccessor {

        @Accessor
        TradedItem getStack();

        @Accessor
        int getPrice();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();
    }

    @Mixin(TradeOffers.SellItemFactory.class)
    interface SellItemFactoryAccessor {

        @Accessor
        ItemStack getSell();

        @Accessor
        int getPrice();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();
    }

    @Mixin(TradeOffers.SellSuspiciousStewFactory.class)
    interface SellSuspiciousStewFactoryAccessor {

        @Accessor
        SuspiciousStewEffectsComponent getStewEffects();

        @Accessor
        int getExperience();

        @Accessor
        float getMultiplier();
    }

    @Mixin(TradeOffers.ProcessItemFactory.class)
    interface ProcessItemFactoryAccessor {

        @Accessor
        TradedItem getToBeProcessed();

        @Accessor
        int getPrice();

        @Accessor
        ItemStack getProcessed();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();

        @Accessor
        Optional<RegistryKey<EnchantmentProvider>> getEnchantmentProviderKey();

        @Accessor
        float getMultiplier();
    }

    @Mixin(TradeOffers.SellEnchantedToolFactory.class)
    interface SellEnchantedToolFactoryAccessor {

        @Accessor
        ItemStack getTool();

        @Accessor
        int getBasePrice();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();

        @Accessor
        float getMultiplier();
    }

    @Mixin(TradeOffers.TypeAwareBuyForOneEmeraldFactory.class)
    interface TypeAwareBuyForOneEmeraldFactoryAccessor {

        @Accessor
        Map<VillagerType, Item> getMap();

        @Accessor
        int getCount();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();
    }

    @Mixin(TradeOffers.SellPotionHoldingItemFactory.class)
    interface SellPotionHoldingItemFactoryAccessor {

        @Accessor
        ItemStack getSell();

        @Accessor
        int getSellCount();

        @Accessor
        int getPrice();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();

        @Accessor
        Item getSecondBuy();

        @Accessor
        int getSecondCount();
    }

    @Mixin(TradeOffers.EnchantBookFactory.class)
    interface EnchantBookFactoryAccessor {

        @Accessor
        int getExperience();

        @Accessor
        int getMinLevel();

        @Accessor
        int getMaxLevel();

        @Accessor
        TagKey<Enchantment> getPossibleEnchantments();
    }

    @Mixin(TradeOffers.SellMapFactory.class)
    interface SellMapFactoryAccessor {

        @Accessor
        int getPrice();

        @Accessor
        TagKey<Structure> getStructure();

        @Accessor
        String getNameKey();

        @Accessor
        RegistryEntry<MapDecorationType> getDecoration();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();
    }

    @Mixin(TradeOffers.SellDyedArmorFactory.class)
    interface SellDyedArmorFactoryAccessor {

        @Accessor
        Item getSell();

        @Accessor
        int getPrice();

        @Accessor
        int getMaxUses();

        @Accessor
        int getExperience();
    }
}
