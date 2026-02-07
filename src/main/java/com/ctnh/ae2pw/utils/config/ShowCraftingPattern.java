package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.*;

public enum ShowCraftingPattern implements IPWEnumOption<ShowCraftingPattern>{

    All(SHOW_ALL, List.of(Component.translatable("gui.ae2pw.showCraftingPattern.all"))),

    Crafting(SHOW_CRAFTING, List.of(Component.translatable("gui.ae2pw.showCraftingPattern.crafting"))),

    Processing(SHOW_PROCESSING, List.of(Component.translatable("gui.ae2pw.showCraftingPattern.processing")));

    public static final String KEY = "show_crafting_pattern";

    @Getter
    private final PWIcon icon;

    @Getter
    private final List<Component> tooltip;

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.showCraftingPattern.title");

    ShowCraftingPattern(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }
}
