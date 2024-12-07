/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.wasm.WasmExport

internal expect fun adapter(): FrameworkAdapter

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
    adapter().suite(name, ignored, suiteFn)
}

internal fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
    adapter().test(name, ignored, testFn)
}

// This is called from the js-launcher alongside wasm start function
// TODO: Remove after bootstrap
@WasmExport
internal fun startUnitTests() {
    // This will be filled with the corresponding code during lowering
}
