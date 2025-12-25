package com.ctnh.ae2pw.common;

import net.minecraft.world.level.Level;

public interface IPatternWorkstationLogicHost {
    PatternWorkStationLogic getLogic();

    Level getLevel();

    void markForSave();
}
