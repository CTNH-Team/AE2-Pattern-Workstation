package com.ctnh.ae2pw.common;

import appeng.api.storage.ITerminalHost;

public interface IPatternWorkStationMenuHost extends ITerminalHost {
    PatternWorkStationLogic getLogic();
}
