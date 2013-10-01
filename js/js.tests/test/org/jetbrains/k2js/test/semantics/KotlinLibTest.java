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

package org.jetbrains.k2js.test.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;

public final class KotlinLibTest extends SingleFileTranslationTest {

    public KotlinLibTest() {
        super("kotlinLib/");
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

    public void testArray() throws Exception {
        runJavascriptTest("array.js");
    }


    public void testHashMap() throws Exception {
        runJavascriptTest("hashMap.js");
    }


    private void runJavascriptTest(@NotNull String filename) throws Exception {
        runRhinoTest(withAdditionalFiles(cases(filename), EcmaVersion.defaultVersion()),
                     new RhinoFunctionResultChecker("test", true));
    }

    @Override
    protected boolean shouldCreateOut() {
        return false;
    }

}
