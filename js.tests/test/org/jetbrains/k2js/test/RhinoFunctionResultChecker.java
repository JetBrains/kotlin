/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.test;

import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Talanov
 */
public final class RhinoFunctionResultChecker implements RhinoResultChecker {

    private final String namespaceName;
    private final String functionName;
    private final Object expectedResult;

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
