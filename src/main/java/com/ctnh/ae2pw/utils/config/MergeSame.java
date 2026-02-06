package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.ctnh.ae2pw.client.icon.PWIcon.NO;
import static com.ctnh.ae2pw.client.icon.PWIcon.YSE;

public enum MergeSame implements IPWEnumOption<MergeSame> {

    TRUE(
            YSE,
            List.of(Component.translatable("gui.ae2pw.true"))
    ),

    FALSE(
            NO,
            List.of(Component.translatable("gui.ae2pw.false"))
    );

    @Getter
    private final Component displayName =
            Component.translatable("gui.ae2pw.mergeSame.title");

    @Getter
    private final PWIcon icon;

    @Getter
    private final List<Component> tooltip;

    MergeSame(PWIcon icon, List<Component> tooltip) {
        this.icon = icon;
        this.tooltip = tooltip;
    }
}
