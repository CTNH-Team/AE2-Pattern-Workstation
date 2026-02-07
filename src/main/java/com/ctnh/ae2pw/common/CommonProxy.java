package com.ctnh.ae2pw.common;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.parts.PartModels;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import com.ctnh.ae2pw.AE2PW;
import com.ctnh.ae2pw.AE2PWConfig;
import com.ctnh.ae2pw.data.AE2PWDatagen;
import com.ctnh.ae2pw.utils.Utils;
import com.glodblock.github.extendedae.common.EPPItemAndBlock;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.List;

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
                .recipe((ctx, provider) ->{
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.getEntry(), 1)
                            .pattern("abc")
                            .define('a', AEParts.PATTERN_ENCODING_TERMINAL)
                            .define('b', EPPItemAndBlock.PATTERN_MODIFIER)
                            .define('c', AEParts.PATTERN_ACCESS_TERMINAL)
                            .unlockedBy("has_pattern_encoding_terminal",
                                    new InventoryChangeTrigger.TriggerInstance(ContextAwarePredicate.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY,
                                            new ItemPredicate[]{
                                                    ItemPredicate.Builder.item().of(new ItemLike[]{AEParts.PATTERN_ENCODING_TERMINAL}).build()
                                            }))
                            .save(provider, AE2PW.id("crafting/patternworkstation"));
                })
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

        eventBus.addListener(CommonProxy::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event){
        event.enqueueWork(() -> {
            Utils.craftingMachines.addAll(List.of(
                    AEBlocks.MOLECULAR_ASSEMBLER.block().getName(),
                    EPPItemAndBlock.EX_ASSEMBLER.getName(),
                    EPPItemAndBlock.ASSEMBLER_MATRIX_PATTERN.getName()
            ));
        });
    }
}
