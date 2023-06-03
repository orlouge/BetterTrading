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

import io.github.orlouge.dynamicvillagertrades.trade_offers.generators.Generator;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TradeOfferManager extends JsonDataLoader {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Identifier WANDERING_TRADER_PROFESSION_ID = Registry.ENTITY_TYPE.getId(EntityType.WANDERING_TRADER);
    public static final Identifier ID = DynamicVillagerTradesMod.id("trade_offers");

    private Map<Identifier, Map<String, TradeGroup>> tradeGroups = Map.of();

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
                builderMap.computeIfAbsent(trades.profession, p -> generateTradeGroups(Registry.VILLAGER_PROFESSION.get(p)).orElse(new TreeMap<>()));
                if (trades.replace) {
                    builderMap.put(trades.profession, new TreeMap<>());
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
        Generator.resetAll();
    }

    public Optional<Map<String, TradeGroup>> getVillagerOffers(VillagerProfession profession) {
        return Optional.ofNullable(tradeGroups.get(Registry.VILLAGER_PROFESSION.getId(profession))).or(() -> generateTradeGroups(profession));
    }

    private Optional<Map<String, TradeGroup>> generateTradeGroups(VillagerProfession profession) {
        return Generator.generateAll(profession);
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
