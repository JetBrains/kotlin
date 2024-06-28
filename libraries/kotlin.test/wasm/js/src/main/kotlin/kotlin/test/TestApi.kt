/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

private fun isJasmine(): Boolean =
    js("typeof describe === 'function' && typeof it === 'function'")

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

@JsName("kotlinTest")
internal external val kotlinTestNamespace: ExternalKotlinTestNamespace?

internal external interface ExternalKotlinTestNamespace : JsAny {
    public val adapterTransformer: ((ExternalFrameworkAdapter) -> ExternalFrameworkAdapter)?
}

internal actual fun adapter(): FrameworkAdapter {
    currentAdapter?.let { return it }

    val adapter: FrameworkAdapter
    if (isJasmine()) {
        val jasmineLikeAdapter = JasmineLikeAdapter()
        val transformer = kotlinTestNamespace?.adapterTransformer
        if (transformer != null) {
            adapter = transformer(jasmineLikeAdapter.externalize()).internalize()
        } else {
            adapter = jasmineLikeAdapter
        }
    } else {
        adapter = TeamcityAdapterWithPromiseSupport()
    }
    currentAdapter = adapter
    return adapter
}