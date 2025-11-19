package com.ctnh.ae2pw.registry;

import com.ctnh.ae2pw.AE2PW;
import com.tterrag.registrate.AbstractRegistrate;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class AE2PWRegistrate extends AbstractRegistrate<AE2PWRegistrate> {
    protected AE2PWRegistrate() {
        super(AE2PW.MODID);
    }

    public static AE2PWRegistrate create(){
        return new AE2PWRegistrate();
    }

    @SuppressWarnings("removal")
    public void registerRegistrate() {
        registerEventListeners(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
