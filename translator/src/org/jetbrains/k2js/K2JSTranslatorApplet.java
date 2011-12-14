package org.jetbrains.k2js;

import org.jetbrains.annotations.NotNull;

import java.applet.Applet;

/**
 * @author Talanov Pavel
 */
public final class K2JSTranslatorApplet extends Applet {

    @NotNull
    public String translate(@NotNull String code) {
        String generatedCode = (new K2JSTranslator()).translateString(code);
        System.out.println("GENERATED JAVASCRIPT CODE:\n-----------------------------------\n");
        System.out.println(generatedCode);
        return generatedCode;
    }
}
