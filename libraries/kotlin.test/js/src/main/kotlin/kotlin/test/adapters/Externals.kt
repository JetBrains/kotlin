/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.adapters

/**
 * The [QUnit](http://qunitjs.com/) API
 */
internal external object QUnit {
    fun module(name: String, suiteFn: () -> Unit): Unit
    fun test(name: String, testFn: (dynamic) -> Any?): Unit
    fun skip(name: String, testFn: (dynamic) -> Any?): Unit
}

/*
 * Jasmine/Mocha/Jest API
 */

internal external fun describe(name: String, fn: () -> Unit)
internal external fun xdescribe(name: String, fn: () -> Unit)
internal external fun it(name: String, fn: () -> Any?)
internal external fun xit(name: String, fn: () -> Any?)

internal fun isQUnit() = js("typeof QUnit !== 'undefined'")

internal fun isJasmine() = js("typeof describe === 'function' && typeof it === 'function'")