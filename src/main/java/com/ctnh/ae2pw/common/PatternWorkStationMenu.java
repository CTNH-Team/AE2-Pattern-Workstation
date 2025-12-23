package com.ctnh.ae2pw.common;

import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.InventoryAction;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class PatternWorkStationMenu extends PatternEncodingTermMenu {
    public static final MenuType<PatternWorkStationMenu> TYPE = MenuTypeBuilder
            .create(PatternWorkStationMenu::new, IPatternTerminalMenuHost.class)
            .build("patternworkstation");

    public PatternWorkStationMenu(int id, Inventory ip, IPatternTerminalMenuHost host) {
        this(TYPE, id, ip, host, true);
    }

    public PatternWorkStationMenu(MenuType<?> menuType, int id, Inventory ip, IPatternTerminalMenuHost host, boolean bindInventory) {
        super(menuType, id, ip, host, bindInventory);
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        super.doAction(player, action, slot, id);
    }
}
