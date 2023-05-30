package io.github.orlouge.dynamicvillagertrades.trade_offers;

import com.mojang.serialization.Codec;
import io.github.orlouge.dynamicvillagertrades.api.CodecHelper;
import io.github.orlouge.dynamicvillagertrades.api.SerializableTradeOfferFactory;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerType;

import java.util.Map;
import java.util.function.Supplier;

public record TypeAwareTradeOfferFactory(Map<VillagerType, TradeOffers.Factory> tradeOffers) implements SerializableTradeOfferFactory {
    public static final Codec<TypeAwareTradeOfferFactory> CODEC = CodecHelper.villagerTypeMap(TradeOfferFactoryType.CODEC).fieldOf("trades").xmap(TypeAwareTradeOfferFactory::new, TypeAwareTradeOfferFactory::tradeOffers).codec();

    @Override
    public Supplier<TradeOfferFactoryType<?>> getType() {
        return TradeOfferFactoryType.TYPE_AWARE::get;
    }

    @Nullable
    @Override
    public TradeOffer create(Entity entity, Random random) {
        if (entity instanceof VillagerDataContainer villager) {
            return this.tradeOffers.get(villager.getVillagerData().getType()).create(entity, random);
        }
        return null;
    }
}
