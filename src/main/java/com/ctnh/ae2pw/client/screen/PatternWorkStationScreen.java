package com.ctnh.ae2pw.client.screen;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.ShowPatternProviders;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.me.patternaccess.PatternContainerRecord;
import appeng.client.gui.me.patternaccess.PatternSlot;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.*;
import appeng.client.guidebook.document.LytRect;
import appeng.client.guidebook.render.SimpleRenderContext;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.ConfigButtonPacket;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.helpers.InventoryAction;
import appeng.menu.SlotSemantics;
import appeng.parts.encoding.EncodingMode;
import com.ctnh.ae2pw.client.button.PWEnumToggleButton;
import com.ctnh.ae2pw.client.icon.PWIcon;
import com.ctnh.ae2pw.client.button.PWActionButton;
import com.ctnh.ae2pw.client.components.*;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import com.ctnh.ae2pw.utils.*;
import com.ctnh.ae2pw.utils.config.*;
import com.glodblock.github.extendedae.client.button.HighlightButton;
import com.glodblock.github.extendedae.util.MessageUtil;
import com.google.common.collect.HashMultimap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import me.towdium.jecharacters.utils.Match;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PatternWorkStationScreen extends MEStorageScreen<PatternWorkStationMenu> {

    //////////////////////
    ////Pattern Access////
    //////////////////////
    private static final int GUI_WIDTH = 195;
    private static final int MAGIC_NUMBER = 50;
    private static final int GUI_TOP_AND_BOTTOM_PADDING = 54;

    private static final int GUI_PADDING_X = 13;
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
    private static final Rect2i ROW_TEXT_TOP_BBOX = new Rect2i(0, 17, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_MIDDLE_BBOX = new Rect2i(0, 53, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_BOTTOM_BBOX = new Rect2i(0, 89, GUI_WIDTH, ROW_HEIGHT);
    // Background for a inventory row in the scroll-box.
    // Spans across the whole texture including the right and left borders including the scrollbar.
    // Covers separate textures for the top, middle and bottoms rows for more customization.
    private static final Rect2i ROW_INVENTORY_TOP_BBOX = new Rect2i(0, 35, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_MIDDLE_BBOX = new Rect2i(0, 71, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_BOTTOM_BBOX = new Rect2i(0, 107, GUI_WIDTH, ROW_HEIGHT);
    // This is the lower part of the UI, anything below the scrollable area (incl. its bottom border)
    private static final Rect2i FOOTER_BBOX = new Rect2i(0, 159, GUI_WIDTH, GUI_FOOTER_HEIGHT);

    private static final Comparator<PatternContainerGroup> GROUP_COMPARATOR = Comparator
            .comparing(group -> group.name().getString().toLowerCase(Locale.ROOT));

    private final HashMap<Long, PatternContainerRecord> byId = new HashMap<>();
    private final HashMap<Integer, HighlightButton> highlightBtns = new HashMap<>();
    private final HashMap<Integer, PWActionButton> transferBtns = new HashMap<>();

    private final HashMap<Long, PatternProviderInfo> infoMap = new HashMap<>();
    // Used to show multiple pattern providers with the same name under a single header
    private final HashMultimap<PatternContainerGroup, PatternContainerRecord> byGroup = HashMultimap.create();
    private final ArrayList<PatternContainerGroup> groups = new ArrayList<>();
    private final ArrayList<Row> rows = new ArrayList<>();

    private final Map<String, Set<Object>> cachedSearches = new WeakHashMap<>();

    private final Map<String, Set<PatternContainerRecord>> patternNameCache = new HashMap<>();;
    private final Map<String, Set<PatternContainerRecord>> inputCache = new HashMap<>();;
    private final Map<String, Set<PatternContainerRecord>> outputCache = new HashMap<>();;


    private final Object2BooleanMap<ItemStack> matchedInputStack = new StackMap();

    private final Object2BooleanMap<ItemStack> matchedOutputStack = new StackMap();

    private final Scrollbar scrollbar;
    private final PatternBufferPanel patternBufferPanel;

    private final PWEnumToggleButton<FillMode> fillMode;
    private final PWEnumToggleButton<FilterInput> filterInput;
    private final PWEnumToggleButton<FilterOutput> filterOutput;
    public final PWEnumToggleButton<MergeSame> mergeSame;

    private final AETextField searchPatternField;

    private int visibleRows = 0;

    private final ServerSettingToggleButton<ShowPatternProviders> showPatternProviders;
    private final PWEnumToggleButton<ShowCraftingPattern> showCraftingPattern;

    private final VerticalButtonBar upLeftToolbar;


    //////////////////////
    ////Pattern Encode////
    //////////////////////
    private final Map<EncodingMode, EncodingModePanel> modePanels = new EnumMap<>(EncodingMode.class);
    private final Map<EncodingMode, TabButton> modeTabButtons = new EnumMap<>(EncodingMode.class);

    public PatternWorkStationScreen(PatternWorkStationMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("upLeftToolbar", upLeftToolbar = new VerticalButtonBar());
        scrollbar = widgets.addScrollBar("scrollbar2", Scrollbar.SMALL);

        showPatternProviders = new ServerSettingToggleButton<>(Settings.TERMINAL_SHOW_PATTERN_PROVIDERS,
                ShowPatternProviders.VISIBLE);
        upLeftToolbar.add(showPatternProviders);

        showCraftingPattern = new PWEnumToggleButton<>(ShowCraftingPattern.class, ShowCraftingPattern.All,
                c -> refreshList());
        upLeftToolbar.add(showCraftingPattern);

        fillMode = new PWEnumToggleButton<>(
                FillMode.class,
                FillMode.VALUE,
                a -> {}
        ).halfSize();

        widgets.add("fillMode", fillMode);

        searchPatternField = widgets.addTextField("searchPattern");
        searchPatternField.setResponder(str -> refreshList());

        searchPatternField.setPlaceholder(Component.empty());

        var copyButton = new PWActionButton(PWIcon.COPY,
                Component.translatable("gui.aw2pw.copy.title"),
                Component.translatable("gui.aw2pw.copy.tooltip"),
                () -> {
            if(searchPatternField.getValue().isEmpty()){
                Minecraft.getInstance().keyboardHandler.setClipboard(searchPatternField.getPlaceholder().getString());
            } else {
                Minecraft.getInstance().keyboardHandler.setClipboard(searchPatternField.getValue());
            }
        }
        ).halfSize();
        widgets.add("copySearch", copyButton);

        filterInput = new PWEnumToggleButton<>(
                FilterInput.class,
                FilterInput.FALSE,
                c -> refreshList()
        ).halfSize();
        widgets.add("filterInput", filterInput);

        filterOutput = new PWEnumToggleButton<>(
                FilterOutput.class,
                FilterOutput.FALSE,
                c -> refreshList()

        ).halfSize();
        widgets.add("filterOutput", filterOutput);

        mergeSame = new PWEnumToggleButton<>(
                MergeSame.class,
                MergeSame.TRUE,
                c -> menu.mergeSame = (c == MergeSame.TRUE)
        ).halfSize();
        widgets.add("mergeSame", mergeSame);

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

        var encodeBtn = new ActionButton(ActionItems.ENCODE, act -> encodePattern());
        widgets.add("encodePattern", encodeBtn);

        patternBufferPanel = new PatternBufferPanel(this, widgets);
        widgets.add("patternBuffer", patternBufferPanel);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double wheelDelta) {
        if(x >= leftPos && x <= leftPos + 185 && y < topPos + imageHeight - style.getTerminalStyle().getBottom().getSrcHeight()){
            return scrollbar.onMouseWheel(Point.ZERO, wheelDelta);
        }
        return super.mouseScrolled(x, y, wheelDelta);
    }

    @Override
    public void init() {
        super.init();
        int rowSpace = Math.min(this.height - GUI_HEADER_HEIGHT - GUI_FOOTER_HEIGHT - GUI_TOP_AND_BOTTOM_PADDING + MAGIC_NUMBER,
                baseYOffset);

        this.visibleRows = appeng.api.config.TerminalStyle.FULL
                .getRows(rowSpace / ROW_HEIGHT);
        if (this.visibleRows < 2) {
            this.visibleRows = 2;
        }


        this.highlightBtns.forEach((k, v) -> {v.setVisibility(false); addRenderableWidget(v);});
        this.transferBtns.forEach((k, v) -> {v.setVisibility(false); addRenderableWidget(v);});
        this.resetScrollbar();

        patternBufferPanel.init(imageHeight - 89 - style.getTerminalStyle().getBottom().getSrcHeight());
    }

    public void fillSearchPattern(String content){
        if(fillMode.getCurrent() == FillMode.VALUE)
            searchPatternField.setValue(content);
        else
            searchPatternField.setPlaceholder(Component.literal(content));
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        for (var mode : EncodingMode.values()) {
            var selected = menu.getMode() == mode;
            modeTabButtons.get(mode).setSelected(selected);
            modePanels.get(mode).setVisible(selected);
        }
        showPatternProviders.set(menu.getShownPatternProviders());
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        // handler for middle mouse button crafting in survival mode
        if (this.minecraft.options.keyPickItem.matchesMouse(btn)) {
            var slot = this.findSlot(xCoord, yCoord);
            if (menu.canModifyAmountForSlot(slot)) {
                var currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    var screen = new SetProcessingPatternInfoScreen(
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
        y = y - topPos - ROW_HEIGHT;
        if (x < 0 || y < 0) {
            return -1;
        }
        if (x >= SLOT_SIZE * COLUMNS / 2 || y >= visibleRows * ROW_HEIGHT) {
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

    public void encodePattern(){
        long id = 0L;
        if(hasShiftDown()){
            id = findContainer(ItemStack.EMPTY);
        }
        menu.encode(id);
    }

    public void quickMovePattern(){
        var serverId = findContainer(ItemStack.EMPTY);
        if(serverId != 0){
            final InventoryActionPacket p = new InventoryActionPacket(
                    InventoryAction.FILL_ITEM, //what?
                    hasShiftDown() ? -2 : -1,
                    serverId
            );
            NetworkHandler.instance().sendToServer(p);
        }
    }

    public long findContainer(ItemStack stack){
        for(var group: groups){
            var containers = new ArrayList<>(byGroup.get(group));
            Collections.sort(containers);
            for(var container : containers){
                if(Utils.quickInsert(container.getInventory(), stack, true)){

                    return container.getServerId();
                }
            }
        }
        return 0;
    }


    @Override
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if(slot instanceof PatternRecycleSlot && clickType== ClickType.PICKUP){
            final InventoryActionPacket p = new InventoryActionPacket(
                    InventoryAction.PICKUP_OR_SET_DOWN,
                    slotIdx,
                    0
            );
            NetworkHandler.instance().sendToServer(p);
            return;
        }

        if(slot instanceof PatternBufferSlot patternBufferSlot
                && !slot.getItem().isEmpty()
        ){
            if(mouseButton == 1){
                switch (clickType){
                    case PICKUP:{
                        var serverId = findContainer(slot.getItem());
                        if(serverId != 0){
                            final InventoryActionPacket p = new InventoryActionPacket(
                                    InventoryAction.FILL_ITEM, //what?
                                    slotIdx,
                                    serverId
                            );
                            NetworkHandler.instance().sendToServer(p);
                        }
                        break;
                    }
                    case QUICK_MOVE:{
                        final InventoryActionPacket p = new InventoryActionPacket(
                                InventoryAction.EMPTY_ITEM, //what?
                                slotIdx,
                                0
                        );
                        NetworkHandler.instance().sendToServer(p);
                        break;
                    }

                }
                return;
            } else if(mouseButton == 2 && clickType == ClickType.CLONE){
                final InventoryActionPacket p = new InventoryActionPacket(
                        InventoryAction.CREATIVE_DUPLICATE,
                        slotIdx,
                        0
                );
                NetworkHandler.instance().sendToServer(p);
                return;
            }


        }

        if (slot instanceof PatternSlot machineSlot) {
            InventoryAction action = null;

            switch (clickType) {
                case PICKUP: // pickup / set-down.
                    action = mouseButton == 1 ?
                            InventoryAction.SPLIT_OR_PLACE_SINGLE //quick retrieve
                            : InventoryAction.PICKUP_OR_SET_DOWN;
                    break;
                case QUICK_MOVE:
                    action = mouseButton == 1 ?
                            InventoryAction.PICKUP_SINGLE // delete
                            : InventoryAction.SHIFT_CLICK;
                    break;

                case CLONE: // creative dupe:
                    if (getPlayer().getAbilities().instabuild) {
                        action = InventoryAction.CREATIVE_DUPLICATE;
                    }

                    break;
            }

            if (action != null) {
                final InventoryActionPacket p = new InventoryActionPacket(action, machineSlot.getSlotIndex(),
                        machineSlot.getMachineInv().getServerId());
                NetworkHandler.instance().sendToServer(p);
            }

            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);

        this.menu.slots.removeIf(slot -> slot instanceof PatternSlot);
        this.highlightBtns.forEach((key, value) -> value.setVisibility(false));
        transferBtns.forEach((key, value) -> value.setVisibility(false));

        int textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();

        final int scrollLevel = scrollbar.getCurrentScroll();

        int i = 0;
        for (; i < this.visibleRows; ++i) {
            if (scrollLevel + i < this.rows.size()) {
                var row = this.rows.get(scrollLevel + i);
                if (highlightBtns.containsKey(scrollLevel + i)) {
                    var btn = highlightBtns.get(scrollLevel + i);
                    btn.setPosition(this.leftPos + GUI_PADDING_X - 10, this.topPos + (i + 1) * SLOT_SIZE + 4);
                    btn.setVisibility(true);
                }
                if(transferBtns.containsKey(scrollLevel + i)){
                    var btn = transferBtns.get(scrollLevel + i);
                    btn.setPosition(this.leftPos + 160, this.topPos + (i + 1) * SLOT_SIZE + 4);
                    btn.setVisibility(true);
                }
                if (row instanceof SlotsRow slotsRow) {
                    // Note: We have to shift everything after the header up by 1 to avoid black line duplication.
                    var container = slotsRow.container;
                    for (int col = 0; col < slotsRow.slots; col++) {
                        var slot = new PatternSlot(
                                container,
                                slotsRow.offset + col,
                                col * SLOT_SIZE + GUI_PADDING_X,
                                (i + 1) * SLOT_SIZE);
                        this.menu.slots.add(slot);
                        if (isFilterOutput() && !searchField.getValue().isEmpty() && itemStackMatchesSearchTerm(slot.getItem(), searchField.getValue(), matchedOutputStack, false)) {
                            fillRect(guiGraphics, new Rect2i(slot.x - 1 , slot.y - 1 , 18, 18), 0x8A00FF00);
                        } else
                        if(isFilterInput() && !searchField.getValue().isEmpty() && itemStackMatchesSearchTerm(slot.getItem(), searchField.getValue(), matchedInputStack, true)){
                            fillRect(guiGraphics, new Rect2i(slot.x - 1 , slot.y - 1, 18, 18), 0xAAFFFF00);
                        }
                    }
                } else if (row instanceof GroupHeaderRow headerRow) {
                    var group = headerRow.group;
                    if (group.icon() != null) {
                        var renderContext = new SimpleRenderContext(LytRect.empty(), guiGraphics);
                        renderContext.renderItem(
                                group.icon().toStack(),
                                GUI_PADDING_X + PATTERN_PROVIDER_NAME_MARGIN_X,
                                GUI_PADDING_Y + 17 + i * ROW_HEIGHT,
                                8,
                                8);
                    }

                    final int rows = this.byGroup.get(group).size();

                    FormattedText displayName;
                    if (rows > 1) {
                        displayName = Component.empty()
                                .append(group.name())
                                .append(Component.literal(" (" + rows + ')'));
                    } else {
                        displayName = group.name();
                    }

                    var text = Language.getInstance().getVisualOrder(
                            this.font.substrByWidth(displayName, TEXT_MAX_WIDTH - 10));

                    guiGraphics.drawString(font, text, GUI_PADDING_X + PATTERN_PROVIDER_NAME_MARGIN_X + 10,
                            GUI_PADDING_Y + 17 + i * ROW_HEIGHT, textColor, false);


                }
            }
        }
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        Blitter.texture("guis/pw_pattern_encode.png")
                .src(0, 71, 195, 89 )
                .dest(offsetX + imageWidth - 195, offsetY)
                .blit(guiGraphics);

        final int scrollLevel = scrollbar.getCurrentScroll();

        int currentY = offsetY + 17;

        for (int i = 0; i < this.visibleRows; ++i) {
            // Draw the dialog background for this row
            // Skip 1 pixel for the first row in order to not over-draw on the top scrollbox border,
            // and do the same but for the bottom border on the last row
            boolean firstLine = i == 0;
            boolean lastLine = i == this.visibleRows - 1;

            // Draw the background for the slots in an inventory row
            Rect2i bbox = selectRowBackgroundBox(false, firstLine, lastLine);
            blit(guiGraphics, offsetX, currentY, bbox);
            if (scrollLevel + i < this.rows.size()) {
                var row = this.rows.get(scrollLevel + i);
                if (row instanceof SlotsRow slotsRow) {
                    bbox = selectRowBackgroundBox(true, firstLine, lastLine);
                    bbox.setWidth(GUI_PADDING_X + SLOT_SIZE * slotsRow.slots - 1);
                    blit(guiGraphics, offsetX, currentY, bbox);
                }
            }

            currentY += ROW_HEIGHT;
        }
    }

    private Rect2i selectRowBackgroundBox(boolean isInvLine, boolean firstLine, boolean lastLine) {
        if (isInvLine) {
            if (firstLine) {
                return ROW_INVENTORY_TOP_BBOX;
            } else if (lastLine) {
                return ROW_INVENTORY_BOTTOM_BBOX;
            } else {
                return ROW_INVENTORY_MIDDLE_BBOX;
            }
        } else if (firstLine) {
            return ROW_TEXT_TOP_BBOX;
        } else if (lastLine) {
            return ROW_TEXT_BOTTOM_BBOX;
        } else {
            return ROW_TEXT_MIDDLE_BBOX;
        }
    }

    public void clear() {
        this.byId.clear();
        this.infoMap.clear();
        // invalid caches on refresh
        clearCache();
        this.refreshList();
    }

    void clearCache(){
        patternNameCache.clear();
        inputCache.clear();
        outputCache.clear();
    }

    public void postTileInfo(long id, BlockPos pos, ResourceKey<Level> dim, Direction face) {
        this.infoMap.put(id, new PatternProviderInfo(pos, face, dim));
        this.refreshList();
    }

    public void postFullUpdate(long inventoryId,
                               long sortBy,
                               PatternContainerGroup group,
                               int inventorySize,
                               Int2ObjectMap<ItemStack> slots) {
        var record = new PatternContainerRecord(inventoryId, inventorySize, sortBy, group);
        this.byId.put(inventoryId, record);

        var inventory = record.getInventory();
        for (var entry : slots.int2ObjectEntrySet()) {
            inventory.setItemDirect(entry.getIntKey(), entry.getValue());
        }

        // invalid caches on refresh
        clearCache();
        this.refreshList();
    }

    public void postIncrementalUpdate(long inventoryId,
                                      Int2ObjectMap<ItemStack> slots) {
        var record = byId.get(inventoryId);
        if (record == null) {
            return;
        }

        var inventory = record.getInventory();
        for (var entry : slots.int2ObjectEntrySet()) {
            inventory.setItemDirect(entry.getIntKey(), entry.getValue());
        }
    }

    @Override
    protected void setSearchText(String text) {
        super.setSearchText(text);
        if(isFilterInput() || isFilterOutput()){
            refreshList();
        }
    }

    boolean isFilterInput(){
        return filterInput != null && filterInput.getCurrent() == FilterInput.TRUE;
    }

    boolean isFilterOutput(){
        return filterOutput != null && filterOutput.getCurrent() == FilterOutput.TRUE;
    }

    boolean checkCrafting(PatternContainerRecord record){
        return switch (showCraftingPattern.getCurrent()){
            case All -> true;
            case Crafting -> Utils.isCraftingMachine(record.getGroup());
            case Processing -> Utils.isProcessingMachine(record.getGroup());
        };
    }

    public void setShowCrafting(boolean crafting){
        if(crafting){
            showCraftingPattern.setCurrent(ShowCraftingPattern.Crafting);
        } else if(showCraftingPattern.getCurrent() == ShowCraftingPattern.Crafting){
            showCraftingPattern.setCurrent(ShowCraftingPattern.All);
        }
    }

    private void refreshList() {
        this.byGroup.clear();
        this.highlightBtns.forEach((k, v) -> this.removeWidget(v));
        this.highlightBtns.clear();
        this.transferBtns.forEach((k, v) -> this.removeWidget(v));
        this.transferBtns.clear();

        this.matchedInputStack.clear();
        this.matchedOutputStack.clear();

        final String patternFilter = searchPatternField.getValue().toLowerCase();

        final String inputFilter = searchField.getValue().toLowerCase();

        final String outputFilter = searchField.getValue().toLowerCase();

        Set<PatternContainerRecord> result =
                byId.values().stream().filter(this::checkCrafting).collect(Collectors.toCollection(HashSet::new));

        if(!patternFilter.isEmpty()){
            Set<PatternContainerRecord> patternSet = getCache(patternNameCache, patternFilter,
                    entry -> matchesPatternName(entry, patternFilter));
            result.retainAll(patternSet);
        }

        if(isFilterInput()){
            Set<PatternContainerRecord> inputSet = getCache(inputCache, inputFilter,
                    entry -> matchesInput(entry, inputFilter));
            result.retainAll(inputSet);
        }

        if(isFilterOutput()){
            Set<PatternContainerRecord> outputSet = getCache(outputCache, outputFilter,
                    entry -> matchesOutput(entry, outputFilter));
            result.retainAll(outputSet);
        }


        for (PatternContainerRecord entry : result) {

            this.byGroup.put(entry.getGroup(), entry);
        }


        this.groups.clear();
        this.groups.addAll(this.byGroup.keySet());

        this.groups.sort(GROUP_COMPARATOR);

        this.rows.clear();
        this.rows.ensureCapacity(this.getMaxRows());

        for (var group : this.groups) {
            var containers = new ArrayList<>(this.byGroup.get(group));
            Collections.sort(containers);
            var tBtn = new PWActionButton(PWIcon.WHITE_ARROW_DOWN,
                    Component.translatable("gui.ae2pw.quickMovePatternTitle"),
                    Component.translatable("gui.ae2pw.quickMovePatternTooltip2"),
                    ()->{
                        for (var container : containers) {
                            if(Utils.quickInsert(container.getInventory(), ItemStack.EMPTY)){
                                final InventoryActionPacket p = new InventoryActionPacket(
                                        InventoryAction.FILL_ITEM, //what?
                                        hasShiftDown() ? -2 : -1,
                                        container.getServerId()
                                );
                                NetworkHandler.instance().sendToServer(p);
                                break;
                            }
                        }
                }
            );
            tBtn.setVisibility(false);
            tBtn.setHalfSize(true);

            transferBtns.put(this.rows.size(), this.addRenderableWidget(tBtn));

            this.rows.add(new GroupHeaderRow(group));
            for (var container : containers) {
                var inventory = container.getInventory();
                //noinspection SizeReplaceableByIsEmpty
                if (inventory.size() > 0) {
                    var info = this.infoMap.get(container.getServerId());
                    var btn = new HighlightButton();
                    btn.setMultiplier(this.playerToBlockDis(info.pos()));
                    btn.setTarget(info.pos, info.face, info.world);
                    btn.setSuccessJob(() -> {
                        if (this.getPlayer() != null && info.pos != null && info.world != null) {
                            Component message = MessageUtil.createEnhancedHighlightMessage(this.getPlayer(), info.pos, info.world, "chat.ex_pattern_access_terminal.pos");
                            this.getPlayer().displayClientMessage(message, false);
                            if(hasShiftDown()){
                                menu.tpToProvider(container.getServerId());
                                Minecraft.getInstance().setScreen(null);
                            }
                        }
                    });
                    btn.setTooltip(Tooltip.create(
                            Component.translatable("gui.expatternprovider.ex_pattern_access_terminal.tooltip.03")
                                            .append(Component.literal("\nShift点击可传送至目标"))
                    ));

                    btn.setVisibility(false);
                    btn.setHalfSize(true);
                    this.highlightBtns.put(this.rows.size(), this.addRenderableWidget(btn));

                    //var tBtn = new com.ctnh.ae2pw.client.button.PWActionButton()
                }
                for (var offset = 0; offset < inventory.size(); offset += COLUMNS) {
                    var slots = Math.min(inventory.size() - offset, COLUMNS);
                    var containerRow = new SlotsRow(container, offset, slots);
                    this.rows.add(containerRow);
                }
            }
        }

        // lines may have changed - recalculate scroll bar.
        this.resetScrollbar();
    }

    private double playerToBlockDis(BlockPos pos) {
        if (pos == null) {
            return 0;
        }
        var ps = this.getPlayer().getOnPos();
        return pos.distSqr(ps);
    }

    private void resetScrollbar() {
        // Needs to take the border into account, so offset for 1 px on the top and bottom.
        scrollbar.setHeight(this.visibleRows * ROW_HEIGHT - 2);
        scrollbar.setRange(0, this.rows.size() - this.visibleRows, 2);
    }


    private Set<PatternContainerRecord> getCache(
            Map<String, Set<PatternContainerRecord>> cacheMap,
            String term,
            Predicate<PatternContainerRecord> matcher
    ) {
        if (!cacheMap.containsKey(term)) {

            Set<PatternContainerRecord> set = new HashSet<>();

            // 前缀继承
            if (term.length() > 1) {
                set.addAll(getCache(cacheMap, term.substring(0, term.length() - 1), matcher));
            } else {
                set.addAll(this.byId.values());
            }

            // 过滤
            set.removeIf(entry -> !matcher.test(entry));

            cacheMap.put(term, set);
        }

        return cacheMap.get(term);
    }

    private boolean matchesPatternName(PatternContainerRecord entry, String term) {
        if(ModList.get().isLoaded("jecharacters")){
            return Match.contains(entry.getSearchName(), term);
        }
        return entry.getSearchName().contains(term);
    }

    private boolean matchesInput(PatternContainerRecord entry, String term) {
        for (ItemStack stack : entry.getInventory()) {
            if (this.itemStackMatchesSearchTerm(stack, term, matchedInputStack, true)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesOutput(PatternContainerRecord entry, String term) {
        for (ItemStack stack : entry.getInventory()) {
            if (this.itemStackMatchesSearchTerm(stack, term, matchedOutputStack, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean itemStackMatchesSearchTerm(ItemStack itemStack, String searchTerm, Object2BooleanMap<ItemStack> map, boolean checkInput){
        if (itemStack.isEmpty()) {
            return false;
        }

        if(map.containsKey(itemStack)) return map.getBoolean(itemStack);

        IPatternDetails result = null;
        if (itemStack.getItem() instanceof EncodedPatternItem pattern) {
            result = pattern.decode(itemStack, this.menu.getPlayer().level(), false);
        }
        if (result == null) {
            map.put(itemStack, false);
            return false;
        }

        var list = checkInput ?
                Arrays.stream(result.getInputs()).map(i -> i.getPossibleInputs()[0]).toList() :
                Arrays.asList(result.getOutputs());

        for (var item : list) {
            if (item != null) {
                var displayName = item.what().getDisplayName().getString().toLowerCase();
                var match = false;
                if(ModList.get().isLoaded("jecharacters")){
                    match = Match.contains(displayName, searchTerm);
                } else {
                    match = displayName.contains(searchTerm);
                }
                if(match){
                    map.put(itemStack, true);
                    return true;
                }
            }
        }

        map.put(itemStack, false);
        return false;

    }

    private int getMaxRows() {
        return this.groups.size() + this.byId.size();
    }


    private void blit(GuiGraphics guiGraphics, int offsetX, int offsetY, Rect2i srcRect) {
        var texture = AppEng.makeId("textures/guis/patternworkstation.png");
        guiGraphics.blit(texture, offsetX, offsetY, srcRect.getX(), srcRect.getY(), srcRect.getWidth(),
                srcRect.getHeight());
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
