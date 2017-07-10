/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.test.adapters

import kotlin.test.FrameworkAdapter
import kotlin.test.assertHook
import kotlin.test.assertTrue

/**
 * [QUnit](http://qunitjs.com/) adapter
 */
internal class QUnitAdapter : FrameworkAdapter {
    var ignoredSuite = false;

    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        val prevIgnore = ignoredSuite
        ignoredSuite = ignoredSuite or ignored
        QUnit.module(name, suiteFn)
        ignoredSuite = prevIgnore
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> Unit) {
        if (ignored or ignoredSuite) {
            QUnit.skip(name, wrapTest(testFn))
        }
        else {
            QUnit.test(name, wrapTest(testFn))
        }
    }

    private fun wrapTest(testFn: () -> Unit): (dynamic) -> Unit = { assert ->
        var assertionsHappened = false
        assertHook = { testResult ->
            assertionsHappened = true
            assert.ok(testResult.result, testResult.lazyMessage())
        }
        testFn()
        if (!assertionsHappened) {
            assertTrue(true, "A test with no assertions is considered successful")
        }
    }
}