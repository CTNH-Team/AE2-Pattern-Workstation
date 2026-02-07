package com.ctnh.ae2pw.client.button;

import appeng.client.gui.AEBaseScreen;
import com.ctnh.ae2pw.client.icon.PWIcon;
import com.ctnh.ae2pw.utils.config.IPWEnumOption;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PWEnumToggleButton<T extends Enum<T> & IPWEnumOption<T>> extends PWActionButton {

    private final T[] values;
    protected final Consumer<T> onChange;

    @Getter
    private T current;

    public PWEnumToggleButton(
            Class<T> enumClass,
            T initial,
            Consumer<T> onChange
    ) {
        super(
                initial.getIcon(),
                initial.getDisplayName(),
                initial.getTooltip().get(0),
                () -> {}
        );

        this.values = enumClass.getEnumConstants();
        this.current = initial;
        this.onChange = onChange;

        refreshVisuals();
    }

    @Override
    public void onPress() {
        boolean backwards = false;

        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AEBaseScreen<?> aeScreen) {
            backwards = aeScreen.isHandlingRightClick();
        }

        rotate(backwards);
    }

    @Override
    public PWEnumToggleButton<T> halfSize() {
        return (PWEnumToggleButton<T>)super.halfSize();
    }

    private void rotate(boolean backwards) {
        int idx = indexOf(current);
        int next = backwards ? idx - 1 : idx + 1;

        if (next < 0) {
            next = values.length - 1;
        } else if (next >= values.length) {
            next = 0;
        }

        setCurrent(values[next]);

    }

    private int indexOf(T value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private void refreshVisuals() {
        PWIcon icon = current.getIcon();
        if (icon != null) {
            setIcon(icon);
        }
        setMessage(buildMessage(current.getDisplayName(), current.getTooltip().get(0)));
    }

    public void setCurrent(T value) {
        if (this.current != value) {
            this.current = value;
            refreshVisuals();
            if (onChange != null) {
                onChange.accept(current);
            }
        }
    }
}
