/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse

internal open class TestDisposable(parentDisposable: Disposable?) : Disposable {
    init {
        if (parentDisposable != null) {
            @Suppress("LeakingThis")
            Disposer.register(parentDisposable, this)
        }
    }

    @Volatile
    private var disposed = false

    final override fun dispose() {
        disposed = true
    }

    fun assertNotDisposed() {
        assertFalse(disposed) { "Already disposed: ${this::class.java}, $this" }
    }
}
