/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.wasm.WasmExport

internal expect fun adapter(): FrameworkAdapter

/**
 * The functions below are used by the compiler to describe the tests structure, e.g.
 * fun startUnitTests() {
 *     `declare test fun`()
 *     runRootSuites()
 * }
 *
 * fun `declare test fun`() {
 *     registerRootSuiteBlock("top-level-package1") {
 *         suite("TestClass1", ignored = false) {
 *             suite("a subsuite", ignored = false) {
 *                 test("a test", ignored = false) {...}
 *                 test("an ignored/pending test", ignored = true) {...}
 *             }
 *             suite("an ignored/pending test", ignored = true) {...}
 *         }
 *     }
 * }
 */

internal fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
    adapter().suite(name, ignored, suiteFn)
}

internal fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
    adapter().test(name, ignored, testFn)
}

private val rootSuiteBlocks: MutableMap<String, MutableList<() -> Unit>> = mutableMapOf()

internal fun registerRootSuiteBlock(suiteName: String, block: () -> Unit) {
    rootSuiteBlocks.getOrPut(suiteName, ::mutableListOf).add(block)
}

internal fun runRootSuites() {
    rootSuiteBlocks.entries.forEach { (suiteName, block) ->
        suite(name = suiteName, ignored = false) {
            block.forEach { it() }
        }
    }
}