package com.github.orlouge.bettertrading.trade_offers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.github.orlouge.bettertrading.api.CodecHelper;
import com.github.orlouge.bettertrading.api.SerializableTradeOfferFactory;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;

public record SellItemForItemsOfferFactory(ItemStack buy1, ItemStack buy2, ItemStack sell, int maxUses, int experience) implements SerializableTradeOfferFactory {
    public static final Codec<SellItemForItemsOfferFactory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecHelper.SIMPLE_ITEM_STACK_CODEC.fieldOf("buy_1").forGetter(factory -> factory.buy1),
            CodecHelper.SIMPLE_ITEM_STACK_CODEC.optionalFieldOf("buy_2", ItemStack.EMPTY).forGetter(factory -> factory.buy2),
            CodecHelper.SIMPLE_ITEM_STACK_CODEC.fieldOf("sell").forGetter(factory -> factory.sell),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> factory.maxUses),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> factory.experience)
    ).apply(instance, SellItemForItemsOfferFactory::new));

    @Override
    public TradeOffer create(Entity entity, Random random) {
        return new TradeOffer(this.buy1.copy(), this.buy2.copy(), this.sell.copy(), this.maxUses, this.experience, 0.05F);
    }

    @Override
    public TradeOfferFactoryType<?> getType() {
        return TradeOfferFactoryType.SELL_ITEM_FOR_ITEMS;
    }
}
