package com.ctnh.ae2pw.client.icon;

import appeng.client.gui.style.Blitter;
import appeng.core.AppEng;
import com.ctnh.ae2pw.AE2PW;
import net.minecraft.resources.ResourceLocation;

public enum PWIcon{

    WHITE_ARROW_DOWN(48, 0),
    YSE(16, 128),
    NO(0, 128),
    FILL_PLACEHOLDER(32, 0),
    FILL_VALUE(32, 16),
    FILTER_INPUT_TRUE(0, 0),
    FILTER_INPUT_FALSE(0, 16),
    FILTER_OUTPUT_TRUE(16, 0),
    FILTER_OUTPUT_FALSE(16, 16),
    COPY(0, 32),
    MERGE_SAME_TRUE(32, 32),
    MERGE_SAME_FALSE(16, 32),
    SHOW_ALL(32, 48),
    SHOW_CRAFTING(0, 48),
    SHOW_PROCESSING(16, 48),
    X2(48, 16)
    ;


    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(AE2PW.MODID, "textures/guis/states.png");
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    PWIcon(int x, int y) {
        this(x, y, 16, 16);
    }

    PWIcon(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Blitter getBlitter() {
        return Blitter.texture(TEXTURE, TEXTURE_WIDTH, TEXTURE_HEIGHT)
                .src(x, y, width, height);
    }
}
