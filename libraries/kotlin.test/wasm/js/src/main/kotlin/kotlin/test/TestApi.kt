/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

/**
 * Overrides current framework adapter with a provided instance of [FrameworkAdapter]. Use in order to support custom test frameworks.
 *
 * If this function is not called, the test framework will be detected automatically.
 *
 */
internal fun setAdapter(adapter: FrameworkAdapter) {
    currentAdapter = adapter
}

internal var currentAdapter: FrameworkAdapter? = null

private fun isJasmine(): Boolean =
    js("typeof describe === 'function' && typeof it === 'function'")

internal fun adapter(): FrameworkAdapter {
    val result = currentAdapter ?: if (isJasmine()) JasmineLikeAdapter() else TeamcityAdapterWithPromiseSupport()
    currentAdapter = result
    return result
}