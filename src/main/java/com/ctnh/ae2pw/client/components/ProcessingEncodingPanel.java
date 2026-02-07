package com.ctnh.ae2pw.client.components;

import appeng.api.config.ActionItems;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import com.ctnh.ae2pw.client.button.PWActionButton;
import com.ctnh.ae2pw.client.icon.PWIcon;
import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ProcessingEncodingPanel extends EncodingModePanel {
    private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(0, 70, 126, 68);

    private final ActionButton clearBtn;
    private final ActionButton cycleOutputBtn;
    private final PWActionButton x2Button;
    private final Scrollbar scrollbar;

    public ProcessingEncodingPanel(PatternWorkStationScreen screen, WidgetContainer widgets) {
        super(screen, widgets);

        // Add buttons for the processing mode
        clearBtn = new ActionButton(ActionItems.CLOSE, act -> menu.clear());
        clearBtn.setHalfSize(true);
        widgets.add("processingClearPattern", clearBtn);

        this.cycleOutputBtn = new ActionButton(
                ActionItems.CYCLE_PROCESSING_OUTPUT,
                act -> menu.cycleProcessingOutput());
        this.cycleOutputBtn.setHalfSize(true);
        widgets.add("processingCycleOutput", this.cycleOutputBtn);


        x2Button = new PWActionButton(PWIcon.WHITE_ARROW_DOWN,
                Component.translatable("2"),
                Component.translatable("2"),
                this::handleClick)
                .halfSize();

        widgets.add("modifyPattern", x2Button);

        this.scrollbar = widgets.addScrollBar("processingPatternModeScrollbar", Scrollbar.SMALL);
        // The scrollbar ranges from 0 to the number of rows not visible
        this.scrollbar.setRange(0, menu.getProcessingInputSlots().length / 3 - 3, 3);
        this.scrollbar.setCaptureMouseWheel(false);

    }

    void handleClick(){
        boolean right = true;

        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AEBaseScreen<?> aeScreen) {
            right = aeScreen.isHandlingRightClick();
        }

        int multiplier = AbstractContainerScreen.hasControlDown() ? 8 : 2;
        if(right) multiplier *= -1;

        menu.modifyPattern(multiplier);
    }

    @Override
    public void updateBeforeRender() {
        // Update the processing slot position/visibility
        screen.repositionSlots(SlotSemantics.PROCESSING_INPUTS);
        screen.repositionSlots(SlotSemantics.PROCESSING_OUTPUTS);

        for (int i = 0; i < menu.getProcessingInputSlots().length; i++) {
            var slot = menu.getProcessingInputSlots()[i];
            var effectiveRow = (i / 3) - scrollbar.getCurrentScroll();

            slot.setActive(effectiveRow >= 0 && effectiveRow < 3);
            slot.y -= scrollbar.getCurrentScroll() * 18;
        }
        for (int i = 0; i < menu.getProcessingOutputSlots().length; i++) {
            var slot = menu.getProcessingOutputSlots()[i];
            var effectiveRow = i - scrollbar.getCurrentScroll();

            slot.setActive(effectiveRow >= 0 && effectiveRow < 3);
            slot.y -= scrollbar.getCurrentScroll() * 18;
        }

        updateTooltipVisibility();
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + bounds.getWidth() - 195 + 9, bounds.getY() + 14).blit(guiGraphics);
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        return scrollbar.onMouseWheel(mousePos, delta);
    }

    private void updateTooltipVisibility() {
        widgets.setTooltipAreaEnabled("processing-primary-output", visible && scrollbar.getCurrentScroll() == 0);
        widgets.setTooltipAreaEnabled("processing-optional-output1", visible && scrollbar.getCurrentScroll() > 0);
        widgets.setTooltipAreaEnabled("processing-optional-output2", visible);
        widgets.setTooltipAreaEnabled("processing-optional-output3", visible);
    }

    @Override
    public ItemStack getTabIconItem() {
        return Items.FURNACE.getDefaultInstance();
    }

    @Override
    public Component getTabTooltip() {
        return GuiText.ProcessingPattern.text();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        scrollbar.setVisible(visible);
        clearBtn.setVisibility(visible);
        cycleOutputBtn.setVisibility(menu.canCycleProcessingOutputs());
        x2Button.setVisibility(visible);

        screen.setSlotsHidden(SlotSemantics.PROCESSING_INPUTS, !visible);
        screen.setSlotsHidden(SlotSemantics.PROCESSING_OUTPUTS, !visible);

        updateTooltipVisibility();
    }
}
