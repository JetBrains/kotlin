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

import kotlin.test.assertHook

@JsName("suite")
fun suite(name: String, suiteFn: () -> Unit) {
    currentAdapter.suite(name, suiteFn)
}

@JsName("xsuite")
fun xsuite(name: String, suiteFn: () -> Unit) {
    currentAdapter.xsuite(name, suiteFn)
}

@JsName("fsuite")
fun fsuite(name: String, suiteFn: () -> Unit) {
    currentAdapter.fsuite(name, suiteFn)
}

@JsName("test")
fun test(name: String, testFn: () -> Unit) {
    currentAdapter.test(name, testFn)
}

@JsName("xtest")
fun xtest(name: String, testFn: () -> Unit) {
    currentAdapter.xtest(name, testFn)
}

@JsName("only")
fun ftest(name: String, testFn: () -> Unit) {
    currentAdapter.ftest(name, testFn)
}

internal var currentAdapter: FrameworkAdapter = DefaultAdapters.AUTO_DETECTED

@JsName("setAdapter")
fun setAdapter(adapter: dynamic) {
    if (js("typeof adapter === 'string'")) {
        if (adapter in DefaultAdapters.NAME_TO_ADAPTER) {
            setAdapter(DefaultAdapters.NAME_TO_ADAPTER[adapter])
        }
        else {
            throw IllegalArgumentException("Unsupported test framework adapter: '$adapter'")
        }
    }
    else {
        currentAdapter = adapter
    }
}

@JsName("setAssertHook")
fun setAssertHook(hook: (result: Boolean, expected: Any?, actual: Any?, lazyMessage: () -> String?) -> Unit) {
    assertHook = hook
}

external interface FrameworkAdapter {
    fun suite(name: String, suiteFn: () -> Unit)

    fun xsuite(name: String, suiteFn: () -> Unit)

    fun fsuite(name: String, suiteFn: () -> Unit)

    fun test(name: String, testFn: () -> Unit)

    fun xtest(name: String, testFn: () -> Unit)

    fun ftest(name: String, testFn: () -> Unit)
}


enum class DefaultAdapters : FrameworkAdapter {

    QUNIT {
        override fun suite(name: String, suiteFn: () -> Unit) {
            QUnit.module(name, suiteFn)
        }

        override fun xsuite(name: String, suiteFn: () -> Unit) {
            // QUnit doesn't support ignoring modules, so just don't execute it
        }

        override fun fsuite(name: String, suiteFn: () -> Unit) {
            // QUnit doesn't support focusing on a single module
            QUnit.module(name, suiteFn)
        }

        override fun test(name: String, testFn: () -> Unit) {
            QUnit.test(name, wrapTest(testFn))
        }

        override fun xtest(name: String, testFn: () -> Unit) {
            QUnit.skip(name, wrapTest(testFn))
        }

        override fun ftest(name: String, testFn: () -> Unit) {
            QUnit.only(name, wrapTest(testFn))
        }

        private fun wrapTest(testFn: () -> Unit): (dynamic) -> Unit = { assert ->
            if (js("typeof assert !== 'function'")) {
                assertHook = { result, _, _, msgFn -> assert.ok(result, msgFn()) }
            }
            else {
                assertHook = { result, expected, actual, msgFn ->
                    val data = js("{}")
                    data.result = result
                    data.actual = actual
                    data.expected = expected
                    data.message = msgFn()
                    assert.pushResult(data)
                }
            }
            testFn()
        }
    },

    JASMINE {
        override fun suite(name: String, suiteFn: () -> Unit) {
            describe(name, suiteFn)
        }

        override fun xsuite(name: String, suiteFn: () -> Unit) {
            xdescribe(name, suiteFn)
        }

        override fun fsuite(name: String, suiteFn: () -> Unit) {
            fdescribe(name, suiteFn)
        }

        override fun test(name: String, testFn: () -> Unit) {
            it(name, testFn)
        }

        override fun xtest(name: String, testFn: () -> Unit) {
            xit(name, testFn)
        }

        override fun ftest(name: String, testFn: () -> Unit) {
            fit(name, testFn)
        }
    },

    MOCHA {
        override fun suite(name: String, suiteFn: () -> Unit) {
            describe(name, suiteFn)
        }

        override fun xsuite(name: String, suiteFn: () -> Unit) {
            xdescribe(name, suiteFn)
        }

        override fun fsuite(name: String, suiteFn: () -> Unit) {
            js("describe.only")(name, suiteFn)
        }

        override fun test(name: String, testFn: () -> Unit) {
            it(name, testFn)
        }

        override fun xtest(name: String, testFn: () -> Unit) {
            xit(name, testFn)
        }

        override fun ftest(name: String, testFn: () -> Unit) {
            js("it.only")(name, testFn)
        }
    },

    BARE {
        override fun suite(name: String, suiteFn: () -> Unit) {
            suiteFn()
        }

        override fun xsuite(name: String, suiteFn: () -> Unit) {
            // Do nothing
        }

        override fun fsuite(name: String, suiteFn: () -> Unit) {
            suiteFn()
        }

        override fun test(name: String, testFn: () -> Unit) {
            testFn()
        }

        override fun xtest(name: String, testFn: () -> Unit) {
            // Do nothing
        }

        override fun ftest(name: String, testFn: () -> Unit) {
            testFn()
        }
    };

    companion object {
        val AUTO_DETECTED = when {
            js("typeof QUnit !== 'undefined'") -> QUNIT
            js("typeof describe === 'function' && typeof it === 'function'") -> {
                if (js("typeof xit === 'function'")) JASMINE else MOCHA
            }
            else -> BARE
        }

        val NAME_TO_ADAPTER = mapOf(
                "qunit" to QUNIT,
                "jasmine" to JASMINE,
                "mocha" to MOCHA,
                "auto" to AUTO_DETECTED)
    }
}

/**
 * The [QUnit](http://qunitjs.com/) API
 */
external object QUnit {
    fun module(name: String, testFn: () -> Unit): Unit
    fun test(name: String, testFn: (dynamic) -> Unit): Unit
    fun skip(name: String, testFn: (dynamic) -> Unit): Unit
    fun only(name: String, testFn: (dynamic) -> Unit): Unit
}

// Old Qunit API
external fun ok(result: Boolean, message: String?)

/**
 * Jasmine/Mocha API
 */
external fun describe(name: String, fn: () -> Unit)
external fun xdescribe(name: String, fn: () -> Unit)
external fun fdescribe(name: String, fn: () -> Unit)

external fun it(name: String, fn: () -> Unit)
external fun xit(name: String, fn: () -> Unit)
external fun fit(name: String, fn: () -> Unit)