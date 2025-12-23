package com.ctnh.ae2pw;

import com.ctnh.ae2pw.client.ClientProxy;
import com.ctnh.ae2pw.common.CommonProxy;
import com.ctnh.ae2pw.registry.AE2PWRegistrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AE2PW.MODID)
@SuppressWarnings("removal")
public class AE2PW {

    public static final String MODID = "ae2pw";
    public static final Logger LOGGER = LogManager.getLogger();
    public static AE2PWRegistrate REGISTRATE = AE2PWRegistrate.create();

    public AE2PW() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        // Most other events are fired on Forge's bus.
        // If we want to use annotations to register event listeners,
        // we need to register our object like this!
        MinecraftForge.EVENT_BUS.register(this);

        REGISTRATE.registerRegistrate();
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    /**
     * Create a ResourceLocation in the format "modid:path"
     *
     * @param path
     * @return ResourceLocation with the namespace of your mod
     */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }


}
