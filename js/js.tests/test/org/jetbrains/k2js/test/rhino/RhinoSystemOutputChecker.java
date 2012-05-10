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

import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Talanov
 */
public final class RhinoSystemOutputChecker implements RhinoResultChecker {

    private final String expectedResult;

    public RhinoSystemOutputChecker(@NotNull String expectedResult) {
        this.expectedResult = expectedResult;
    }

    @Override
    public void runChecks(@NotNull Context context, @NotNull Scriptable scope)
            throws Exception {
        String result = getSystemOutput(context, scope);
        String trimmedExpected = trimSpace(expectedResult);
        String trimmedActual = trimSpace(result);

        assertTrue("Returned:\n" + trimmedActual + "END_OF_RETURNED\nExpected:\n" + trimmedExpected
                   + "END_OF_EXPECTED\n", trimmedExpected.equals(trimmedActual));
    }

    private static String getSystemOutput(@NotNull Context context, @NotNull Scriptable scope) {
        Object output = context.evaluateString(scope, "Kotlin.System.output()", "test", 0, null);
        assertTrue("Output should be a string.", output instanceof String);
        return (String) output;
    }

    public static String trimSpace(@NotNull String s) {
        String[] choppedUpString = s.trim().split("\\s");
        StringBuilder sb = new StringBuilder();
        for (String word : choppedUpString) {
            sb.append(word);
        }
        return sb.toString();
    }
}
