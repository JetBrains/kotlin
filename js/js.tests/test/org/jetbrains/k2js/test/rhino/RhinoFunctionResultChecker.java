/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.rhino;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.translate.context.Namer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.flushSystemOut;
import static org.junit.Assert.assertEquals;

public class RhinoFunctionResultChecker implements RhinoResultChecker {

    private final String moduleId;
    private final String namespaceName;
    private final String functionName;
    private final Object expectedResult;

    public RhinoFunctionResultChecker(@Nullable String namespaceName, String functionName, Object expectedResult) {
        this(Config.REWRITABLE_MODULE_NAME, namespaceName, functionName, expectedResult);
    }

    public RhinoFunctionResultChecker(@Nullable String moduleId, @Nullable String namespaceName, String functionName, Object expectedResult) {
        this.moduleId = moduleId;
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
        flushSystemOut(context, scope);
        assertResultValid(result, context);
    }

    protected void assertResultValid(Object result, Context context) {
        String ecmaVersion = context.getLanguageVersion() == Context.VERSION_1_8 ? "ecma5" : "ecma3";
        assertEquals("Result of " + namespaceName + "." + functionName + "() is not what expected (" + ecmaVersion + ")!", expectedResult, result);
        String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
        System.out.println(report);
    }

    private Object evaluateFunction(Context cx, Scriptable scope) {
        return cx.evaluateString(scope, functionCallString(), "function call", 0, null);
    }

    protected String functionCallString() {
        StringBuilder sb = new StringBuilder();
        if (namespaceName != null) {
            sb.append("Kotlin.modules");
            if (moduleId.contains(".")) {
                sb.append("['").append(moduleId).append("']");
            } else {
                sb.append(".").append(moduleId);
            }
            if (namespaceName != Namer.getRootNamespaceName()) {
                sb.append('.').append(namespaceName);
            }
            sb.append('.');
        }
        return sb.append(functionName).append("()").toString();
    }
}
