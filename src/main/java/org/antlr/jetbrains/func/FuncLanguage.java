package org.antlr.jetbrains.func;

import com.intellij.lang.Language;

public class FuncLanguage extends Language {
    public static final FuncLanguage INSTANCE = new FuncLanguage();

    private FuncLanguage() {
        super("Func");
    }
}
