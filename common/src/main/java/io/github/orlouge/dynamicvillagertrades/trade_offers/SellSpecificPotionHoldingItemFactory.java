package io.github.orlouge.dynamicvillagertrades.trade_offers;

import com.mojang.serialization.MapCodec;
import io.github.orlouge.dynamicvillagertrades.api.SerializableTradeOfferFactory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.Optional;
import java.util.function.Supplier;

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

    public static final MapCodec<SellSpecificPotionHoldingItemFactory> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Registries.ITEM.getCodec().optionalFieldOf("arrow", Items.ARROW).forGetter(factory -> factory.secondBuy),
            Codec.INT.optionalFieldOf("second_count", 1).forGetter(factory -> factory.secondCount),
            Registries.ITEM.getCodec().optionalFieldOf("tipped_arrow", Items.TIPPED_ARROW).forGetter(factory -> factory.sell.getItem()),
            Codec.INT.fieldOf("price").forGetter(factory -> factory.price),
            Codec.INT.optionalFieldOf("sell_count", 1).forGetter(factory -> factory.sellCount),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(factory -> factory.maxUses),
            Codec.INT.optionalFieldOf("experience", 2).forGetter(factory -> factory.experience),
            Identifier.CODEC.fieldOf("potion").forGetter(SellSpecificPotionHoldingItemFactory::getPotion)
            ).apply(instance, SellSpecificPotionHoldingItemFactory::new));

    public SellSpecificPotionHoldingItemFactory(Item secondBuy, int secondCount, Item sellItem, int sellCount, int price, int maxUses, int experience, Identifier potion) {
        this.sell = new ItemStack(sellItem);
        this.price = price;
        this.maxUses = maxUses;
        this.experience = experience;
        this.secondBuy = secondBuy;
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
        Optional<RegistryEntry.Reference<Potion>> optPotion = Registries.POTION.getEntry(this.potion);
        if (optPotion.isEmpty()) {
            throw new IllegalStateException("Potion " + this.potion + " does not exist.");
        }
        TradedItem emerald = new TradedItem(Items.EMERALD, this.price);
        ItemStack sellItem = new ItemStack(this.sell.getItem(), this.sellCount);
        sellItem.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(optPotion.get()));
        return new TradeOffer(emerald, Optional.of(new TradedItem(this.secondBuy, this.secondCount)), sellItem, this.maxUses, this.experience, this.priceMultiplier);
    }

    @Override
    public Supplier<TradeOfferFactoryType<?>> getType() {
        return TradeOfferFactoryType.SELL_SPECIFIC_POTION_HOLDING_ITEM::get;
    }
}
