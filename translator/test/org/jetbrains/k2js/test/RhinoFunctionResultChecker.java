package org.jetbrains.k2js.test;

import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Talanov
 */
public final class RhinoFunctionResultChecker implements RhinoResultChecker {

    final String namespaceName;
    final String functionName;
    final Object expectedResult;

    public RhinoFunctionResultChecker(@Nullable String namespaceName, String functionName, Object expectedResult) {
        this.namespaceName = namespaceName;
        this.functionName = functionName;
        this.expectedResult = expectedResult;
    }

    public RhinoFunctionResultChecker(String functionName, Object expectedResult) {
        this(null, functionName, expectedResult);
    }

    @Override
    public void runChecks(Context context, Scriptable scope) throws Exception {
        Object result = evaluateFunction(context, scope);
        assertTrue("Result is not what expected! Expected: " + expectedResult + " Evaluated : " + result,
                result.equals(expectedResult));
        String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
        System.out.println(report);
    }

    private Object evaluateFunction(Context cx, Scriptable scope) {
        return cx.evaluateString(scope, functionCallString(), "function call", 0, null);
    }

    private String functionCallString() {
        String result = functionName + "()";
        if (namespaceName != null) {
            result = namespaceName + "." + result;
        }
        return result;
    }
}
