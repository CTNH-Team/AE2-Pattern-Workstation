package com.ctnh.ae2pw.mixin;

import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import com.glodblock.github.extendedae.network.packet.SExPatternInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SExPatternInfo.class, remap = false)
public class SExpatternInfoMixin {
    @Shadow
    private long id;

    @Shadow
    private BlockPos pos;

    @Shadow
    private ResourceKey<Level> dim;

    @Shadow
    private @Nullable Direction face;

    @Inject(method = "onMessage", at = @At("HEAD"), cancellable = true)
    void onMessage(Player player, CallbackInfo ci){
        if(Minecraft.getInstance().screen instanceof PatternWorkStationScreen patternWorkStationScreen) {
            patternWorkStationScreen.postTileInfo(id, pos, dim, face);
            ci.cancel();
        }
    }
}
