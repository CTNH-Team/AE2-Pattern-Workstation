package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.*;

public enum EnableCircuit implements IPWEnumOption<EnableCircuit>{
    TRUE(
            ENABLE_CIRCUIT,
            List.of(Component.translatable("gui.ae2pw.enableCircuit.true"))
    ),

    FALSE(
            DISABLE_CIRCUIT,
            List.of(Component.translatable("gui.ae2pw.enableCircuit.false"))
    );

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.enableCircuit.title");

    @Getter
    private final PWIcon icon;

    @Getter
    private final List<Component> tooltip;

    EnableCircuit(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }
}
