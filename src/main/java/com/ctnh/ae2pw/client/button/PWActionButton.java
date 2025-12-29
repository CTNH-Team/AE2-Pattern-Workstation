package com.ctnh.ae2pw.client.button;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import com.ctnh.ae2pw.client.Icon;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class PWActionButton extends IconButton {
    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\\n", Pattern.LITERAL);

    @Setter
    private Icon icon;

    @Override
    protected final appeng.client.gui.Icon getIcon() {
        return null;
    }

    Blitter getBlitterIcon(){
        return icon.getBlitter();
    }

    public PWActionButton(Icon icon,
                          Component displayName,
                          Component displayValue,
                          Runnable onPress) {
        super(btn -> onPress.run());
        setIcon(icon);
        setMessage(buildMessage(displayName, displayValue));
    }


    private Component buildMessage(Component displayName, @Nullable Component displayValue) {
        String name = displayName.getString();
        if (displayValue == null) {
            return Component.literal(name);
        }
        String value = displayValue.getString();

        value = PATTERN_NEW_LINE.matcher(value).replaceAll("\n");
        final StringBuilder sb = new StringBuilder(value);

        int i = sb.lastIndexOf("\n");
        if (i <= 0) {
            i = 0;
        }
        while (i + 30 < sb.length() && (i = sb.lastIndexOf(" ", i + 30)) != -1) {
            sb.replace(i, i + 1, "\n");
        }

        return Component.literal(name + '\n' + sb);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (this.visible) {
            Blitter blitter = getBlitterIcon();
            if (!this.active) {
                blitter.opacity(0.5f);
            }

            if (this.isHalfSize()) {
                this.width = 8;
                this.height = 8;
            }

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();

            if (isFocused()) {
                guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), 0xFFFFFFFF);
                guiGraphics.fill(getX() - 1, getY(), getX(), getY() + height, 0xFFFFFFFF);
                guiGraphics.fill(getX() + width, getY(), getX() + width + 1, getY() + height, 0xFFFFFFFF);
                guiGraphics.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, 0xFFFFFFFF);
            }

            if (this.isHalfSize()) {
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(getX(), getY(), 0.0F);
                pose.scale(0.5f, 0.5f, 1.f);
                if (!isDisableBackground()) {
                    appeng.client.gui.Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(0, 0).blit(guiGraphics);
                }
                blitter.dest(0, 0).blit(guiGraphics);
                pose.popPose();
            } else {
                if (!isDisableBackground()) {
                    appeng.client.gui.Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).blit(guiGraphics);
                }
                blitter.dest(getX(), getY()).blit(guiGraphics);
            }
            RenderSystem.enableDepthTest();

            var item = this.getItemOverlay();
            if (item != null) {
                guiGraphics.renderItem(new ItemStack(item), getX(), getY());
            }
        }
    }

}
