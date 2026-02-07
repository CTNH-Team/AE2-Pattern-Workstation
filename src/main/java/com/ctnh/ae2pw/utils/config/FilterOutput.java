package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.*;

public enum FilterOutput implements IPWEnumOption<FilterOutput> {

    TRUE(
            FILTER_OUTPUT_TRUE,
            List.of(Component.translatable("gui.ae2pw.filterOutput.true"))
    ),

    FALSE(
            FILTER_OUTPUT_FALSE,
            List.of(Component.translatable("gui.ae2pw.filterOutput.false"))
    );

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.filterOutput.title");

    @Getter
    private final PWIcon icon;

    @Getter
    private final List<Component> tooltip;

    FilterOutput(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }
}
