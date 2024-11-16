package io.github.orlouge.dynamicvillagertrades.trade_offers;

import com.mojang.serialization.DataResult;
import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.TradeOfferManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExtendedTradeOffer extends TradeOffer {
    public final Map<String, Double> attributeIncrement;
    private final boolean writeCache;
    private static final String attribute_key = DynamicVillagerTradesMod.MOD_ID + "_attributes";
    private static final String write_cache_key = DynamicVillagerTradesMod.MOD_ID + "_write_cache";


    public ExtendedTradeOffer(TradedItem firstBuyItem, Optional<TradedItem> secondBuyItem, ItemStack sellItem, int uses, int maxUses, boolean rewardingPlayerExperience, int specialPrice, int demandBonus, float priceMultiplier, int merchantExperience, Map<String, Double> attributes, boolean writeCache) {
        //super(firstBuyItem, secondBuyItem, sellItem, uses, maxUses, merchantExperience, priceMultiplier, demandBonus);
        //this.rewardingPlayerExperience = rewardingPlayerExperience;
        //this.setSpecialPrice(specialPrice);
        super(firstBuyItem, secondBuyItem, sellItem, uses, maxUses, rewardingPlayerExperience, specialPrice, demandBonus, priceMultiplier, merchantExperience);
        this.attributeIncrement = attributes;
        this.writeCache = writeCache;
    }

    public ExtendedTradeOffer(TradeOffer offer, Map<String, Double> attributes, boolean writeCache) {
        this(offer.getFirstBuyItem(), offer.getSecondBuyItem(), offer.getSellItem().copy(), offer.getUses(), offer.getMaxUses(), offer.shouldRewardPlayerExperience(), offer.getSpecialPrice(), offer.getDemandBonus(), offer.getPriceMultiplier(), offer.getMerchantExperience(), attributes, writeCache);
    }

    public static final Codec<ExtendedTradeOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TradedItem.CODEC.fieldOf("buy").forGetter(TradeOffer::getFirstBuyItem),
        TradedItem.CODEC.lenientOptionalFieldOf("buyB").forGetter(TradeOffer::getSecondBuyItem),
        ItemStack.CODEC.fieldOf("sell").forGetter(TradeOffer::getSellItem),
        Codec.INT.lenientOptionalFieldOf("uses", 0).forGetter(TradeOffer::getUses),
        Codec.INT.lenientOptionalFieldOf("maxUses", 4).forGetter(TradeOffer::getMaxUses),
        Codec.BOOL.lenientOptionalFieldOf("rewardExp", true).forGetter(TradeOffer::shouldRewardPlayerExperience),
        Codec.INT.lenientOptionalFieldOf("specialPrice", 0).forGetter(TradeOffer::getSpecialPrice),
        Codec.INT.lenientOptionalFieldOf("demand", 0).forGetter(TradeOffer::getDemandBonus),
        Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", 0.0F).forGetter(TradeOffer::getPriceMultiplier),
        Codec.INT.lenientOptionalFieldOf("xp", 1).forGetter(TradeOffer::getMerchantExperience),
        Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf(attribute_key).xmap(o -> o.orElseGet(HashMap::new), Optional::of).forGetter(tradeOffer -> tradeOffer.attributeIncrement),
        Codec.BOOL.optionalFieldOf(write_cache_key, false).forGetter(tradeOffer -> tradeOffer.writeCache)
    ).apply(instance, ExtendedTradeOffer::new));

    public record Factory(TradeOffers.Factory offer, Integer level,
                          Map<String, Double> attributes,
                          Optional<Map<String, Double>> affinity,
                          Optional<String> key, Boolean cache) {
        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TradeOfferFactoryType.CODEC.fieldOf("offer").forGetter(Factory::offer),
                StringIdentifiable.createCodec(TradeOfferManager.MerchantLevel::values).flatComapMap(TradeOfferManager.MerchantLevel::getId, TradeOfferManager.MerchantLevel::fromId).fieldOf("level").forGetter(Factory::level),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("attributes").forGetter(Factory::attributes),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("affinity").forGetter(Factory::affinity),
                Codec.STRING.optionalFieldOf("key").forGetter(Factory::key),
                Codec.BOOL.optionalFieldOf("cache", false).forGetter(Factory::cache)
        ).apply(instance, Factory::new));
        public static final String cached_offer_key = "cached_offer";

        public ExtendedTradeOffer create(Entity entity, Random random, Optional<NbtCompound> data) {
            if (this.cache && this.key.isPresent() && data.map(d -> d.contains(cached_offer_key)).orElse(false)) {
                DataResult<ExtendedTradeOffer> result = ExtendedTradeOffer.CODEC.parse(entity.getRegistryManager().getOps(NbtOps.INSTANCE), data.get().getCompound(cached_offer_key));
                if (result.isSuccess()) return result.getOrThrow();
            }

            TradeOffer offer = this.offer.create(entity, random);
            return offer == null ? null : new ExtendedTradeOffer(offer, attributes, this.cache);
        }

        @Override
        public String toString() {
            return offer.toString();
        }
    }

    public Optional<NbtCompound> extraNbt(DynamicRegistryManager registryManager) {
        if (this.writeCache) {
            NbtCompound base = new NbtCompound();
            DataResult<NbtElement> encoded = CODEC.encodeStart(registryManager.getOps(NbtOps.INSTANCE), this);
            if (!encoded.isSuccess()) return Optional.empty();
            base.put(Factory.cached_offer_key, encoded.getOrThrow());
            return Optional.of(base);
        } else {
            return Optional.empty();
        }
    }
}
