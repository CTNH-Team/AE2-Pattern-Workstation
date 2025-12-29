package com.ctnh.ae2pw.common;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.parts.PartModels;
import appeng.core.AppEng;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.ctnh.ae2pw.AE2PW;
import com.ctnh.ae2pw.AE2PWConfig;
import com.ctnh.ae2pw.data.AE2PWDatagen;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import static com.ctnh.ae2pw.AE2PW.REGISTRATE;

@SuppressWarnings("removal")

public class CommonProxy {
    public CommonProxy() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        init(eventBus);
    }

    public static ItemEntry<PartItem<PatternWorkStationPart>> PATTERN_WORKSTATION;

    public static void init(IEventBus eventBus) {
        AE2PWConfig.init();
        AE2PWDatagen.init();
        REGISTRATE.registerRegistrate();
        PATTERN_WORKSTATION = REGISTRATE.item("pattern_workstation",
                p -> new PartItem<>(p, PatternWorkStationPart.class, PatternWorkStationPart::new)
                )
                .lang("ME Pattern Workstation")
                .model((ctx, p) ->
                        PartModels.registerModels(PartModelsHelper.createModels(PatternWorkStationPart.class)))
                .register();
        eventBus.addListener((RegisterEvent event) -> {
            if (!event.getRegistryKey().equals(Registries.MENU)) {
                return;
            }
            if(!ForgeRegistries.MENU_TYPES.containsKey(AppEng.makeId("patternworkstation")))
                ForgeRegistries.MENU_TYPES.register(AppEng.makeId("patternworkstation"), PatternWorkStationMenu.TYPE);
        });

        eventBus.addListener((BuildCreativeModeTabContentsEvent event) ->{
            if(event.getTabKey() == AECreativeTabIds.MAIN){
                event.accept(() -> PATTERN_WORKSTATION.asItem());
            }
        });
    }
}
