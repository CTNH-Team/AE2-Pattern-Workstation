package com.ctnh.ae2pw.client.button;

import appeng.client.gui.AEBaseScreen;
import com.ctnh.ae2pw.utils.config.IPWEnumOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BiConsumer;

public class PWServerEnumToggleButton<T extends Enum<T> & IPWEnumOption<T>> extends PWEnumToggleButton<T>{
    BiConsumer<T, Boolean> onChange;

    public PWServerEnumToggleButton(Class<T> enumClass, T initial, BiConsumer<T, Boolean> onChange) {
        super(enumClass, initial, t -> onChange.accept(t, false));
        this.onChange = onChange;
    }

    @Override
    public void onPress() {
        boolean backwards = false;

        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AEBaseScreen<?> aeScreen) {
            backwards = aeScreen.isHandlingRightClick();
        }

        onChange.accept(getCurrent(), backwards);
    }
}
