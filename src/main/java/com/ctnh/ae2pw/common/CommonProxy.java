package com.ctnh.ae2pw.common;

import appeng.api.parts.PartModels;
import appeng.core.AppEng;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.ctnh.ae2pw.AE2PW;
import com.ctnh.ae2pw.AE2PWConfig;
import com.ctnh.ae2pw.data.AE2PWDatagen;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import static com.ctnh.ae2pw.AE2PW.REGISTRATE;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = AE2PW.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
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
        PATTERN_WORKSTATION = REGISTRATE.item("pattern_workstation",
                p -> new PartItem<>(p, PatternWorkStationPart.class, PatternWorkStationPart::new)
                )
                .model((ctx, p) ->
                        PartModels.registerModels(PartModelsHelper.createModels(PatternWorkStationPart.class)))
                .register();
        eventBus.addListener((RegisterEvent event) -> {
            if (!event.getRegistryKey().equals(Registries.BLOCK)) {
                return;
            }

            ForgeRegistries.MENU_TYPES.register(AppEng.makeId("pattern_workstation"), PatternWorkStationMenu.TYPE);
        });
    }
}
