package io.github.orlouge.dynamicvillagertrades;

import io.github.orlouge.dynamicvillagertrades.mixin.TradeOffersAccessor;
import io.github.orlouge.dynamicvillagertrades.trade_offers.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TradeOfferManager extends JsonDataLoader {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Identifier WANDERING_TRADER_PROFESSION_ID = Registry.ENTITY_TYPE.getId(EntityType.WANDERING_TRADER);
    public static final Identifier ID = DynamicVillagerTradesMod.id("trade_offers");

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
                VillagerTrades trades = VillagerTrades.CODEC.decode(JsonOps.INSTANCE, jsonElement).getOrThrow(false, s -> {
                }/* DynamicVillagerTradesMod.LOGGER.error("Failed to read file {}: {}", identifier.toString(), s) */).getFirst();
                if (trades.replace) {
                    builderMap.put(trades.profession, new TreeMap<>());
                } else {
                    builderMap.putIfAbsent(trades.profession, new TreeMap<>());
                }

                Map<String, TradeGroup> groups = builderMap.get(trades.profession);
                trades.offers().forEach((name, group) -> {
                    groups.computeIfPresent(name, (_name, oldGroup) -> TradeGroup.merge(oldGroup, group));
                    groups.putIfAbsent(name, group);
                });
                loadedCount.incrementAndGet();
            }
        });

        this.tradeGroups = builderMap;
    }

    public Optional<Map<String, TradeGroup>> getVillagerOffers(VillagerProfession profession) {
        return Optional.ofNullable(tradeGroups.get(Registry.VILLAGER_PROFESSION.getId(profession))).or(() -> generateTradeGroups(profession));
    }

    private Optional<Map<String, TradeGroup>> generateTradeGroups(VillagerProfession profession) {
        return Optional.ofNullable(TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession)).map(tradeMap ->
            tradeMap.keySet().stream().sorted().map(level -> {
                String levelName = MerchantLevel.fromId(level).result().map(Enum::name).orElse("" + level).toLowerCase();
                TradeOffers.Factory[] trades = tradeMap.get(level);
                List<ExtendedTradeOffer.Factory> offers = new ArrayList<>(trades.length);
                Map<String, Double> attributes = Map.of();
                for (int i = 0; i < trades.length; i++) {
                    if (trades.length > 2) {
                        attributes = Map.of(generateTradeAttributeName(trades[i], levelName + "_" + i), 1.0);
                    }
                    boolean cache = trades[i] instanceof TradeOffers.SellMapFactory;
                    Optional<String> key = cache ? Optional.of(levelName + "_" + generateTradeAttributeName(trades[i], "" + i)) : Optional.empty();
                    offers.add(new ExtendedTradeOffer.Factory(
                            trades[i], level, attributes, Optional.empty(), key, cache
                    ));
                }
                int min_trades = Math.min(trades.length, 2);
                String group_name = "" + level + "_" + levelName;
                TradeGroup group = new TradeGroup(false, min_trades, min_trades, 1.0, Optional.empty(), offers);
                return new Pair<>(group_name, group);
            }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, TradeGroup::merge, TreeMap::new))
        );
    }

    private String generateTradeAttributeName(TradeOffers.Factory factory, String fallback) {
        if (factory instanceof TradeOffers.BuyForOneEmeraldFactory buyfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.BuyForOneEmeraldFactoryAccessor) buyfactory).getBuy().getRegistryEntry().getKey();
            if (key.isPresent()) return key.get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.SellItemFactory sellfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.SellItemFactoryAccessor) sellfactory).getSell().getRegistryEntry().getKey();
            if (key.isPresent()) return key.get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.SellSuspiciousStewFactory stewfactory) {
            Optional<RegistryEntry<StatusEffect>> entry = Registry.STATUS_EFFECT.getEntry(StatusEffect.getRawId(((TradeOffersAccessor.SellSuspiciousStewFactoryAccessor) stewfactory).getEffect()));
            if (entry.isPresent() && entry.get().getKey().isPresent()) return entry.get().getKey().get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.ProcessItemFactory processfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.ProcessItemFactoryAccessor) processfactory).getSell().getRegistryEntry().getKey();
            if (key.isPresent()) return key.get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.SellEnchantedToolFactory sellfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.SellEnchantedToolFactoryAccessor) sellfactory).getTool().getItem().getRegistryEntry().getKey();
            if (key.isPresent()) return key.get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.TypeAwareBuyForOneEmeraldFactory buyfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.TypeAwareBuyForOneEmeraldFactoryAccessor) buyfactory).getMap().values().stream().findFirst().flatMap(i -> i.getRegistryEntry().getKey());
            if (key.isPresent()) return key.get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.SellPotionHoldingItemFactory sellfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.SellPotionHoldingItemFactoryAccessor) sellfactory).getSell().getItem().getRegistryEntry().getKey();
            if (key.isPresent()) return key.get().getValue().getPath();
        }
        if (factory instanceof TradeOffers.EnchantBookFactory) {
            return "enchanted_book";
        }
        if (factory instanceof TradeOffers.SellMapFactory sellfactory) {
            RegistryKey<? extends Registry<Structure>> key = ((TradeOffersAccessor.SellMapFactoryAccessor) sellfactory).getStructure().registry();
            return key.getValue().getPath();
        }
        if (factory instanceof TradeOffers.SellDyedArmorFactory sellfactory) {
            Optional<RegistryKey<Item>> key = ((TradeOffersAccessor.SellDyedArmorFactoryAccessor) sellfactory).getSell().getRegistryEntry().getKey();
            if (key.isPresent()) return key.get().getValue().getPath();

        }
        if (factory instanceof EnchantSpecificBookFactory enchantfactory) {
            return enchantfactory.getEnchantmentType().getPath();
        }
        if (factory instanceof SellSpecificPotionHoldingItemFactory sellpotionfactory) {
            return sellpotionfactory.getPotion().getPath();
        }
        return fallback;
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
