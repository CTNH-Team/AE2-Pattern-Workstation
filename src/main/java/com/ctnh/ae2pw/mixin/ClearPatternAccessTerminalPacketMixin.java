package com.ctnh.ae2pw.mixin;

import appeng.core.sync.packets.ClearPatternAccessTerminalPacket;
import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClearPatternAccessTerminalPacket.class, remap = false)
public class ClearPatternAccessTerminalPacketMixin {
    @Inject(
            method = "clientPacketData",
            at = @At("HEAD"),
            remap = false
    )
    private void handleExGui(Player player, CallbackInfo ci) {
        if(Minecraft.getInstance().screen instanceof PatternWorkStationScreen patternWorkStationScreen){
            patternWorkStationScreen.clear();
        }
    }
}
