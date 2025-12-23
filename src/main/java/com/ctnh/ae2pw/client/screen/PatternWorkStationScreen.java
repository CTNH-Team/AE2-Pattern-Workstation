package com.ctnh.ae2pw.client.screen;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.ActionItems;
import appeng.api.stacks.GenericStack;

import appeng.client.gui.me.common.StackSizeRenderer;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import appeng.parts.encoding.EncodingMode;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import com.ctnh.ae2pw.client.items.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PatternWorkStationScreen extends MEStorageScreen<PatternWorkStationMenu> {

    private final Map<EncodingMode, EncodingModePanel> modePanels = new EnumMap<>(EncodingMode.class);
    private final Map<EncodingMode, TabButton> modeTabButtons = new EnumMap<>(EncodingMode.class);

    public PatternWorkStationScreen(PatternWorkStationMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        for (var mode : EncodingMode.values()) {
            var panel = switch (mode) {
                case CRAFTING -> new CraftingEncodingPanel(this, widgets);
                case PROCESSING -> new ProcessingEncodingPanel(this, widgets);
                case SMITHING_TABLE -> new SmithingTableEncodingPanel(this, widgets);
                case STONECUTTING -> new StonecuttingEncodingPanel(this, widgets);
            };
            var tabButton = new TabButton(
                    panel.getTabIconItem(),
                    panel.getTabTooltip(),
                    btn -> getMenu().setMode(mode));
            tabButton.setStyle(TabButton.Style.HORIZONTAL);

            var modeIndex = modeTabButtons.size();
            widgets.add("modePanel" + modeIndex, panel);
            widgets.add("modeTabButton" + modeIndex, tabButton);
            modeTabButtons.put(mode, tabButton);
            modePanels.put(mode, panel);
        }

        var encodeBtn = new ActionButton(ActionItems.ENCODE, act -> menu.encode());
        widgets.add("encodePattern", encodeBtn);

    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        for (var mode : EncodingMode.values()) {
            var selected = menu.getMode() == mode;
            modeTabButtons.get(mode).setSelected(selected);
            modePanels.get(mode).setVisible(selected);
        }
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        // handler for middle mouse button crafting in survival mode
        if (this.minecraft.options.keyPickItem.matchesMouse(btn)) {
            var slot = this.findSlot(xCoord, yCoord);
            if (menu.canModifyAmountForSlot(slot)) {
                var currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    var screen = new SetProcessingPatternAmountScreen(
                            this,
                            currentStack,
                            newStack -> NetworkHandler.instance().sendToServer(new InventoryActionPacket(
                                    InventoryAction.SET_FILTER, slot.index,
                                    GenericStack.wrapInItemStack(newStack))));
                    switchToScreen(screen);
                    return true;
                }
            }
        }

        return super.mouseClicked(xCoord, yCoord, btn);
    }

    /**
     * When in processing mode, show a hint in the tooltip that middle-click will open the amount entry dialog.
     */
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && menu.canModifyAmountForSlot(this.hoveredSlot)) {
            var itemTooltip = new ArrayList<>(getTooltipFromContainerItem(this.hoveredSlot.getItem()));
            var unwrapped = GenericStack.fromItemStack(this.hoveredSlot.getItem());
            if (unwrapped != null) {
                itemTooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.Amount, unwrapped));
            }
            itemTooltip.add(Tooltips.getSetAmountTooltip());
            drawTooltip(guiGraphics, x, y, itemTooltip);
        } else {
            super.renderTooltip(guiGraphics, x, y);
        }
    }

    @Override
    protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
        // Since the crafting matrix and output slot are not backed by a config inventory, the default behavior
        // does not work out of the box.
        if (menu.isProcessingPatternSlot(slot)) {
            // See if we should offer the left-/right-click differentiation for setting a different filter
            var emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
            if (emptyingAction != null) {
                return emptyingAction;
            }
        }

        return super.getEmptyingAction(slot, carried);
    }

    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot s) {
        super.renderSlot(guiGraphics, s);

        if (shouldShowCraftableIndicatorForSlot(s)) {
            var poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(0, 0, 100); // Items are rendered with offset of 100, offset text too.
            StackSizeRenderer.renderSizeLabel(guiGraphics, this.font, s.x - 11, s.y - 11, "+", false);
            poseStack.popPose();
        }
    }

    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack stack) {
        var lines = super.getTooltipFromContainerItem(stack);

        // Append an indication to the tooltip that the item is craftable
        if (hoveredSlot != null && shouldShowCraftableIndicatorForSlot(hoveredSlot)) {
            lines = new ArrayList<>(lines); // Ensures we're not modifying a cached copy
            lines.add(ButtonToolTips.Craftable.text().withStyle(ChatFormatting.DARK_GRAY));
        }

        return lines;
    }

    private boolean shouldShowCraftableIndicatorForSlot(Slot s) {
        // Mark inputs for patterns for which the grid already has a pattern
        var semantic = menu.getSlotSemantic(s);
        if (semantic == SlotSemantics.CRAFTING_GRID
                || semantic == SlotSemantics.PROCESSING_INPUTS
                || semantic == SlotSemantics.SMITHING_TABLE_ADDITION
                || semantic == SlotSemantics.SMITHING_TABLE_BASE
                || semantic == SlotSemantics.SMITHING_TABLE_TEMPLATE
                || semantic == SlotSemantics.STONECUTTING_INPUT) {
            var slotContent = GenericStack.fromItemStack(s.getItem());
            if (slotContent == null) {
                return false;
            }

            return repo.isCraftable(slotContent.what());
        }
        return false;
    }

    @Override
    public void onClose() {
        if (AEConfig.instance().isClearGridOnClose()) {
            this.getMenu().clear();
        }
        super.onClose();
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        Blitter.texture("guis/pattern.png")
                .src(0, 71, 195, 89 )
                .dest(offsetX + imageWidth - 195, offsetY)
                .blit(guiGraphics);
    }
}
