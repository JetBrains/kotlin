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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Arrays;

/**
 * @author Pavel Talanov
 */
public final class KotlinLibTest extends TranslationTest {

    final private static String MAIN = "kotlinLib/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testKotlinJsLibRunsWithRhino() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoResultChecker() {
            @Override
            public void runChecks(Context context, Scriptable scope) throws Exception {
                //do nothing
            }
        });
    }

    public void testNamespaceHasDeclaredFunction() throws Exception {
        runJavascriptTest("namespace.js");
    }


    public void testNamespaceHasDeclaredClasses() throws Exception {
        runJavascriptTest("namespaceWithClasses.js");
    }


    public void testIsSameType() throws Exception {
        runJavascriptTest("isSameType.js");
    }


    public void testIsAncestorType() throws Exception {
        runJavascriptTest("isAncestorType.js");
    }


    public void testIsComplexTest() throws Exception {
        runJavascriptTest("isComplexTest.js");
    }


    public void testCommaExpression() throws Exception {
        runJavascriptTest("commaExpression.js");
    }

    public void testArray() throws Exception {
        runJavascriptTest("array.js");
    }


    public void testHashMap() throws Exception {
        runJavascriptTest("hashMap.js");
    }


    private void runJavascriptTest(@NotNull String filename) throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases(filename)),
                     new RhinoFunctionResultChecker("test", true));
    }

    @Override
    protected boolean shouldCreateOut() {
        return false;
    }

}
