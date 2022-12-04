package com.github.orlouge.bettertrading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.orlouge.bettertrading.trade_offers.TradeOfferFactoryType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BetterTradingMod implements ModInitializer {
	public static final String MOD_ID = "bettertrading";
	public static final Logger LOGGER = LoggerFactory.getLogger("BetterTrading");
	public static final Registry<TradeOfferFactoryType<?>> TRADE_OFFER_FACTORY_REGISTRY = FabricRegistryBuilder.createSimple(getType(), id("trade_offer_factory")).buildAndRegister();
	public static final TradeOfferManager TRADE_OFFER_MANAGER = new TradeOfferManager();

	@Override
	public void onInitialize() {
		TradeOfferFactoryType.init();
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(TRADE_OFFER_MANAGER);
	}

	/**
	 * Create an identifier with the mod ID as the namespace.
	 * @param string the identifier's path
	 * @return the identifier
	 */
	public static Identifier id(String string) {
		return new Identifier(MOD_ID, string);
	}

	private static Class<TradeOfferFactoryType<?>> getType() {
		return null;
	}
}
