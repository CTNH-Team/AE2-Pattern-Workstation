package com.ctnh.ae2pw.integration.emi;

import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class AE2PWEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(PatternWorkStationMenu.TYPE,
                new EmiPatternWorkstationHandler(PatternWorkStationMenu.class));
    }
}
