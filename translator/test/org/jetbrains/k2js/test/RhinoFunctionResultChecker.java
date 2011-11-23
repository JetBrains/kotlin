package org.jetbrains.k2js.test;

import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertTrue;

/**
 * @author Talanov Pavel
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
        Object result = extractAndCallFunctionObject(namespaceName, functionName, context, scope);
        assertTrue("Result is not what expected! Expected: " + expectedResult + " Evaluated : " + result,
                result.equals(expectedResult));
        String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
        System.out.println(report);
    }

    private Object extractAndCallFunctionObject(String namespaceName, String functionName,
                                                Context cx, Scriptable scope) {
        Object functionObject;
        if (namespaceName != null) {
            functionObject = extractFunctionFromObject(namespaceName, functionName, scope);
        } else {
            functionObject = extractFunctionFromGlobalScope(functionName, scope);
        }
        return callFunctionAndCheckResults(cx, scope, (Function) functionObject);
    }

    private Object callFunctionAndCheckResults(Context cx, Scriptable scope, Function functionObject) {
        Object functionArgs[] = {};
        return functionObject.call(cx, scope, scope, functionArgs);
    }

    private Object extractFunctionFromGlobalScope(String functionName, Scriptable scope) {
        Object functionObject;
        functionObject = scope.get(functionName, scope);
        assertTrue("Function " + functionName + " is not defined in global scope",
                functionObject instanceof Function);
        return functionObject;
    }

    private Object extractFunctionFromObject(String namespaceName, String functionName, Scriptable scope) {
        Object functionObject;
        NativeObject namespaceObject = RhinoUtils.extractObject(namespaceName, scope);
        functionObject = namespaceObject.get(functionName, namespaceObject);
        assertTrue("Function " + functionName + " is not defined in namespace " + namespaceName,
                functionObject instanceof Function);
        return functionObject;
    }
}
