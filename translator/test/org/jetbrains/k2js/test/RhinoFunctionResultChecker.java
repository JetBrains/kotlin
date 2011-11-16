package org.jetbrains.k2js.test;

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

    public RhinoFunctionResultChecker(String namespaceName, String functionName, Object expectedResult) {
        this.namespaceName = namespaceName;
        this.functionName = functionName;
        this.expectedResult = expectedResult;
    }

    @Override
    public void runChecks(Context context, Scriptable scope) throws Exception {
        Object result = extractAndCallFunctionObject(namespaceName, functionName, context, scope);
        assertTrue("Result is not what expected!", result.equals(expectedResult));
        String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
        System.out.println(report);
    }

    private Object extractAndCallFunctionObject(String namespaceName, String functionName,
                                                Context cx, Scriptable scope) {
        NativeObject namespaceObject = RhinoUtils.extractObject(namespaceName, scope);
        Object box = namespaceObject.get(functionName, namespaceObject);
        assertTrue("Function " + functionName + " not defined in namespace " + namespaceName, box instanceof Function);
        Object functionArgs[] = {};
        Function function = (Function) box;
        return function.call(cx, scope, scope, functionArgs);
    }
}
