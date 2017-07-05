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

/**
 * A fallback adapter for the case when no framework is detected.
 */
internal open class BareAdapter : FrameworkAdapter {

    internal open fun runTest(testFn: () -> kotlin.Unit,
                              nameStack: Iterable<String>,
                              ignored: Boolean,
                              focused: Boolean,
                              shouldRun: Boolean) {
        if (shouldRun) {
            testFn()
        }
    }

    override fun suite(name: String, suiteFn: () -> Unit) = visitSuite(name, suiteFn, TestState.DEFAULT)

    override fun xsuite(name: String, suiteFn: () -> Unit) = visitSuite(name, suiteFn, TestState.IGNORED)

    override fun fsuite(name: String, suiteFn: () -> Unit) = visitSuite(name, suiteFn, TestState.FOCUSED)

    override fun test(name: String, testFn: () -> Unit) = visitTest(name, testFn, TestState.DEFAULT)

    override fun xtest(name: String, testFn: () -> Unit) = visitTest(name, testFn, TestState.IGNORED)

    override fun ftest(name: String, testFn: () -> Unit) = visitTest(name, testFn, TestState.FOCUSED)

    private fun visitSuite(name: String, suiteFn: () -> Unit, state: TestState) {
        val prevList = testList
        val nextList = mutableListOf<Testable>()
        testList = nextList

        suiteFn()

        val suite = Suite(name, state, nextList)

        if (prevList == null) {
            suite.runTest()
        } else {
            prevList.add(suite)
            testList = prevList
        }
    }

    private fun visitTest(name: String, testFn: () -> Unit, state: TestState) {
        val test = Test(name, state, testFn)
        testList?.add(test) ?: test.runTest()
    }

    private var testList: MutableList<Testable>? = null

    internal enum class TestState { DEFAULT, IGNORED, FOCUSED }

    private class NameStack(val name: String, val next: NameStack?) : Iterable<String> {
        override fun iterator(): Iterator<String> {
            return object : AbstractIterator<String>() {
                private var head: NameStack? = this@NameStack

                override fun computeNext() {
                    val next = head?.name
                    head = head?.next
                    if (next != null) {
                        setNext(next)
                    }
                    else {
                        done()
                    }
                }
            }
        }
    }

    private interface Testable {
        val name: String
        val state: TestState
        val focused: Boolean
        fun runTest(names: NameStack? = null,
                    ignored: Boolean = false,
                    focused: Boolean = false,
                    shouldRun: Boolean = true)
    }

    private inner class Suite(override val name: String, override val state: TestState, val tests: List<Testable>) : Testable {
        private val focusedSubtests = tests.any { it.focused }

        override val focused: Boolean = state == TestState.FOCUSED || focusedSubtests

        override fun runTest(names: NameStack?,
                             ignored: Boolean,
                             focused: Boolean,
                             shouldRun: Boolean) {
            tests.forEach { test ->
                test.runTest(NameStack(name, names),
                        ignored || state == TestState.IGNORED,
                        test.focused || focused && !focusedSubtests,
                        shouldRun && !ignored && (test.focused || !focusedSubtests))
            }
        }
    }

    private inner class Test(override val name: String, override val state: TestState, val testFn: () -> Unit) : Testable {
        override val focused: Boolean
            get() = state == TestState.FOCUSED

        override fun runTest(names: NameStack?,
                             ignored: Boolean,
                             focused: Boolean,
                             shouldRun: Boolean) {
            runTest(testFn,
                    NameStack(name, names),
                    ignored || state == TestState.IGNORED,
                    focused,
                    shouldRun)
        }
    }
}