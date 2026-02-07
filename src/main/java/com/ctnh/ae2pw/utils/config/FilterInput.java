package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.*;

public enum FilterInput implements IPWEnumOption<FilterInput> {

    TRUE(
            FILTER_INPUT_TRUE,
            List.of(Component.translatable("gui.ae2pw.filterInput.true"))
    ),

    FALSE(
            FILTER_INPUT_FALSE,
            List.of(Component.translatable("gui.ae2pw.filterInput.false"))
    );

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.filterInput.title");

    @Getter
    private final PWIcon icon;
    @Getter
    private final List<Component> tooltip;

    FilterInput(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }

}

