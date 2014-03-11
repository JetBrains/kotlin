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

/**
 */
public class StdLibJsArrayQUnitTest extends StdLibQUnitTestSupport {
    public void testDummy() {
    }

    // TODO for some reason this test fails when ran as part of all the js-backend tests, but works stand alone
    // generates: ReferenceError: "QUnit" is not defined. (js/js.translator/testData/stdlib/out/ArrayQUnitTest.compiler_v3.js#3058)
    // when ran in batch

    public void DISABLED_testArrayQUnitTest() throws Exception {
        performStdLibTest(DEFAULT_ECMA_VERSIONS,
                          "libraries/stdlib/test",
                          "js/JsArrayTest.kt");
    }
}
