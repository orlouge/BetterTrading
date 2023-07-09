package io.github.orlouge.dynamicvillagertrades.trade_offers;

import io.github.orlouge.dynamicvillagertrades.api.SerializableTradeOfferFactory;
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
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

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

    public static final Codec<SellSpecificPotionHoldingItemFactory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
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
        Optional<Potion> optPotion = Registries.POTION.getOrEmpty(this.potion);
        if (optPotion.isEmpty()) {
            throw new IllegalStateException("Potion " + this.potion + " does not exist.");
        }
        ItemStack emerald = new ItemStack(Items.EMERALD, this.price);
        ItemStack sellItem = PotionUtil.setPotion(new ItemStack(this.sell.getItem(), this.sellCount), optPotion.get());
        return new TradeOffer(emerald, new ItemStack(this.secondBuy, this.secondCount), sellItem, this.maxUses, this.experience, this.priceMultiplier);
    }

    @Override
    public Supplier<TradeOfferFactoryType<?>> getType() {
        return TradeOfferFactoryType.SELL_SPECIFIC_POTION_HOLDING_ITEM::get;
    }
}
