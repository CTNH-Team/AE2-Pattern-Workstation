package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.*;

public enum FillMode implements IPWEnumOption<FillMode> {

    VALUE(
            FILL_VALUE,
            List.of(Component.translatable("gui.ae2pw.fillMode.value.tooltip"))
    ),

    PLACEHOLDER(
            FILL_PLACEHOLDER,
            List.of(Component.translatable("gui.ae2pw.fillMode.placeHolder.tooltip"))
    );

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.fillMode.title");

    @Getter
    private final PWIcon icon;

    @Getter
    private final List<Component> tooltip;

    FillMode(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }
}
