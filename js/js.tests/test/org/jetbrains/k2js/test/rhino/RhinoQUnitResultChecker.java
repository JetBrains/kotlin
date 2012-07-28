/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.k2js.test.config.TestConfig;
import org.jetbrains.k2js.translate.context.Namer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.flushSystemOut;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Runs the QUnit test cases in headless mode (without requiring a browser) and asserts they all PASS
 */
public class RhinoQUnitResultChecker implements RhinoResultChecker {

    private final String moduleId;

    public RhinoQUnitResultChecker(@Nullable String moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public void runChecks(Context context, Scriptable scope) throws Exception {
        Object result = evaluateFunction(context, scope);
        flushSystemOut(context, scope);
        assertResultValid(result, context);
    }

    protected void assertResultValid(Object result, Context context) {
        if (result instanceof NativeArray) {
            NativeArray array = (NativeArray) result;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0, size = array.size(); i < size; i++) {
                Object value = array.get(i);
                String text = value.toString();
                System.out.println(text);

                if (!text.startsWith("PASS")) {
                    if (buffer.length() > 0) {
                        buffer.append("\n");
                    }
                    buffer.append(text);
                }
            }
            if (buffer.length() > 0) {
                fail(buffer.toString());
            }
        } else {
            fail("Uknown QUnit result: " + result);
        }
    }

    private Object evaluateFunction(Context cx, Scriptable scope) {
        return cx.evaluateString(scope, functionCallString(), "function call", 0, null);
    }

    protected String functionCallString() {
        return "runQUnitSuite()";
    }

}
