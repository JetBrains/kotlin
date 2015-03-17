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

package org.jetbrains.kotlin.js.test.semantics;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.test.rhino.RhinoResultChecker;

/**
 */
public class StdLibTestToJSTest extends StdLibQUnitTestSupport {
    public void testGenerateTestCase() throws Exception {
        performStdLibTest(DEFAULT_ECMA_VERSIONS,
                          "libraries/stdlib/test",
                          "js/JsArrayTest.kt",
                          "js/MapJsTest.kt",
                          "GetOrElseTest.kt",
                          "collections/ListSpecificTest.kt",
                          "collections/IteratorsTest.kt",
                          "text/StringTest.kt",
                          // TODO review: somethings FAILED if run:
                          "js/JsDomTest.kt",
                          "dom/DomTest.kt",
                          "collections/SequenceTest.kt",
                          "collections/IterableTests.kt",
                          "language/RangeTest.kt",
                          "language/RangeIterationTest.kt"
        );
    }

    @Nullable
    @Override
    protected RhinoResultChecker getResultChecker() {
        // don't run, it's just smoke test this tests should be run in maven build.
        return null;
    }
}
