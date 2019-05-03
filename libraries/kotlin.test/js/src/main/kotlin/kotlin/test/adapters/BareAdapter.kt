/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.adapters

import kotlin.test.FrameworkAdapter

/**
 * A fallback adapter for the case when no framework is detected.
 */
internal open class BareAdapter : FrameworkAdapter {

    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        if (!ignored) {
            suiteFn()
        }
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
        if (!ignored) {
            testFn()
        }
    }
}