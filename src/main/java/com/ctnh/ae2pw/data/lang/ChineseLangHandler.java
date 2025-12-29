package com.ctnh.ae2pw.data.lang;

public class ChineseLangHandler {
    public static void init(RegistrateCNLangProvider provider){
        provider.add("gui.ae2pw.PatternAccess", "样板管理");
        provider.add("gui.ae2pw.PatternBuffer", "样板缓存");
        provider.add("gui.ae2pw.Storage", "库存");
        provider.add("gui.ae2pw.SelectAmountOrRename", "设置数量/重命名物品");
        provider.add("gui.ae2pw.rename", "请输入自定义名称");
        provider.add("gui.ae2pw.quickMovePatternTitle", "快速转移");
        provider.add("gui.ae2pw.quickMovePatternTooltip1", "将缓存区最后一个样板转移至管理区第一个空槽位");
        provider.add("gui.ae2pw.quickMovePatternTooltip2", "将缓存区最后一个样板转移至该组第一个空槽位");
    }
}
