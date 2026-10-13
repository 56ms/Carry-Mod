package com.power16.carryMod.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class CarryHelperScreen extends Screen {

    private final Screen parent;

    public CarryHelperScreen(Screen parent) {
        super(Component.literal("Carry Helper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CarryConfig cfg = CarryConfig.get();

        int w = 240;
        int gap = 24;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 55;

        addToggle(x, y, w, "Carry price command", () -> cfg.carryCommand, v -> cfg.carryCommand = v);
        addToggle(x, y + gap, w, "Counter commands", () -> cfg.counterCommands, v -> cfg.counterCommands = v);
        addToggle(x, y + gap * 2, w, "Counter keybinds", () -> cfg.counterKeybinds, v -> cfg.counterKeybinds = v);
        addToggle(x, y + gap * 3, w, "Counter chat messages", () -> cfg.counterMessages, v -> cfg.counterMessages = v);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(x, y + gap * 4 + 10, w, 20)
                .build());
    }

    private void addToggle(int x, int y, int w, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button button = Button.builder(label(name, getter.getAsBoolean()), b -> {
            boolean now = !getter.getAsBoolean();
            setter.accept(now);
            b.setMessage(label(name, now));
            CarryConfig.save();
        }).bounds(x, y, w, 20).build();

        this.addRenderableWidget(button);
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + ": ")
                .append(Component.literal(on ? "ON" : "OFF")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "Carry Helper";
        graphics.text(this.font, title, this.width / 2 - this.font.width(title) / 2,
                this.height / 2 - 55 - 30, 0xFFFFFFFF, true);

        String count = "Counter: " + CarryConfig.get().count;
        graphics.text(this.font, count, this.width / 2 - this.font.width(count) / 2,
                this.height / 2 - 55 - 16, 0xFFAAAAAA, true);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}