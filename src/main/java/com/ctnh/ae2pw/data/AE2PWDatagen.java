package com.ctnh.ae2pw.data;

import com.ctnh.ae2pw.data.lang.ChineseLangHandler;
import com.ctnh.ae2pw.data.lang.EnglishLangHandler;
import com.tterrag.registrate.providers.ProviderType;

import static com.ctnh.ae2pw.AE2PW.REGISTRATE;
import static com.ctnh.ae2pw.data.lang.ProviderTypes.CNLANG;

public class AE2PWDatagen {
    public static void init(){
        REGISTRATE.addDataGenerator(ProviderType.LANG, EnglishLangHandler::init);
        REGISTRATE.addDataGenerator(CNLANG, ChineseLangHandler::init);
    }
}
