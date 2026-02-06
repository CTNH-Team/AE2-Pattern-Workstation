package com.ctnh.ae2pw.mixin;

import appeng.api.util.IConfigManager;
import appeng.menu.me.common.MEStorageMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MEStorageMenu.class, remap = false)
public interface MEStorageMenuAccessor {
    @Accessor
    IConfigManager getClientCM();
}
