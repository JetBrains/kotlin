/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.js.JsExport

/**
 * Overrides current framework adapter with a provided instance of [FrameworkAdapter]. Use in order to support custom test frameworks.
 *
 * If this function is not called, the test framework will be detected automatically.
 *
 */
internal fun setAdapter(adapter: FrameworkAdapter) {
    currentAdapter = adapter
}

/**
 * The functions below are used by the compiler to describe the tests structure, e.g.
 *
 * suite('a suite', false, function() {
 *   suite('a subsuite', false, function() {
 *     test('a test', false, function() {...});
 *     test('an ignored/pending test', true, function() {...});
 *   });
 *   suite('an ignored/pending test', true, function() {...});
 * });
 */

internal fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
    currentAdapter.suite(name, ignored, suiteFn)
}

internal fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
    currentAdapter.test(name, ignored, testFn)
}

internal var currentAdapter: FrameworkAdapter = TeamcityAdapter()

// This is called from the js-launcher alongside wasm start function
@JsExport
@OptIn(kotlin.js.ExperimentalJsExport::class)
internal fun startUnitTests() {
    // This will be filled with the corresponding code during lowering
}
