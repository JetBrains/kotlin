package org.jetbrains.k2js.utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class GenerationUtils {

    @NotNull
    public static String generateCallToMain(@NotNull String namespaceName, @NotNull List<String> arguments) {
        String constructArguments = "var args = [];\n";
        int index = 0;
        for (String argument : arguments) {
            constructArguments = constructArguments + "args[" + index + "]= \"" + argument + "\";\n";
            index++;
        }
        String callMain = namespaceName + ".main(args);\n";
        return constructArguments + callMain;
    }


}
