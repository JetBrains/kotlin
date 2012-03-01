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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.utils.GenerationUtils;
import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.Scriptable;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Talanov
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
        String trimmedExpected = trimSpace(expectedResult);
        String trimmedActual = trimSpace(result);
        // System.out.println(trimmedActual);
        // System.out.println(trimmedExpected);
        assertTrue("Returned:\n" + trimmedActual + "END_OF_RETURNED\nExpected:\n" + trimmedExpected
                   + "END_OF_EXPECTED\n", trimmedExpected.equals(trimmedActual));
    }

    private String getSystemOutput(@NotNull Context context, @NotNull Scriptable scope) {
        Object output = context.evaluateString(scope, "Kotlin.System.output()", "test", 0, null);
        assertTrue("Output should be a string.", output instanceof String);
        return (String) output;
    }

    private void runMain(Context context, Scriptable scope) {
        String callToMain = GenerationUtils.generateCallToMain("Anonymous", arguments);
        context.evaluateString(scope, callToMain, "function call", 0, null);
    }

    public String trimSpace(String s) {
        String[] choppedUpString = s.trim().split("\\s");
        StringBuilder sb = new StringBuilder();
        for (String word : choppedUpString) {
            sb.append(word);
        }
        return sb.toString();
    }
}
