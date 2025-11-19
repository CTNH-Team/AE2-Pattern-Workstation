package com.ctnh.ae2pw.common;

import com.ctnh.ae2pw.AE2PW;
import com.ctnh.ae2pw.AE2PWConfig;
import com.ctnh.ae2pw.data.AE2PWDatagen;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = AE2PW.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonProxy {
    public CommonProxy() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        init(eventBus);
    }
    public static void init(IEventBus eventBus) {
        AE2PWConfig.init();
        AE2PWDatagen.init();
    }
}
