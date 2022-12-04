package com.github.orlouge.bettertrading.trade_offers;

import com.github.orlouge.bettertrading.BetterTradingMod;
import com.github.orlouge.bettertrading.TradeOfferManager;
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
    private static final String attribute_key = BetterTradingMod.MOD_ID + "_attributes";

    public ExtendedTradeOffer(NbtCompound nbt) {
        super(nbt);
        this.attributeIncrement = new HashMap<>();
        if (nbt.contains(attribute_key)) {
            NbtCompound nbta = nbt.getCompound(attribute_key);
            nbta.getKeys().forEach(attr -> { this.attributeIncrement.put(attr, nbta.getDouble(attr)); });
        }
    }

    public ExtendedTradeOffer(TradeOffer offer, Map<String, Double> attributes) {
        super(offer.getOriginalFirstBuyItem(), offer.getSecondBuyItem(), offer.getSellItem(), offer.getUses(), offer.getMaxUses(), offer.getMerchantExperience(), offer.getPriceMultiplier(), offer.getDemandBonus());
        this.attributeIncrement = attributes;
    }

    public record Factory(TradeOffers.Factory offer, Integer level,
                          Map<String, Double> attributes, Optional<Map<String, Double>> affinity) {
        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TradeOfferFactoryType.CODEC.fieldOf("offer").forGetter(Factory::offer),
                StringIdentifiable.createCodec(TradeOfferManager.MerchantLevel::values).flatComapMap(TradeOfferManager.MerchantLevel::getId, TradeOfferManager.MerchantLevel::fromId).fieldOf("level").forGetter(Factory::level),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("attributes").forGetter(Factory::attributes),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("affinity").forGetter(Factory::affinity)

        ).apply(instance, Factory::new));

        public ExtendedTradeOffer create(Entity entity, Random random) {
            TradeOffer offer = this.offer.create(entity, random);
            return offer == null ? null : new ExtendedTradeOffer(offer, attributes);
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
        return base;
    }
}
