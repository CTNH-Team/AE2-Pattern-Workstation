/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package com.ctnh.ae2pw.client.components;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import com.google.common.primitives.Longs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Allows precisely setting the amount to use for a processing pattern slot.
 * <p/>

 */
public class SetProcessingPatternInfoScreen
        extends AESubScreen<PatternWorkStationMenu, PatternWorkStationScreen> {

    private final NumberEntryWidget amount;
    private final AETextField rename;

    private final GenericStack currentStack;

    private final Consumer<GenericStack> setter;

    public SetProcessingPatternInfoScreen(PatternWorkStationScreen parentScreen,
                                          GenericStack currentStack,
                                          Consumer<GenericStack> setter) {
        super(parentScreen, "/screens/terminals/set_stock_info.json");

        this.currentStack = currentStack;
        this.setter = setter;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);

        var icon = getMenu().getHost().getMainMenuIcon();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        var button = new TabButton(icon, icon.getHoverName(), btn -> {
            returnToParent();
        });
        widgets.add("back", button);

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        this.amount.setLongValue(currentStack.amount());
        this.amount.setMaxValue(getMaxAmount());
        this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.amount.setMinValue(0);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);

        rename = widgets.addTextField("rename");
        rename.setPlaceholder(Component.translatable("gui.ae2pw.rename"));
        rename.setValue(currentStack.what().getDisplayName().getString());

        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    protected void init() {
        super.init();

        // The screen JSON includes the toolbox, but we don't actually have a need for it here
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private void confirm() {
        this.amount.getLongValue().ifPresent(newAmount -> {
            newAmount = Longs.constrainToRange(newAmount, 0, getMaxAmount());
            AEKey renamed = currentStack.what();
            if(!rename.getValue().equals(currentStack.what().getDisplayName().getString())){
                if(renamed instanceof AEItemKey itemKey){
                    renamed = AEItemKey.of(
                            itemKey.toStack().setHoverName(Component.literal(rename.getValue()))
                    );
                }
            }


            if (newAmount <= 0) {
                setter.accept(null);
            } else {
                setter.accept(new GenericStack(renamed, newAmount));
            }
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        if(btn == 1 && rename.isMouseOver(xCoord, yCoord)){
            rename.setValue("");
        }
        return super.mouseClicked(xCoord, yCoord, btn);
    }
}
