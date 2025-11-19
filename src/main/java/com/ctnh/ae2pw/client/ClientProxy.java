package com.ctnh.ae2pw.client;

import appeng.init.client.InitScreens;
import com.ctnh.ae2pw.AE2PW;
import com.ctnh.ae2pw.common.CommonProxy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod.EventBusSubscriber(modid = AE2PW.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE,value = Dist.CLIENT)
public class ClientProxy extends CommonProxy {
    public ClientProxy() {
        super();

    }

}
