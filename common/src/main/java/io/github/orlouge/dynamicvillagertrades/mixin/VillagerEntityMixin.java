package io.github.orlouge.dynamicvillagertrades.mixin;

import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.ExtendedVillagerEntity;
import io.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity implements ExtendedVillagerEntity {
    @Shadow public abstract VillagerData getVillagerData();

    @Shadow public abstract void setAttacker(@Nullable LivingEntity attacker);

    private int dvtRestockCount = 0;

    private final Map<String, Double> attributes = new HashMap<>();
    private final Map<String, NbtCompound> extra_offer_data = new HashMap<>();
    private static final String attributes_key = DynamicVillagerTradesMod.MOD_ID + "_attributes";
    private static final String extra_offer_data_key = DynamicVillagerTradesMod.MOD_ID + "_extra_offer_data";

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    /*
    @ModifyConstant(method = "canRestock", constant = @Constant(longValue = 2400L))
    private long modifyRestockTime(long previous) {
        return previous / 120;
    }

    @ModifyConstant(method = "canRestock", constant = @Constant(intValue = 2))
    private int modifyRestockCount(int previous) {
        return previous * 30;
    }
    */

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void loadAttributes(NbtCompound nbt, CallbackInfo info) {
        readExtraData(nbt);
    }

    private void readAttributes(NbtCompound nbt) {
        nbt.getKeys().forEach(attr -> attributes.put(attr, nbt.getDouble(attr)));
    }

    private void readExtraOfferData(NbtCompound nbt) {
        this.extra_offer_data.clear();
        NbtCompound data_map = nbt.getCompound(extra_offer_data_key);
        data_map.getKeys().forEach(key -> {
            this.extra_offer_data.put(key, data_map.getCompound(key));
        });
    }

    @Override
    public void readExtraData(NbtCompound nbt) {
        if (nbt.contains(attributes_key)) {
            readAttributes(nbt.getCompound(attributes_key));

            if (nbt.contains("Offers", 10)) {
                loadExtendedOffers(nbt.getCompound("Offers"));
            }
        }

        if (nbt.contains(extra_offer_data_key)) {
            readExtraOfferData(nbt);
        }
    }

    @Override
    public void writeExtraData(NbtCompound nbt) {
        NbtCompound nbta = new NbtCompound();
        attributes.forEach(nbta::putDouble);
        nbt.put(attributes_key, nbta);

        NbtCompound nbteod = new NbtCompound();
        extra_offer_data.forEach(nbteod::put);
        nbt.put(extra_offer_data_key, nbteod);
    }

    private void loadExtendedOffers(NbtCompound offers) {
        NbtList recipes = offers.getList("Recipes", 10);
        TradeOfferList tradeOffers = new TradeOfferList();

        for (int i = 0; i < recipes.size(); ++i) {
            tradeOffers.add(ExtendedTradeOffer.CODEC.parse(this.getRegistryManager().getOps(NbtOps.INSTANCE), recipes.getCompound(i)).getOrThrow());
        }

        this.offers = tradeOffers;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void saveAttributes(NbtCompound nbt, CallbackInfo info) {
        this.writeExtraData(nbt);
    }

    @Inject(method = "fillRecipes", at = @At("HEAD"), cancellable = true)
    private void refreshOnLevelUp(CallbackInfo ci) {
        if (refreshOffers()) ci.cancel();
    }

    @Inject(method = "restock", at = @At("HEAD"))
    private void refreshOnRestock(CallbackInfo ci) {
        if (dvtRestockCount >= DynamicVillagerTradesMod.REFRESH_DELAY) {
            refreshOffers();
            dvtRestockCount = 0;
        } else {
            dvtRestockCount += 1;
        }
    }

    @Inject(method = "restockAndUpdateDemandBonus", at = @At("HEAD"))
    private void refreshOnRestockAndUpdateDemandBonus(CallbackInfo ci) {
        refreshOffers();
    }

    @Inject(method = "afterUsing", at = @At("HEAD"))
    private void updateAttributesAfterUse(TradeOffer offer, CallbackInfo ci) {
        if (offer instanceof ExtendedTradeOffer extOffer) {
            extOffer.attributeIncrement.forEach((attr, increment) -> {
                this.attributes.put(attr, increment + this.attributes.getOrDefault(attr, 0.0));
                // BetterTradingMod.LOGGER.info(attr + " = " + this.attributes.get(attr));
            });
        }
    }

    private boolean refreshOffers() {
        Optional<Collection<TradeGroup>> optoffers = DynamicVillagerTradesMod.TRADE_OFFER_MANAGER.getVillagerOffers(this.getVillagerData().getProfession(), this.getRegistryManager()).map(Map::values);
        optoffers.ifPresent(offerGroups -> {
            TradeOfferList offerList = this.getOffers();
            offerList.clear();

            int level = this.getVillagerData().getLevel();
            TradeGroup.TradeGroupSelector selector = new TradeGroup.TradeGroupSelector(offerGroups, level * 2, level * 2, level, this.attributes, this.random, new HashMap<>());

            HashMap<String, NbtCompound> new_data = new HashMap<>();
            selector.getSelectedTrades().forEach(tradeFactory -> {
                ExtendedTradeOffer generatedOffer = tradeFactory.create(
                        this, this.random, tradeFactory.key().flatMap(
                                key -> Optional.ofNullable(this.extra_offer_data.get(key))
                ));
                if (generatedOffer != null) {
                    offerList.add(generatedOffer);
                    tradeFactory.key().ifPresent(key -> generatedOffer.extraNbt(this.getRegistryManager()).ifPresent(
                            data -> new_data.put(key, data))
                    );
                }
            });
            this.extra_offer_data.clear();
            this.extra_offer_data.putAll(new_data);
        });

        return optoffers.isPresent();
    }
}
