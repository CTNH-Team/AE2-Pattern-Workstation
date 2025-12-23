package com.ctnh.ae2pw.mixin;

import com.ctnh.ae2pw.client.PatternWorkStationScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int imageWidth;

    @Inject(method = "init", at = @At("TAIL"))
    public void addLeft(CallbackInfo ci){
        if((Object)this instanceof PatternWorkStationScreen){
            leftPos += imageWidth /2;
        }
    }
}
