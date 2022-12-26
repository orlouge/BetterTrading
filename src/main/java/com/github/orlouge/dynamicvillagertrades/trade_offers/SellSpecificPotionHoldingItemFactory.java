package com.github.orlouge.dynamicvillagertrades.trade_offers;

import com.github.orlouge.dynamicvillagertrades.api.SerializableTradeOfferFactory;
import com.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;

import java.util.Optional;

public class SellSpecificPotionHoldingItemFactory implements SerializableTradeOfferFactory {
    private final ItemStack sell;
    private final int sellCount;
    private final int price;
    private final int maxUses;
    private final int experience;
    private final Item secondBuy;
    private final int secondCount;
    private final float priceMultiplier;
    private final Identifier potion;

    public static final Codec<SellSpecificPotionHoldingItemFactory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Registry.ITEM.getCodec().optionalFieldOf("arrow", Items.ARROW).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSecondBuy()),
            Codec.INT.optionalFieldOf("second_count", 1).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSecondCount()),
            Registry.ITEM.getCodec().optionalFieldOf("tipped_arrow", Items.TIPPED_ARROW).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSell().getItem()),
            Codec.INT.fieldOf("price").forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getPrice()),
            Codec.INT.optionalFieldOf("sell_count", 1).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getSellCount()),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getMaxUses()),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) factory).getExperience()),
            Identifier.CODEC.fieldOf("potion").forGetter(SellSpecificPotionHoldingItemFactory::getPotion)
            ).apply(instance, SellSpecificPotionHoldingItemFactory::new));

    public SellSpecificPotionHoldingItemFactory(Item arrow, int secondCount, Item tippedArrow, int sellCount, int price, int maxUses, int experience, Identifier potion) {
        this.sell = new ItemStack(tippedArrow);
        this.price = price;
        this.maxUses = maxUses;
        this.experience = experience;
        this.secondBuy = arrow;
        this.secondCount = secondCount;
        this.sellCount = sellCount;
        this.potion = potion;
        this.priceMultiplier = 0.05F;
    }

    public Identifier getPotion() {
        return potion;
    }

    @Override
    public TradeOffer create(Entity entity, Random random) {
        Optional<Potion> optPotion = Registry.POTION.getOrEmpty(this.potion);
        if (optPotion.isEmpty()) {
            throw new IllegalStateException("Potion " + this.potion + " does not exist.");
        }
        ItemStack emerald = new ItemStack(Items.EMERALD, this.price);
        ItemStack sellItem = PotionUtil.setPotion(new ItemStack(this.sell.getItem(), this.sellCount), optPotion.get());
        return new TradeOffer(emerald, new ItemStack(this.secondBuy, this.secondCount), sellItem, this.maxUses, this.experience, this.priceMultiplier);
    }

    @Override
    public TradeOfferFactoryType<?> getType() {
        return TradeOfferFactoryType.SELL_SPECIFIC_POTION_HOLDING_ITEM;
    }
}
