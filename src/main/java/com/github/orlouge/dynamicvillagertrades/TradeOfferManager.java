package com.github.orlouge.dynamicvillagertrades;

import com.github.orlouge.dynamicvillagertrades.trade_offers.ExtendedTradeOffer;
import com.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

import net.minecraft.entity.EntityType;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TradeOfferManager extends JsonDataLoader implements IdentifiableResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Identifier WANDERING_TRADER_PROFESSION_ID = Registry.ENTITY_TYPE.getId(EntityType.WANDERING_TRADER);
    private static final Identifier ID = DynamicVillagerTradesMod.id("trade_offers");

    public Map<Identifier, Map<String, TradeGroup>> tradeGroups = Map.of();

    public TradeOfferManager() {
        super(GSON, ID.getPath());
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        Map<Identifier, Map<String, TradeGroup>> builderMap = new HashMap<>();

        AtomicInteger loadedCount = new AtomicInteger();
        manager.findResources(ID.getPath(), id -> id.getPath().endsWith(".json")).forEach((id, res) -> {
            Identifier identifier = new Identifier(id.getNamespace(), id.getPath().substring(ID.getPath().length() + 1, id.getPath().length() - 5));
            JsonElement jsonElement = prepared.get(identifier);
            if (jsonElement != null) {
                DynamicVillagerTradesMod.LOGGER.info("Loading {}" + identifier.toString());
                VillagerTrades trades = VillagerTrades.CODEC.decode(JsonOps.INSTANCE, jsonElement).getOrThrow(false, s -> DynamicVillagerTradesMod.LOGGER.error("Failed to read file {}: {}", identifier.toString(), s)).getFirst();
                if (trades.replace) {
                    builderMap.put(trades.profession, new TreeMap<>());
                } else {
                    builderMap.putIfAbsent(trades.profession, new TreeMap<>());
                }

                /*
                BetterTradingMod.LOGGER.info("Profession: " + trades.profession);
                BetterTradingMod.LOGGER.info("Replace: " + trades.replace);
                trades.offers.forEach((name, group) -> {
                    BetterTradingMod.LOGGER.info("[" + name + "]");
                    BetterTradingMod.LOGGER.info("Replace: " + Boolean.toString(group.replace()));
                    BetterTradingMod.LOGGER.info("Min trades: " + Integer.toString(group.minTrades()));
                    BetterTradingMod.LOGGER.info("Max trades: " + Integer.toString(group.maxTrades()));
                    group.offers().forEach(offer -> {
                        BetterTradingMod.LOGGER.info("Class: " + offer.offer().getClass().toString());
                        BetterTradingMod.LOGGER.info("Level: " + Integer.toString(offer.level()));
                        offer.attributes().forEach((attr, n) -> BetterTradingMod.LOGGER.info(attr + ": " + Double.toString(n)));
                    });
                });
                */


                Map<String, TradeGroup> groups = builderMap.get(trades.profession);
                groups.keySet().forEach(k -> DynamicVillagerTradesMod.LOGGER.info("{} before: {}", identifier.toString(), k));
                trades.offers().forEach((name, group) -> {
                    groups.computeIfPresent(name, (_name, oldGroup) -> TradeGroup.merge(oldGroup, group));
                    groups.putIfAbsent(name, group);
                });
                groups.keySet().forEach(k -> DynamicVillagerTradesMod.LOGGER.info("{} after: {}", identifier.toString(), k));
                loadedCount.incrementAndGet();
            }
        });

        /*
        this.tradeGroups = builderMap.entrySet().stream().map(entry -> {
            Int2ObjectMap<TradeOffers.Factory[]> entries = entry.getValue().entrySet().stream().map(entry2 -> Pair.of(entry2.getKey(), entry2.getValue().toArray(TradeOffers.Factory[]::new))).collect(Int2ObjectOpenHashMap::new, (map, pair) -> map.put((int) pair.getFirst(), pair.getSecond()), Int2ObjectOpenHashMap::putAll);
            return Pair.of(entry.getKey(), entries);
        }).collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
         */
        this.tradeGroups = builderMap;

        DynamicVillagerTradesMod.LOGGER.info("Loaded {} trade offer files", loadedCount.get());
    }

    public Optional<Collection<TradeGroup>> getVillagerOffers(VillagerProfession profession) {
        return Optional.ofNullable(tradeGroups.get(Registry.VILLAGER_PROFESSION.getId(profession))).map(Map::values).or(() -> generateTradeGroups(profession));
    }

    private Optional<Collection<TradeGroup>> generateTradeGroups(VillagerProfession profession) {
        return Optional.ofNullable(TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession)).map(tradeMap ->
            tradeMap.keySet().stream().sorted().map(level -> {
                TradeOffers.Factory[] trades = tradeMap.get(level);
                List<ExtendedTradeOffer.Factory> offers = new ArrayList<>(trades.length);
                Map<String, Double> attributes = Map.of();
                for (int i = 0; i < trades.length; i++) {
                    if (trades.length > 2) {
                        attributes = Map.of("" + level + "_" + i, 1.0);
                    }
                    String key = level.toString() + "_" + i;
                    boolean cache = trades[i] instanceof TradeOffers.SellMapFactory;
                    offers.add(new ExtendedTradeOffer.Factory(
                            trades[i], level, attributes, Optional.empty(), Optional.of(key), cache
                    ));
                }
                int min_trades = Math.min(trades.length, 2);
                return new TradeGroup(false, min_trades, min_trades, 1.0, Optional.empty(), offers);
            }).collect(Collectors.toList())
        );
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public enum MerchantLevel implements StringIdentifiable {
        NOVICE("novice", 1),
        APPRENTICE("apprentice", 2),
        JOURNEYMAN("journeyman", 3),
        EXPERT("expert", 4),
        MASTER("master", 5),
        COMMON("common", 1),
        RARE("rare", 2);

        public final String name;
        public final int id;

        MerchantLevel(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static DataResult<MerchantLevel> fromId(int id) {
            for (MerchantLevel value : values()) {
                if (value.id == id) {
                    return DataResult.success(value);
                }
            }

            return DataResult.error("Invalid level index " + id + " provided.");
        }

        @Override
        public String asString() {
            return this.name;
        }
    }

    public record VillagerTrades(Identifier profession, boolean replace, Map<String, TradeGroup> offers) {
        public static final Codec<VillagerTrades> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("profession").forGetter(VillagerTrades::profession),
                Codec.BOOL.optionalFieldOf("replace", false).forGetter(VillagerTrades::replace),
                Codec.unboundedMap(Codec.STRING, TradeGroup.CODEC).fieldOf("offers").forGetter(VillagerTrades::offers)
        ).apply(instance, VillagerTrades::new));
    }

}
