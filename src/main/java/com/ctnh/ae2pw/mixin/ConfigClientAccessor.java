package com.ctnh.ae2pw.mixin;

import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yuuki1293.pccard.ConfigClient;

@Mixin(value = ConfigClient.class, remap = false)
public interface ConfigClientAccessor {

    @Accessor("JEI_INTEGRATION")
    static ForgeConfigSpec.BooleanValue getJeiIntegrationValue() {
        throw new AssertionError();
    }
}

