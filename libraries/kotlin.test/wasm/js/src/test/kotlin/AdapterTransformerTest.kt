/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*

private fun wrapAdapter(externalAdapter: ExternalFrameworkAdapter): ExternalFrameworkAdapter = js("""{
    return {
        suite: (name, ignored, suiteFn) => externalAdapter.suite(name, ignored, suiteFn),
        test: (name, ignored, testFn) => externalAdapter.test(name, ignored, () => 'overridden')
    };
}""")

private class TestAdapter : FrameworkAdapter {
    var testResult: Any? = null
    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) = suiteFn()
    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) { testResult = testFn() }
}

class AdapterTransformerTest {
    @Test
    fun loopAround() {
        val frameworkAdapter = TestAdapter()

        val adapter = frameworkAdapter.externalize().internalize()
        adapter.test("someTest", false) { "abc".toJsString() }
        check(frameworkAdapter.testResult !is String)
        check(frameworkAdapter.testResult.toString() == "abc")

        adapter.test("someTest", false) { "abc" }
        check(frameworkAdapter.testResult is String)
        check(frameworkAdapter.testResult == "abc")

        adapter.test("someTest", false) { 42 }
        check(frameworkAdapter.testResult is Int)
        check(frameworkAdapter.testResult == 42)
    }

    @Test
    fun wrappedAdapter() {
        val adapter = TestAdapter()

        val roundTripAdapter = wrapAdapter(adapter.externalize()).internalize()
        roundTripAdapter.test("someTest", false) { "abc".toJsString() }
        check(adapter.testResult !is String)
        check(adapter.testResult.toString() == "overridden")
    }
}