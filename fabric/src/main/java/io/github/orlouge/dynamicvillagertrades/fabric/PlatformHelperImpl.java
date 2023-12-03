package io.github.orlouge.dynamicvillagertrades.fabric;

import com.mojang.serialization.Codec;
import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.PlatformHelper;
import io.github.orlouge.dynamicvillagertrades.TradeOfferManager;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.nio.file.Path;
import java.util.function.Supplier;

public class PlatformHelperImpl {
    public static TradeOfferManagerFabric TRADE_OFFER_MANAGER = new TradeOfferManagerFabric();
    private static final RegistryHelperFabric<TradeOfferFactoryType<?>> tradeOfferFactory = new RegistryHelperFabric<>(FabricRegistryBuilder.createSimple(getType(), DynamicVillagerTradesMod.id("trade_offer_factory")).buildAndRegister());

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static PlatformHelper.RegistryHelper<TradeOfferFactoryType<?>> getTradeOfferRegistry() {
        return tradeOfferFactory;
    }

    public static TradeOfferManager getTradeOfferManager() {
        return TRADE_OFFER_MANAGER;
    }

    private static Class<TradeOfferFactoryType<?>> getType() {
        return null;
    }

    private static class RegistryHelperFabric<T> implements PlatformHelper.RegistryHelper<T> {
        private final Registry<T> registry;

        private RegistryHelperFabric(Registry<T> registry) {
            this.registry = registry;
        }

        @Override
        public Supplier<Codec<T>> getCodec() {
            return this.registry::getCodec;
        }

        @Override
        public <V extends T> Supplier<V> register(Identifier id, Supplier<V> entry) {
            V x = Registry.register(this.registry, id, entry.get());
            return () -> x;
        }
    }

    public static class TradeOfferManagerFabric extends TradeOfferManager implements IdentifiableResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return ID;
        }
    }
}
