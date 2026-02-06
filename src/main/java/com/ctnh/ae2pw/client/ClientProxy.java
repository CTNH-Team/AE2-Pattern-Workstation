package com.ctnh.ae2pw.client;

import appeng.api.util.AEColor;
import appeng.init.client.InitScreens;
import com.ctnh.ae2pw.AE2PW;
import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import com.ctnh.ae2pw.common.CommonProxy;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


public class ClientProxy extends CommonProxy {
    @SuppressWarnings("removal")
    public ClientProxy() {
        super();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerItemColors);
        init();
    }

    private static void init() {

    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            InitScreens.register(
                    PatternWorkStationMenu.TYPE,
                    PatternWorkStationScreen::new,
                    "/screens/terminals/pattern_workstation.json"
            );
        });
    }

    public void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicItemColor(AEColor.TRANSPARENT), CommonProxy.PATTERN_WORKSTATION);
    }
}
