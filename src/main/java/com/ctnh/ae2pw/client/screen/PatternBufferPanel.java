package com.ctnh.ae2pw.client.screen;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.slot.AppEngSlot;
import com.ctnh.ae2pw.client.Icon;
import com.ctnh.ae2pw.client.button.PWActionButton;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import static com.ctnh.ae2pw.common.PatternWorkStationLogic.MAX_PATTERN_SLOTS;

public class PatternBufferPanel implements ICompositeWidget {
    private static final Blitter BG = Blitter.texture("guis/pattern.png").src(0, 0, 195, 71);
    private static final Blitter BG_HEAD = BG
            .copy()
            .src(0, 0, 195, 17);

    private static final Blitter BG_FIRST_ROW = BG
            .copy()
            .src(0, 17, 195, 18);

    private static final Blitter BG_ROW = BG
            .copy()
            .src(0, 35, 195, 18);

    private static final Blitter BG_LAST_ROW = BG
            .copy()
            .src(0, 53, 195, 18);
    private static final int COLUMNS = 9;

    public static String QUICK_MOVE_PATTERN_TITLE = "gui.ae2pw.quickMovePatternTitle";
    public static String QUICK_MOVE_PATTERN_TOOLTIP = "gui.ae2pw.quickMovePatternTooltip";

    protected final PatternWorkStationScreen screen;
    protected final PatternWorkStationMenu menu;
    protected final WidgetContainer widgets;
    @Setter
    protected boolean visible = true;

    int x;
    int y;
    static int WIDTH = 195;
    int height;

    int slotX;
    int slotY;

    private final Scrollbar scrollbar;
    private int visibleRows = 0;

    public PatternBufferPanel(PatternWorkStationScreen screen, WidgetContainer widgets) {
        this.screen = screen;
        this.menu = screen.getMenu();
        this.widgets = widgets;

        this.scrollbar = widgets.addScrollBar("patternBufferScrollbar");
        this.scrollbar.setCaptureMouseWheel(false);

        var quickMoveBtn = new PWActionButton(Icon.WHITE_ARROW_DOWN,
                Component.translatable(QUICK_MOVE_PATTERN_TITLE),
                Component.translatable(QUICK_MOVE_PATTERN_TOOLTIP),
                screen::quickMovePattern
                );
        quickMoveBtn.setHalfSize(true);
        widgets.add("quickMovePattern", quickMoveBtn);
    }

    public void init(int rowSpace){
        height = rowSpace;
        slotX = menu.encodedPatternSlots[0].x;
        slotY = menu.encodedPatternSlots[0].y;
        visibleRows = Math.max((rowSpace -17) / 18, 2);
        scrollbar.setHeight(visibleRows * 18 - 2);
        scrollbar.setRange(0, MAX_PATTERN_SLOTS/COLUMNS - visibleRows, 2);
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        return scrollbar.onMouseWheel(mousePos, delta);
    }

    @Override
    public void setPosition(Point position) {
        x = position.getX();
        y = position.getY();
    }

    @Override
    public void setSize(int width, int height) {
        this.height = Math.max(height / 18, 1) * 18;
    }

    @Override
    public Rect2i getBounds() {
        return new Rect2i(x, y, WIDTH, height);
    }

    @Override
    public final boolean isVisible() {
        return visible;
    }

    @Override
    public void updateBeforeRender() {
        int scroll = scrollbar.getCurrentScroll();
        var slots = menu.encodedPatternSlots;
        for (int i = 0; i < slots.length; i++) {
            AppEngSlot slot = slots[i];

            int row = i / COLUMNS;
            int col = i % COLUMNS;

            int visibleRow = row - scroll;

            boolean visible = visibleRow >= 0 && visibleRow < visibleRows;

            slot.setActive(visible);

            if (visible) {
                slot.x = slotX + col * 18;
                slot.y = slotY + visibleRow * 18;
            }

        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        var offsetX = bounds.getX() + x;
        var offsetY = bounds.getY() + y;
        BG_HEAD.dest(offsetX, offsetY).blit(guiGraphics);
        offsetY += BG_HEAD.getSrcHeight();
        for(int i = 0; i < visibleRows; i++){
            var texture = i == 0 ? BG_FIRST_ROW :
                            i == visibleRows - 1 ? BG_LAST_ROW : BG_ROW;

            texture.dest(offsetX, offsetY).blit(guiGraphics);
            offsetY += texture.getSrcHeight();
        }
    }


}
