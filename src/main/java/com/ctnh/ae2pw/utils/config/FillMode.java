package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.NO;
import static com.ctnh.ae2pw.client.icon.PWIcon.YSE;

public enum FillMode implements IPWEnumOption<FillMode> {

    VALUE(
            YSE,
            List.of(Component.translatable("gui.ae2pw.fillModeValueTooltip"))
    ),

    PLACEHOLDER(
            NO,
            List.of(Component.translatable("gui.ae2pw.fillModePlaceHolder"))
    );

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.fillModeValueTitle");

    @Getter
    private final PWIcon icon;

    @Getter
    private final List<Component> tooltip;

    FillMode(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }
}
