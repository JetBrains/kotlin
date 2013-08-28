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

import closurecompiler.internal.com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.rhino.CompositeRhinoResultsChecker;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoResultChecker;

import java.util.Map;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;

/**
 */
public class StdLibJsArrayScriptTest extends StdLibTestBase {
    public void testArrayScriptTest() throws Exception {
        performStdLibTest(DEFAULT_ECMA_VERSIONS,
                          "libraries/stdlib/test",
                          "js/JsArrayScript.kt");
    }

    @Override
    protected void performChecksOnGeneratedJavaScript(String path, EcmaVersion version) throws Exception {
        Map<String, Object> variables = Maps.newHashMap();
        String moduleId = moduleIdFromOutputFile(path);
        RhinoResultChecker checker = new CompositeRhinoResultsChecker(
                new RhinoFunctionResultChecker(moduleId, "jstest", "testSize", 3.0),
                new RhinoFunctionResultChecker(moduleId, "jstest", "testToListToString", "[]-[foo]-[foo, bar]")
        );
        runRhinoTest(Lists.newArrayList(path), checker, variables, version);

        /*
        RhinoResultChecker checker = new RhinoFunctionResultChecker(moduleId, "jstest", "runTest", 2.0) {

            @Override
            protected String functionCallString() {
                //return "QUnit.test('JsArrayTest.arraySizeAndToList', function(){ (new Kotlin.modules['" + moduleId + "'].jstest.JsArrayTest).arraySizeAndToList(); });";
                return "(new Kotlin.modules['" + moduleId + "'].jstest.JsArrayTest).arraySizeAndToList();";
            }
        };
        runRhinoTest(Lists.newArrayList(path),
                     checker, variables, version,
                     Lists.newArrayList("js/js.translator/qunit/qunit.js"));
        */
    }
}
