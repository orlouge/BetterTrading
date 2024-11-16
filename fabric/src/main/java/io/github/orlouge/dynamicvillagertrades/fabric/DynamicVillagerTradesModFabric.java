package io.github.orlouge.dynamicvillagertrades.fabric;

import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.TradeOfferManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.resource.ResourceType;

public class DynamicVillagerTradesModFabric implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("DynamicVillagerTrades");

	@Override
	public void onInitialize() {
		DynamicVillagerTradesMod.init();
		/*
		DynamicRegistrySetupCallback.EVENT.register(view -> {
			if (view.asDynamicRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).isPresent()) {
				DynamicRegistryManager mgr = view.asDynamicRegistryManager();
				TradeOfferManager.REGISTRY_ACCESS = () -> mgr;
			}
		});

		 */
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(PlatformHelperImpl.TRADE_OFFER_MANAGER);
		CommandRegistrationCallback.EVENT.register((d, a, e) -> DynamicVillagerTradesMod.registerCommands(d));
	}

}
