package io.github.orlouge.dynamicvillagertrades;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.function.Supplier;


public class PlatformHelper {
    @ExpectPlatform
    public static RegistryHelper<TradeOfferFactoryType<?>> getTradeOfferRegistry() {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static TradeOfferManager getTradeOfferManager() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String id) {
        throw new AssertionError();
    }

    public interface RegistryHelper<T> {
        Supplier<Codec<T>> getCodec();

        <V extends T> Supplier<V> register(Identifier id, Supplier<V> entry);
    }
}
