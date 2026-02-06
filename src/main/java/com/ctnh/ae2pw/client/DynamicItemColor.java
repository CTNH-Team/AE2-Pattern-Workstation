package com.ctnh.ae2pw.client;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class DynamicItemColor extends StaticItemColor {

    public static final boolean IS_APRIL_FOOLS = java.time.LocalDate.now().getMonthValue() == 4 && java.time.LocalDate.now().getDayOfMonth() == 1;

    public DynamicItemColor(AEColor color) {
        super(color);
    }

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if(IS_APRIL_FOOLS){
            var values = AEColor.values();
            int time = (int)(Minecraft.getInstance().level.getGameTime() / 15) % values.length;
            int phase = stack.hashCode() % values.length;
            return values[(time + phase) % values.length].getVariantByTintIndex(tintIndex);
        }

        return super.getColor(stack, tintIndex);
    }
}
