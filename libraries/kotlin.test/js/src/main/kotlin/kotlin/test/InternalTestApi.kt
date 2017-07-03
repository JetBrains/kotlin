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