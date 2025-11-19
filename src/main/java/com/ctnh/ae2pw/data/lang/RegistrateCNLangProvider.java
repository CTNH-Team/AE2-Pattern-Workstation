package com.ctnh.ae2pw.data.lang;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.RegistrateProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.LogicalSide;

import static com.ctnh.ae2pw.data.lang.ProviderTypes.CNLANG;

public class RegistrateCNLangProvider extends LanguageProvider implements RegistrateProvider {
    private final AbstractRegistrate<?> owner;

    public RegistrateCNLangProvider(AbstractRegistrate<?> owner, PackOutput packOutput) {
        super(packOutput, owner.getModid(), "zh_cn");
        this.owner = owner;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

    public String getName() {
        return "Lang (zh_cn)";
    }

    @Override
    protected void addTranslations() {
        owner.genData(CNLANG, this);
    }
}
