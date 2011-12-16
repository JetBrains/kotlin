package org.jetbrains.k2js;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.utils.ErrorSender;

import java.applet.Applet;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Pavel Talanov
 */
public final class K2JSTranslatorApplet extends Applet {

    public static final int SESSION_ID = 100500;

    @SuppressWarnings("FieldCanBeLocal")
    private static String EXCEPTION = "exception=";

    @Nullable
    public String translateToJS(@NotNull String code, @NotNull String arguments) {
        try {
            return generateJSCode(code, arguments);
        } catch (AssertionError e) {
            reportException(e);
            return EXCEPTION + " Translation error.";
        } catch (UnsupportedOperationException e) {
            reportException(e);
            return EXCEPTION + " Unsupported feature.";
        } catch (Throwable e) {
            reportException(e);
            return EXCEPTION + " Unexpected exception.";
        }
    }

    @NotNull
    private String generateJSCode(@NotNull String code, @NotNull String arguments) {
        String generatedCode = (new K2JSTranslator()).translateStringWithCallToMain(code, arguments);
        System.out.println("GENERATED JAVASCRIPT CODE:\n-----------------------------------\n");
        System.out.println(generatedCode);
        return generatedCode;
    }

    private void reportException(@NotNull Throwable e) {
        System.out.println("Exception in translateToJS!!!");
        e.printStackTrace();
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String request = getCodeBase().getProtocol() + "://" + getCodeBase().getHost();
        ErrorSender.sendTextToServer(stringWriter.toString(), request);
    }
}
