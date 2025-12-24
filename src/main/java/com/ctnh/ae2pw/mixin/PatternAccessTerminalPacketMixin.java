package com.ctnh.ae2pw.mixin;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.core.sync.packets.PatternAccessTerminalPacket;
import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternAccessTerminalPacket.class, remap = false)
public class PatternAccessTerminalPacketMixin {
    @Shadow
    private boolean fullUpdate;
    @Shadow
    private long inventoryId;
    @Shadow
    private int inventorySize;
    @Shadow
    private long sortBy;
    @Shadow
    private PatternContainerGroup group;
    @Shadow
    private Int2ObjectMap<ItemStack> slots;

    @Inject(
            method = "clientPacketData",
            at = @At("HEAD"),
            cancellable = true
    )
    private void handleExGui(Player player, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof PatternWorkStationScreen patternWorkStationScreen) {
            if (fullUpdate) {
                patternWorkStationScreen.postFullUpdate(this.inventoryId, sortBy, group, inventorySize, slots);
            } else {
                patternWorkStationScreen.postIncrementalUpdate(this.inventoryId, slots);
            }
            ci.cancel();
        }
    }
}
