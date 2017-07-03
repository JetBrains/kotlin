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

package kotlin.test

import kotlin.test.adapters.*

/**
 * Overrides the framework adapter. Use in order to add support to a custom test framework.
 *
 * Also some string arguments are supported. Use "qunit" to set the adapter to [QUnit](https://qunitjs.com/), "mocha" for
 * [Mocha](https://mochajs.org/), "jest" for [Jest](https://facebook.github.io/jest/),
 * "jasmine" for [Jasmine](https://github.com/jasmine/jasmine), and "auto" to detect one of those frameworks automatically.
 *
 * If this function is not called, the test framework will be detected automatically (as if "auto" was passed).
 *
 */
@JsName("setAdapter")
public fun setAdapter(adapter: dynamic) {
    if (js("typeof adapter === 'string'")) {
        NAME_TO_ADAPTER[adapter]?.let {
            setAdapter(it.invoke())
        }?: throw IllegalArgumentException("Unsupported test framework adapter: '$adapter'")
    }
    else {
        currentAdapter = adapter
    }
}

/**
 * Use in order to define which action should be taken by the test framework on the testResult.
 */
@JsName("setAssertHook")
public fun setAssertHook(hook: (TestResult) -> Unit) {
    assertHook = hook
}

@JsName("suite")
internal fun suite(name: String, suiteFn: () -> Unit) {
    currentAdapter.suite(name, suiteFn)
}

@JsName("xsuite")
internal fun xsuite(name: String, suiteFn: () -> Unit) {
    currentAdapter.xsuite(name, suiteFn)
}

@JsName("fsuite")
internal fun fsuite(name: String, suiteFn: () -> Unit) {
    currentAdapter.fsuite(name, suiteFn)
}

@JsName("test")
internal fun test(name: String, testFn: () -> Unit) {
    currentAdapter.test(name, testFn)
}

@JsName("xtest")
internal fun xtest(name: String, testFn: () -> Unit) {
    currentAdapter.xtest(name, testFn)
}

@JsName("ftest")
internal fun ftest(name: String, testFn: () -> Unit) {
    currentAdapter.ftest(name, testFn)
}

internal var currentAdapter: FrameworkAdapter = detectAdapter()

internal fun detectAdapter() = when {
    isQUnit() -> QUnitAdapter()
    isJasmine() -> JasmineAdapter()
    isMocha() -> MochaAdapter()
    else -> BareAdapter()
}

internal val NAME_TO_ADAPTER: Map<String, () -> FrameworkAdapter> = mapOf(
        "qunit" to ::QUnitAdapter,
        "jasmine" to ::JasmineAdapter,
        "jest" to ::JasmineAdapter, // Jest support both Mocha- and Jasmine-style test declarations.
        "mocha" to ::MochaAdapter,
        "auto" to ::detectAdapter)