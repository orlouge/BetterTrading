package io.github.orlouge.dynamicvillagertrades;


import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import net.minecraft.util.Identifier;

public class DynamicVillagerTradesMod {
    public static final String MOD_ID = "dynamicvillagertrades";

    public static void init() {
        // forces the class to load early
        TradeOfferFactoryType.init();
    }

    public static Identifier id(String string) {
        // backward compatibility because CF forced me to change the name of the mod
        return new Identifier("bettertrading", string);
    }

    public static final TradeOfferManager TRADE_OFFER_MANAGER = PlatformHelper.getTradeOfferManager();
}
