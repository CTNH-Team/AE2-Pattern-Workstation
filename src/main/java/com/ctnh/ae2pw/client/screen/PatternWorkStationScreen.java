package com.ctnh.ae2pw.client.screen;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.ShowPatternProviders;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;

import appeng.client.gui.me.common.StackSizeRenderer;

import appeng.client.gui.me.patternaccess.PatternContainerRecord;
import appeng.client.gui.me.patternaccess.PatternSlot;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.*;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import appeng.parts.encoding.EncodingMode;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import com.ctnh.ae2pw.client.items.*;
import com.glodblock.github.extendedae.client.button.HighlightButton;
import com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal;
import com.google.common.collect.HashMultimap;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PatternWorkStationScreen extends MEStorageScreen<PatternWorkStationMenu> {

    //////////////////////
    ////Pattern Access////
    //////////////////////
    private static final int GUI_WIDTH = 380;
    private static final int MAGIC_NUMBER = 50;
    private static final int GUI_TOP_AND_BOTTOM_PADDING = 54;

    private static final int GUI_PADDING_X = 22;
    private static final int GUI_PADDING_Y = 6;

    private static final int GUI_HEADER_HEIGHT = 51;
    private static final int GUI_FOOTER_HEIGHT = 97;
    private static final int COLUMNS = 9;

    /**
     * Additional margin in pixel for a text row inside the scrolling box.
     */
    private static final int PATTERN_PROVIDER_NAME_MARGIN_X = 2;

    /**
     * The maximum length for the string of a text row in pixel.
     */
    private static final int TEXT_MAX_WIDTH = 155;

    /**
     * Height of a table-row in pixels.
     */
    private static final int ROW_HEIGHT = 18;

    /**
     * Size of a slot in both x and y dimensions in pixel, most likely always the same as ROW_HEIGHT.
     */
    private static final int SLOT_SIZE = ROW_HEIGHT;

    // Bounding boxes of key areas in the UI texture.
    // The upper part of the UI, anything above the scrollable area (incl. its top border)
    private static final Rect2i HEADER_BBOX = new Rect2i(0, 0, GUI_WIDTH, GUI_HEADER_HEIGHT);
    // Background for a text row in the scroll-box.
    // Spans across the whole texture including the right and left borders including the scrollbar.
    // Covers separate textures for the top, middle and bottoms rows for more customization.
    private static final Rect2i ROW_TEXT_TOP_BBOX = new Rect2i(0, 51, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_MIDDLE_BBOX = new Rect2i(0, 87, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_BOTTOM_BBOX = new Rect2i(0, 123, GUI_WIDTH, ROW_HEIGHT);
    // Background for a inventory row in the scroll-box.
    // Spans across the whole texture including the right and left borders including the scrollbar.
    // Covers separate textures for the top, middle and bottoms rows for more customization.
    private static final Rect2i ROW_INVENTORY_TOP_BBOX = new Rect2i(0, 69, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_MIDDLE_BBOX = new Rect2i(0, 105, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_BOTTOM_BBOX = new Rect2i(0, 141, GUI_WIDTH, ROW_HEIGHT);
    // This is the lower part of the UI, anything below the scrollable area (incl. its bottom border)
    private static final Rect2i FOOTER_BBOX = new Rect2i(0, 159, GUI_WIDTH, GUI_FOOTER_HEIGHT);

    private static final Comparator<PatternContainerGroup> GROUP_COMPARATOR = Comparator
            .comparing(group -> group.name().getString().toLowerCase(Locale.ROOT));

    private final HashMap<Long, PatternContainerRecord> byId = new HashMap<>();
    private final HashMap<Integer, HighlightButton> highlightBtns = new HashMap<>();
    private final HashMap<Long, PatternProviderInfo> infoMap = new HashMap<>();
    // Used to show multiple pattern providers with the same name under a single header
    private final HashMultimap<PatternContainerGroup, PatternContainerRecord> byGroup = HashMultimap.create();
    private final ArrayList<PatternContainerGroup> groups = new ArrayList<>();
    private final ArrayList<Row> rows = new ArrayList<>();

    private final Map<String, Set<Object>> cachedSearches = new WeakHashMap<>();
    private final Set<ItemStack> matchedStack = new ObjectOpenCustomHashSet<>(new Hash.Strategy<>() {
        @Override
        public int hashCode(ItemStack o) {
            return o.getItem().hashCode() ^ (o.hasTag() ? o.getTag().hashCode() : 0xFFFFFFFF);
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return a == b || (a != null && b != null && ItemStack.isSameItemSameTags(a, b));
        }
    });
    private final Set<PatternContainerRecord> matchedProvider = new HashSet<>();
    private final Scrollbar scrollbar;

    private final AETextField searchPatternField;

    private int visibleRows = 0;

    private final ServerSettingToggleButton<ShowPatternProviders> showPatternProviders;




    //////////////////////
    ////Pattern Encode////
    //////////////////////
    private final Map<EncodingMode, EncodingModePanel> modePanels = new EnumMap<>(EncodingMode.class);
    private final Map<EncodingMode, TabButton> modeTabButtons = new EnumMap<>(EncodingMode.class);

    public PatternWorkStationScreen(PatternWorkStationMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        scrollbar = widgets.addScrollBar("scrollbar2");

        showPatternProviders = new ServerSettingToggleButton<>(Settings.TERMINAL_SHOW_PATTERN_PROVIDERS,
                ShowPatternProviders.VISIBLE);
        //TODO add showPatternProviders to toolbar
        searchPatternField = widgets.addTextField("search_pattern");
        //TODO setResponder
        //searchPatternField.setResponder(str -> this.refreshList());
        searchPatternField.setPlaceholder(GuiText.SearchPlaceholder.text());


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
    public void init() {
        this.visibleRows = config.getTerminalStyle().getRows(
                (this.height - GUI_HEADER_HEIGHT - GUI_FOOTER_HEIGHT - GUI_TOP_AND_BOTTOM_PADDING + MAGIC_NUMBER) / ROW_HEIGHT);
        if (this.visibleRows < 2) {
            this.visibleRows = 2;
        }
        super.init();

        this.highlightBtns.forEach((k, v) -> {v.setVisibility(false); addRenderableWidget(v);});
        this.resetScrollbar();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        for (var mode : EncodingMode.values()) {
            var selected = menu.getMode() == mode;
            modeTabButtons.get(mode).setSelected(selected);
            modePanels.get(mode).setVisible(selected);
        }
        this.showPatternProviders.set(this.menu.getShownProviders());
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

        if (btn == 1 && this.searchPatternField.isMouseOver(xCoord, yCoord)) {
            this.searchPatternField.setValue("");
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
            if (hoveredSlot == null) {
                var hoveredLineIndex = getHoveredLineIndex(x, y);
                if (hoveredLineIndex != -1) {
                    var row = rows.get(hoveredLineIndex);
                    if (row instanceof GroupHeaderRow headerRow && !headerRow.group.tooltip().isEmpty()) {
                        guiGraphics.renderTooltip(font, headerRow.group.tooltip(), Optional.empty(), x, y);
                        return;
                    }
                }
            }
            super.renderTooltip(guiGraphics, x, y);
        }
    }

    private int getHoveredLineIndex(int x, int y) {
        x = x - leftPos - GUI_PADDING_X;
        y = y - topPos - GUI_HEADER_HEIGHT;
        if (x < 0 || y < 0) {
            return -1;
        }
        if (x >= SLOT_SIZE * COLUMNS || y >= visibleRows * ROW_HEIGHT) {
            return -1;
        }

        var rowIndex = scrollbar.getCurrentScroll() + y / ROW_HEIGHT;
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return -1;
        }
        return rowIndex;
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
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof PatternSlot) {
            InventoryAction action = null;

            switch (clickType) {
                case PICKUP: // pickup / set-down.
                    action = mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                            : InventoryAction.PICKUP_OR_SET_DOWN;
                    break;
                case QUICK_MOVE:
                    action = mouseButton == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
                    break;

                case CLONE: // creative dupe:
                    if (getPlayer().getAbilities().instabuild) {
                        action = InventoryAction.CREATIVE_DUPLICATE;
                    }

                    break;

                default:
                case THROW: // drop item:
            }

            if (action != null) {
                PatternSlot machineSlot = (PatternSlot) slot;
                final InventoryActionPacket p = new InventoryActionPacket(action, machineSlot.getSlotIndex(),
                        machineSlot.getMachineInv().getServerId());
                NetworkHandler.instance().sendToServer(p);
            }

            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        Blitter.texture("guis/pattern.png")
                .src(0, 71, 195, 89 )
                .dest(offsetX + imageWidth - 195, offsetY)
                .blit(guiGraphics);
    }


    private void resetScrollbar() {
        // Needs to take the border into account, so offset for 1 px on the top and bottom.
        scrollbar.setHeight(this.visibleRows * ROW_HEIGHT - 2);
        scrollbar.setRange(0, this.rows.size() - this.visibleRows, 2);
    }


    sealed interface Row {
    }

    /**
     * A row containing a header for a group.
     */
    record GroupHeaderRow(PatternContainerGroup group) implements Row {
    }

    /**
     * A row containing slots for a subset of a pattern container inventory.
     */
    record SlotsRow(PatternContainerRecord container, int offset, int slots) implements Row {
    }

    public record PatternProviderInfo(@Nullable BlockPos pos, @Nullable Direction face, @Nullable ResourceKey<Level> world) {

    }

}
