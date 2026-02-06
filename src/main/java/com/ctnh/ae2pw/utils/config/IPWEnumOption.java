package com.ctnh.ae2pw.utils.config;

import com.ctnh.ae2pw.client.icon.PWIcon;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface IPWEnumOption<T extends Enum<T>> {

    PWIcon getIcon();

    List<Component> getTooltip();

    Component getDisplayName();
}

