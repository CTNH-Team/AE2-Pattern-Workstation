package com.ctnh.ae2pw.mixin;

import appeng.core.AELog;
import appeng.core.sync.packets.MEInventoryUpdatePacket;

import appeng.menu.me.common.GridInventoryEntry;
import com.ctnh.ae2pw.common.MEStorageMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MEInventoryUpdatePacket.class, remap = false)
public class MEInventoryUpdatePacketMixin {
    @Shadow
    private int containerId;

    @Shadow
    private boolean fullUpdate;

    @Shadow
    @Final
    private List<GridInventoryEntry> entries;

    @Inject(method = "clientPacketData", at = @At("HEAD"))
    void pwClientPacketData(Player player, CallbackInfo ci){
        if (player.containerMenu.containerId == containerId
                && player.containerMenu instanceof MEStorageMenu meMenu) {
            var clientRepo = meMenu.getClientRepo();
            if (clientRepo == null) {
                AELog.info("Ignoring ME inventory update packet because no client repo is available.");
                return;
            }
            clientRepo.handleUpdate(fullUpdate, entries);
        }
    }
}
