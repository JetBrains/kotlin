/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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