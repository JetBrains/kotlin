/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

internal external interface ExternalFrameworkAdapter : JsAny {
    fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit)
    fun test(name: String, ignored: Boolean, testFn: () -> JsAny?)
}

private fun createExternalFrameworkAdapter(
    suiteFn: (String, Boolean, () -> Unit) -> Unit,
    testFn: (String, Boolean, () -> JsAny?) -> Unit
): ExternalFrameworkAdapter = js("{ return { suite: suiteFn, test: testFn } }")

private class ExternalAdapterWrapper(private val externalAdapter: ExternalFrameworkAdapter) : FrameworkAdapter {
    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) = externalAdapter.suite(name, ignored, suiteFn)
    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) = externalAdapter.test(name, ignored, { testFn()?.toJsReference() })
}

internal fun FrameworkAdapter.externalize(): ExternalFrameworkAdapter =
    createExternalFrameworkAdapter(
        suiteFn = this::suite,
        testFn = { name, ignored, testFn -> this.test(name, ignored, { testFn() }) }
    )

internal fun ExternalFrameworkAdapter.internalize(): FrameworkAdapter = ExternalAdapterWrapper(this)
