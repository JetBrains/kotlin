/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.test.rhino.CompositeRhinoResultsChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoResultChecker;

public class StdLibJsArrayScriptTest extends StdLibTestBase {
    public void testArrayScriptTest() throws Exception {
        performStdLibTest(DEFAULT_ECMA_VERSIONS, "libraries/stdlib/test", "js/JsArrayScript.kt");
    }

    @NotNull
    @Override
    protected RhinoResultChecker getResultChecker() {
        return new CompositeRhinoResultsChecker(
                new RhinoFunctionResultChecker(TEST_MODULE, "test.collections.js", "testSize", 3.0),
                new RhinoFunctionResultChecker(TEST_MODULE, "test.collections.js", "testToListToString", "[]-[foo]-[foo, bar]")
        );
    }
}
