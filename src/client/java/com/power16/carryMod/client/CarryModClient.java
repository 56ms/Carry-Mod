package com.power16.carryMod.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class CarryModClient implements ClientModInitializer {

    private static final List<String> COUNT_SUGGESTIONS = List.of("1", "3", "5", "10");

    /** label = what shows in chat, single = price for one, bulk = per-run price once count >= bulkMin */
    private record Price(String label, long single, long bulk, int bulkMin) {
        long total(int count) {
            return (count >= bulkMin ? bulk : single) * count;
        }
    }

    private static Price p(String label, long single, long bulk, int bulkMin) {
        return new Price(label, single, bulk, bulkMin);
    }

    private static Price flat(String label, long price) {
        return new Price(label, price, price, 1);
    }

    // type -> (score -> price)
    private static final Map<String, Map<String, Price>> TYPES = new LinkedHashMap<>();

    static {
        // ---------- Dungeon (F4-F7) ----------
        Map<String, Price> dungeon = new LinkedHashMap<>();
        dungeon.put("f4_comp",  flat("F4 Completion", 600_000));
        dungeon.put("f4_s",     p("F4 S", 850_000, 700_000, 5));
        dungeon.put("f5_comp",  flat("F5 Completion", 600_000));
        dungeon.put("f5_s",     p("F5 S", 750_000, 650_000, 5));
        dungeon.put("f5_splus", p("F5 S+", 1_200_000, 1_000_000, 5));
        dungeon.put("f6_comp",  flat("F6 Completion", 700_000));
        dungeon.put("f6_s",     p("F6 S", 1_000_000, 850_000, 5));
        dungeon.put("f6_splus", p("F6 S+", 1_400_000, 1_200_000, 5));
        dungeon.put("f7_comp",  p("F7 Completion", 5_000_000, 4_000_000, 5));
        dungeon.put("f7_s",     p("F7 S", 9_500_000, 8_250_000, 5));
        dungeon.put("f7_splus", p("F7 S+", 12_000_000, 10_500_000, 5));
        TYPES.put("dungeon", dungeon);

        // ---------- Master Mode ----------
        Map<String, Price> mm = new LinkedHashMap<>();
        mm.put("m1", p("M1 S", 1_250_000, 1_000_000, 5));
        mm.put("m2", p("M2 S", 2_500_000, 2_200_000, 5));
        mm.put("m3", p("M3 S", 4_000_000, 3_600_000, 5));
        mm.put("m4", p("M4 S", 13_000_000, 11_000_000, 5));
        mm.put("m5", p("M5 S", 5_500_000, 4_500_000, 5));
        mm.put("m6", p("M6 S", 8_000_000, 6_750_000, 5));
        mm.put("m7", p("M7 S+", 32_000_000, 30_000_000, 5));
        TYPES.put("mm", mm);

        // ---------- Slayer ----------
        Map<String, Price> slayer = new LinkedHashMap<>();
        slayer.put("rev5",            p("Rev T5", 200_000, 150_000, 5));
        slayer.put("void3",           p("Void T3", 700_000, 600_000, 10));
        slayer.put("void4_bruiser",   p("Void T4 Bruiser", 1_800_000, 1_600_000, 10));
        slayer.put("void4_sepulture", p("Void T4 Sepulture", 1_400_000, 1_200_000, 10));
        slayer.put("blaze2",          p("Blaze T2", 1_400_000, 1_100_000, 5));
        slayer.put("blaze3",          p("Blaze T3", 2_500_000, 2_200_000, 5));
        slayer.put("blaze4",          p("Blaze T4", 5_000_000, 4_200_000, 5));
        TYPES.put("slayer", slayer);

        // ---------- Kuudra ----------
        Map<String, Price> kuudra = new LinkedHashMap<>();
        kuudra.put("basic",    p("Kuudra Basic", 8_000_000, 6_000_000, 3));
        kuudra.put("hot",      p("Kuudra Hot", 11_000_000, 8_500_000, 3));
        kuudra.put("burning",  p("Kuudra Burning", 16_000_000, 13_000_000, 3));
        kuudra.put("fiery",    p("Kuudra Fiery", 20_000_000, 17_000_000, 3));
        kuudra.put("infernal", p("Kuudra Infernal", 40_000_000, 35_000_000, 3));
        TYPES.put("kuudra", kuudra);

        // ---------- Crimson ----------
        Map<String, Price> crimson = new LinkedHashMap<>();
        crimson.put("ashfang",  p("Ashfang", 4_500_000, 3_500_000, 5));
        crimson.put("magma",    p("Magma Boss", 2_000_000, 1_500_000, 5));
        crimson.put("miniboss", p("Miniboss", 2_500_000, 2_000_000, 5));
        TYPES.put("crimson", crimson);
    }

    @Override
    public void onInitializeClient() {
        CarryConfig.load();
        CounterFeature.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = literal("carry")
                    .requires(source -> CarryConfig.get().carryCommand);

            for (Map.Entry<String, Map<String, Price>> typeEntry : TYPES.entrySet()) {
                LiteralArgumentBuilder<FabricClientCommandSource> typeNode = literal(typeEntry.getKey());

                for (Map.Entry<String, Price> scoreEntry : typeEntry.getValue().entrySet()) {
                    Price price = scoreEntry.getValue();

                    typeNode.then(literal(scoreEntry.getKey())
                            // /carry <type> <score>  -> defaults to 1
                            .executes(ctx -> send(ctx.getSource(), 1, price))
                            // /carry <type> <score> <count>
                            .then(argument("count", IntegerArgumentType.integer(1))
                                    .suggests((ctx, builder) ->
                                            SharedSuggestionProvider.suggest(COUNT_SUGGESTIONS, builder))
                                    .executes(ctx -> {
                                        int count = IntegerArgumentType.getInteger(ctx, "count");
                                        return send(ctx.getSource(), count, price);
                                    })));
                }

                root.then(typeNode);
            }

            dispatcher.register(root);
        });
    }

    private static int send(FabricClientCommandSource source, int count, Price price) {
        String message = "The cost for " + count + " " + price.label()
                + " is " + formatCoins(price.total(count));

        source.getClient().getConnection().sendCommand("pc " + message);
        return 1;
    }

    /** 21000000 -> "21m", 1250000 -> "1.25m", 850000 -> "850k" */
    private static String formatCoins(long value) {
        if (value >= 1_000_000) {
            return new BigDecimal(value).divide(new BigDecimal(1_000_000))
                    .stripTrailingZeros().toPlainString() + "m";
        }
        if (value >= 1_000) {
            return new BigDecimal(value).divide(new BigDecimal(1_000))
                    .stripTrailingZeros().toPlainString() + "k";
        }
        return String.valueOf(value);
    }
}