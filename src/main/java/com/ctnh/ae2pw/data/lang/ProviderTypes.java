package com.ctnh.ae2pw.data.lang;

import com.tterrag.registrate.providers.ProviderType;

public class ProviderTypes {
    public static ProviderType<RegistrateCNLangProvider> CNLANG = ProviderType.register("cnlang", (p,e)->
        new RegistrateCNLangProvider(p, e.getGenerator().getPackOutput())
    ) ;
}
