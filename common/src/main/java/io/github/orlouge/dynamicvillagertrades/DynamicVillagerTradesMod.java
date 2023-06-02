package io.github.orlouge.dynamicvillagertrades;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.JsonOps;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import net.minecraft.SharedConstants;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> baseCommand = CommandManager.literal(MOD_ID).requires(source -> source.hasPermissionLevel(2));
        baseCommand.then(CommandManager.literal("export").executes(context -> {
            File zipFile = new File("dvt_generated_pack.zip");
            try {
                ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));
                int packVersion = ResourceType.SERVER_DATA.getPackVersion(SharedConstants.getGameVersion());
                zip.putNextEntry(new ZipEntry("pack.mcmeta"));
                byte[] metadata_json = ("{\"pack\":{\"description\":\"Generated datapack\",\"pack_format\":" + Integer.toString(packVersion) + "}}").getBytes();
                zip.write(metadata_json, 0, metadata_json.length);
                zip.closeEntry();
                Registry.VILLAGER_PROFESSION.getEntrySet().forEach(professionEntry -> {
                    Identifier professionId = professionEntry.getKey().getValue();
                    VillagerProfession profession = professionEntry.getValue();
                    Optional<Map<String, TradeGroup>> tradeGroups = TRADE_OFFER_MANAGER.getVillagerOffers(profession);
                    if (tradeGroups.isPresent()) {
                        try {
                            TradeOfferManager.VillagerTrades trades = new TradeOfferManager.VillagerTrades(professionId, true, tradeGroups.get());
                            byte[] json = TradeOfferManager.GSON.toJson(TradeOfferManager.VillagerTrades.CODEC.encodeStart(JsonOps.INSTANCE, trades).getOrThrow(false, s -> context.getSource().sendFeedback(Text.literal(s), false))).getBytes();
                            zip.putNextEntry(new ZipEntry("data/" + MOD_ID + "/" + TradeOfferManager.ID.getPath() + "/" + professionId.getPath() + ".json"));
                            zip.write(json, 0, json.length);
                            zip.closeEntry();
                        } catch (Exception e) {
                            context.getSource().sendFeedback(Text.literal(e.getMessage()), false);
                        }
                    }
                });
                zip.close();
            } catch (Exception e) {
                context.getSource().sendFeedback(Text.literal(e.getMessage()), false);
            }
            context.getSource().sendFeedback(Text.literal("Exported to " + zipFile.getAbsolutePath()), false);
            return 1;
        }));
        dispatcher.register(baseCommand);
    }

    public static final TradeOfferManager TRADE_OFFER_MANAGER = PlatformHelper.getTradeOfferManager();
}
