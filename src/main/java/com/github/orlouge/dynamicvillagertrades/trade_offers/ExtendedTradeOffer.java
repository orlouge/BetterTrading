package com.github.orlouge.dynamicvillagertrades.trade_offers;

import com.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import com.github.orlouge.dynamicvillagertrades.TradeOfferManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExtendedTradeOffer extends TradeOffer {
    public final Map<String, Double> attributeIncrement;
    private final boolean writeCache;
    private static final String attribute_key = DynamicVillagerTradesMod.MOD_ID + "_attributes";
    private static final String write_cache_key = DynamicVillagerTradesMod.MOD_ID + "_write_cache";

    public ExtendedTradeOffer(NbtCompound nbt) {
        this(nbt, nbt.contains(write_cache_key) && nbt.getBoolean(write_cache_key));
    }
    public ExtendedTradeOffer(NbtCompound nbt, boolean writeCache) {
        super(nbt);
        this.attributeIncrement = new HashMap<>();
        this.writeCache = writeCache;
        if (nbt.contains(attribute_key)) {
            NbtCompound nbta = nbt.getCompound(attribute_key);
            nbta.getKeys().forEach(attr -> {
                this.attributeIncrement.put(attr, nbta.getDouble(attr));
            });
        }
    }

    public ExtendedTradeOffer(TradeOffer offer, Map<String, Double> attributes, boolean writeCache) {
        super(offer.getOriginalFirstBuyItem(), offer.getSecondBuyItem(), offer.getSellItem(), offer.getUses(), offer.getMaxUses(), offer.getMerchantExperience(), offer.getPriceMultiplier(), offer.getDemandBonus());
        this.attributeIncrement = attributes;
        this.writeCache = writeCache;
    }

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
                return new ExtendedTradeOffer(data.get().getCompound(cached_offer_key), this.cache);
            } else {
                TradeOffer offer = this.offer.create(entity, random);
                return offer == null ? null : new ExtendedTradeOffer(offer, attributes, this.cache);
            }
        }

        @Override
        public String toString() {
            return offer.toString();
        }
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound base = super.toNbt();
        NbtCompound nbta = new NbtCompound();
        this.attributeIncrement.forEach(nbta::putDouble);
        base.put(attribute_key, nbta);
        base.putBoolean(write_cache_key, this.writeCache);
        return base;
    }

    public Optional<NbtCompound> extraNbt() {
        if (this.writeCache) {
            NbtCompound base = new NbtCompound();
            base.put(Factory.cached_offer_key, this.toNbt());
            return Optional.of(base);
        } else {
            return Optional.empty();
        }
    }
}
