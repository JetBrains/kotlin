/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import static org.jetbrains.kotlin.js.test.rhino.RhinoUtils.flushSystemOut;
import static org.junit.Assert.fail;

/**
 * Runs the QUnit test cases in headless mode (without requiring a browser) and asserts they all PASS
 */
public class RhinoQUnitResultChecker implements RhinoResultChecker {
    @Override
    public void runChecks(Context context, Scriptable scope) throws Exception {
        Object result = evaluateFunction(context, scope);
        flushSystemOut(context, scope);
        assertResultValid(result);
    }

    protected void assertResultValid(Object result) {
        if (result instanceof NativeArray) {
            NativeArray array = (NativeArray) result;
            StringBuilder buffer = new StringBuilder();
            for (Object value : array) {
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
            fail("Unknown QUnit result: " + result);
        }
    }

    private Object evaluateFunction(Context cx, Scriptable scope) {
        return cx.evaluateString(scope, functionCallString(), "function call", 0, null);
    }

    protected String functionCallString() {
        return "runQUnitSuite()";
    }
}
