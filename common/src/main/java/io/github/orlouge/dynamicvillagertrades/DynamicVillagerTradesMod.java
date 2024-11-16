package io.github.orlouge.dynamicvillagertrades;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.JsonOps;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeGroup;
import io.github.orlouge.dynamicvillagertrades.trade_offers.TradeOfferFactoryType;
import net.minecraft.SharedConstants;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DynamicVillagerTradesMod {
    public static final String MOD_ID = "dynamicvillagertrades";
    public static boolean NO_BOOK_DUPLICATES = true;
    public static boolean ENCHANT_REPAIR_COMPAT = PlatformHelper.isModLoaded("enchantrepair");
    public static double GLOBAL_RANDOMNESS = 1.0;
    public static int REFRESH_DELAY = 0;

    public static final String CONFIG_FNAME = PlatformHelper.getConfigDirectory() + "/" + MOD_ID + ".properties";

    public static void init() {
        // forces the class to load early
        TradeOfferFactoryType.init();

        Properties defaultProps = new Properties();
        defaultProps.setProperty("no_book_duplicates", Boolean.toString(NO_BOOK_DUPLICATES));
        defaultProps.setProperty("global_randomness", Double.toString(GLOBAL_RANDOMNESS));
        defaultProps.setProperty("refresh_delay", Integer.toString(REFRESH_DELAY));
        defaultProps.setProperty("enchant_repair_compat", Boolean.toString(ENCHANT_REPAIR_COMPAT));

        File f = new File(CONFIG_FNAME);
        if (f.isFile() && f.canRead()) {
            try (FileInputStream in = new FileInputStream(f)) {
                Properties props = new Properties(defaultProps);
                props.load(in);
                NO_BOOK_DUPLICATES = Boolean.parseBoolean(props.getProperty("no_book_duplicates"));
                GLOBAL_RANDOMNESS = Double.parseDouble(props.getProperty("global_randomness"));
                REFRESH_DELAY = Integer.parseInt(props.getProperty("refresh_delay"));
                ENCHANT_REPAIR_COMPAT = Boolean.parseBoolean(props.getProperty("enchant_repair_compat"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileOutputStream out = new FileOutputStream(CONFIG_FNAME)) {
                defaultProps.store(out, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static Identifier id(String string) {
        return Identifier.of(MOD_ID, string);
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> baseCommand = CommandManager.literal(MOD_ID).requires(source -> source.hasPermissionLevel(2));
        baseCommand.then(CommandManager.literal("export").executes(context -> {
            exportDatapack(
                    context.getSource().getRegistryManager(),
                    err -> context.getSource().sendFeedback(() -> Text.literal(err.getMessage()), false),
                    path -> context.getSource().sendFeedback(() -> Text.literal("Exported to " + path), false)
            );
            return 1;
        }));
        dispatcher.register(baseCommand);
    }

    public static void exportDatapack(DynamicRegistryManager registryManager, Consumer<Exception> onException, Consumer<String> onExport) {
        File zipFile = new File("dvt_generated_pack.zip");
        boolean exported = false;
        List<Exception> exceptions = new LinkedList<>();
        try {
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));
            int packVersion = SharedConstants.DATA_PACK_VERSION;
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            byte[] metadata_json = ("{\"pack\":{\"description\":\"Generated datapack\",\"pack_format\":" + packVersion + "}}").getBytes();
            zip.write(metadata_json, 0, metadata_json.length);
            zip.closeEntry();
            Registries.VILLAGER_PROFESSION.getEntrySet().forEach(professionEntry -> {
                Identifier professionId = professionEntry.getKey().getValue();
                VillagerProfession profession = professionEntry.getValue();
                Optional<Map<String, TradeGroup>> tradeGroups = TRADE_OFFER_MANAGER.getVillagerOffers(profession, registryManager);
                if (tradeGroups.isPresent()) {
                    try {
                        TradeOfferManager.VillagerTrades trades = new TradeOfferManager.VillagerTrades(professionId, true, tradeGroups.get());
                        byte[] json = TradeOfferManager.GSON.toJson(TradeOfferManager.VillagerTrades.CODEC.encodeStart(JsonOps.INSTANCE, trades).getOrThrow()).getBytes();
                        zip.putNextEntry(new ZipEntry("data/" + MOD_ID + "/" + TradeOfferManager.ID.getPath() + "/" + professionId.getPath() + ".json"));
                        zip.write(json, 0, json.length);
                        zip.closeEntry();
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            });
            zip.close();
            exported = true;
        } catch (Exception e) {
            exceptions.add(e);
        }
        for (Exception e : exceptions) onException.accept(e);
        if (exported) onExport.accept(zipFile.getAbsolutePath());
    }

    public static final TradeOfferManager TRADE_OFFER_MANAGER = PlatformHelper.getTradeOfferManager();
}
