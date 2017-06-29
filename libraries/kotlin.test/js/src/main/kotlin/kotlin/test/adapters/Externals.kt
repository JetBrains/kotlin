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

/**
 * The [QUnit](http://qunitjs.com/) API
 */
external object QUnit {
    fun module(name: String, testFn: () -> Unit): Unit
    fun test(name: String, testFn: (dynamic) -> Unit): Unit
    fun skip(name: String, testFn: (dynamic) -> Unit): Unit
    fun only(name: String, testFn: (dynamic) -> Unit): Unit
}

/**
 * Jasmine/Mocha API
 */
external fun describe(name: String, fn: () -> Unit)
external fun xdescribe(name: String, fn: () -> Unit)
external fun it(name: String, fn: () -> Unit)
external fun xit(name: String, fn: () -> Unit)
/**
 * Jasmine-only syntax for focused spec's. Mocha uses the 'it.only' and 'describe.only' syntax
 */
external fun fit(name: String, fn: () -> Unit)
external fun fdescribe(name: String, fn: () -> Unit)


private fun isFunction(a: String) = js("typeof a === 'function'")

internal fun isQUnit1() = jsTypeOf(QUnit) !== "undefined" && isFunction("ok")

internal fun isQUnit2() = jsTypeOf(QUnit) !== "undefined" && isFunction("QUnit.module.skip") && isFunction("QUnit.module.only")

internal fun isJasmine() = isFunction("describe") && isFunction("it") && isFunction("fit")

internal fun isMocha() = isFunction("describe") && isFunction("it") && isFunction("it.only")
