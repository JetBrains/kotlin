package org.jetbrains.k2js.test;

import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Talanov Pavel
 */
public final class RhinoSystemOutputChecker implements RhinoResultChecker {

    private final List<String> arguments;
    private final String expectedResult;

    public RhinoSystemOutputChecker(String expectedResult, List<String> arguments) {
        this.expectedResult = expectedResult;
        this.arguments = arguments;
    }

    @Override
    public void runChecks(@NotNull Context context, @NotNull Scriptable scope)
            throws Exception {
        runMain(context, scope);
        String result = getSystemOutput(context, scope);
        assertTrue("Returned:\n" + result + "\n\nExpected:\n" + expectedResult, result.equals(expectedResult));
    }

    private String getSystemOutput(@NotNull Context context, @NotNull Scriptable scope) {
        Object output = context.evaluateString(scope, "Kotlin.System.output()", "test", 0, null);
        assertTrue("Output should be a string.", output instanceof String);
        return (String) output;
    }

    private void runMain(Context context, Scriptable scope) {
        context.evaluateString(scope, execMain(), "function call", 0, null);
    }

    @NotNull
    private String execMain() {
        String constructArguments = "var args = Kotlin.array(" + arguments.size() + ");\n";
        int index = 0;
        for (String argument : arguments) {
            constructArguments = constructArguments + "args.set(" + index + ", \"" + argument + "\");\n";
            index++;
        }
        String callMain = "Anonymous.main(args);\n";
        return constructArguments + callMain;
    }
}
