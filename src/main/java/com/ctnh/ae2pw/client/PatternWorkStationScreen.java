package com.ctnh.ae2pw.client;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.ScreenStyle;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PatternWorkStationScreen extends PatternEncodingTermScreen<PatternWorkStationMenu> {
    public PatternWorkStationScreen(PatternWorkStationMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

    }

    @Override
    public void init() {

        super.init();
        //leftPos += imageWidth /2;

//        for(var widget: renderables){
//            if(widget instanceof AbstractWidget aw){
//                aw.setX(aw.getX() + imageWidth /2 );
//            }
//        }

    }



    //    @Override
//    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
//        super.drawBG(guiGraphics, offsetX + imageWidth/2, offsetY, mouseX, mouseY, partialTicks);
//    }
//
//    @Override
//    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
//        super.drawFG(guiGraphics, offsetX + imageWidth/2, offsetY, mouseX, mouseY);
//    }
}
