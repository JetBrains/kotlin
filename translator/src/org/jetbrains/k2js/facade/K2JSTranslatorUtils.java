package org.jetbrains.k2js.facade;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.config.TestConfig;

/**
 * Created by IntelliJ IDEA.
 * User: Natalia.Ukhorskaya
 * Date: 2/9/12
 * Time: 7:49 PM
 */

public class K2JSTranslatorUtils {
    @SuppressWarnings("FieldCanBeLocal")
    private static String EXCEPTION = "exception=";

    @Nullable
    public String translateToJS(@NotNull String code, @NotNull String arguments) {
        try {
            return generateJSCode(code, arguments);
        } catch (AssertionError e) {
            reportException(e);
            return EXCEPTION + "Translation error.";
        } catch (UnsupportedOperationException e) {
            reportException(e);
            return EXCEPTION + "Unsupported feature.";
        } catch (Throwable e) {
            reportException(e);
            return EXCEPTION + "Unexpected exception.";
        }
    }

    @Nullable
    public BindingContext getBindingContext(@NotNull String programText) {
        try {
            K2JSTranslator k2JSTranslator = new K2JSTranslator(new TestConfig());
            return k2JSTranslator.analyzeProgramCode(programText);
        } catch (Throwable e) {
            e.printStackTrace();
            reportException(e);
            return null;
        }
    }

    @NotNull
    private String generateJSCode(@NotNull String code, @NotNull String arguments) {
        String generatedCode = (new K2JSTranslator(new TestConfig())).translateStringWithCallToMain(code, arguments);
        return generatedCode;
    }

    private void reportException(@NotNull Throwable e) {
        System.out.println("Exception in translateToJS!!!");
        e.printStackTrace();
    }
}
