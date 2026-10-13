package com.power16.carryMod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class CounterFeature {

    /** Must match the "id" in your fabric.mod.json (used for the keybind category). */
    private static final String MOD_ID = "carry-mod";

    private static KeyMapping incKey;
    private static KeyMapping decKey;
    private static KeyMapping openGuiKey;

    // Commands run while the chat screen is open, so the GUI is opened on the next tick instead.
    private static boolean openGuiRequested = false;

    private CounterFeature() {}

    public static void init() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        incKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.carry-mod.counter_inc", InputConstants.Type.KEYSYM, InputConstants.KEY_UP, category));
        decKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.carry-mod.counter_dec", InputConstants.Type.KEYSYM, InputConstants.KEY_DOWN, category));
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.carry-mod.open_gui", InputConstants.Type.KEYSYM, -1, category)); // -1 = unbound

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Always drain the clicks, even when the feature is off, so presses don't pile up.
            while (incKey.consumeClick()) {
                if (CarryConfig.get().counterKeybinds) change(client, 1);
            }
            while (decKey.consumeClick()) {
                if (CarryConfig.get().counterKeybinds) change(client, -1);
            }
            while (openGuiKey.consumeClick()) {
                openGuiRequested = true;
            }

            if (openGuiRequested && client.screen == null) {
                openGuiRequested = false;
                client.setScreen(new CarryHelperScreen(null));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("carry-helper")
                        .then(literal("gui").executes(ctx -> {
                            openGuiRequested = true;
                            return 1;
                        }))
                        .then(literal("count")
                                .requires(source -> CarryConfig.get().counterCommands)
                                // /carry-helper count
                                .executes(ctx -> {
                                    tell(ctx.getSource().getClient());
                                    return 1;
                                })
                                // /carry-helper count set <value>
                                .then(literal("set")
                                        .then(argument("value", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    setCount(ctx.getSource().getClient(),
                                                            IntegerArgumentType.getInteger(ctx, "value"));
                                                    return 1;
                                                })))
                                // /carry-helper count inc [number]
                                .then(literal("inc")
                                        .executes(ctx -> {
                                            change(ctx.getSource().getClient(), 1);
                                            return 1;
                                        })
                                        .then(argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    change(ctx.getSource().getClient(),
                                                            IntegerArgumentType.getInteger(ctx, "amount"));
                                                    return 1;
                                                })))
                                // /carry-helper count dec [number]
                                .then(literal("dec")
                                        .executes(ctx -> {
                                            change(ctx.getSource().getClient(), -1);
                                            return 1;
                                        })
                                        .then(argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    change(ctx.getSource().getClient(),
                                                            -IntegerArgumentType.getInteger(ctx, "amount"));
                                                    return 1;
                                                }))))));
    }

    private static void change(Minecraft client, long delta) {
        setCount(client, (long) CarryConfig.get().count + delta);
    }

    private static void setCount(Minecraft client, long value) {
        CarryConfig config = CarryConfig.get();
        config.count = (int) Math.max(0, Math.min(Integer.MAX_VALUE, value)); // never below 0
        CarryConfig.save();

        if (config.counterMessages) {
            tell(client);
        }
    }

    private static void tell(Minecraft client) {
        if (client.player != null) {
            client.player.sendSystemMessage(
                    Component.literal("Carry count: " + CarryConfig.get().count));
        }
    }
}