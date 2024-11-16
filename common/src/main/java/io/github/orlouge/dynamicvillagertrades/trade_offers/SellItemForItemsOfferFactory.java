package io.github.orlouge.dynamicvillagertrades.trade_offers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.orlouge.dynamicvillagertrades.api.CodecHelper;
import io.github.orlouge.dynamicvillagertrades.api.SerializableTradeOfferFactory;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.Optional;
import java.util.function.Supplier;

public record SellItemForItemsOfferFactory(TradedItem buy1, Optional<TradedItem> buy2, ItemStack sell, int maxUses, int experience) implements SerializableTradeOfferFactory {
    public static final MapCodec<SellItemForItemsOfferFactory> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TradedItem.CODEC.fieldOf("buy_1").forGetter(factory -> factory.buy1),
            TradedItem.CODEC.optionalFieldOf("buy_2").forGetter(factory -> factory.buy2),
            CodecHelper.SIMPLE_ITEM_STACK_CODEC.fieldOf("sell").forGetter(factory -> factory.sell),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> factory.maxUses),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> factory.experience)
    ).apply(instance, SellItemForItemsOfferFactory::new));

    @Override
    public TradeOffer create(Entity entity, Random random) {
        return new TradeOffer(this.buy1, this.buy2, this.sell.copy(), this.maxUses, this.experience, 0.05F);
    }

    @Override
    public Supplier<TradeOfferFactoryType<?>> getType() {
        return TradeOfferFactoryType.SELL_ITEM_FOR_ITEMS::get;
    }
}
