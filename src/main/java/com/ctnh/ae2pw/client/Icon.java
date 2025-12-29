package com.ctnh.ae2pw.client;

import appeng.client.gui.style.Blitter;
import appeng.core.AppEng;
import net.minecraft.resources.ResourceLocation;

public enum Icon {

    WHITE_ARROW_DOWN(128, 0),
    YSE(16, 128),
    NO(0, 128)
    ;


    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(AppEng.MOD_ID, "textures/guis/states.png");
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    Icon(int x, int y) {
        this(x, y, 16, 16);
    }

    Icon(int x, int y, int width, int height) {
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
