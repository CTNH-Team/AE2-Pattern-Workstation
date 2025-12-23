package com.ctnh.ae2pw.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;


@Mixin(value = AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
//    @Shadow
//    protected int leftPos;
//
//    @Shadow
//    protected int imageWidth;
//
//    @Inject(method = "init", at = @At("TAIL"))
//    public void addLeft(CallbackInfo ci){
//        if((Object)this instanceof PatternWorkStationScreen){
//            leftPos += imageWidth /2;
//        }
//    }
}
