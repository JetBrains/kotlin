/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class KtInlineSourceCommonizerTestCase : InlineSourceBuilderDelegate {
    private lateinit var testRootDisposable: Disposable

    @BeforeEach
    fun setUp() {
        testRootDisposable = object : Disposable.Default {}
    }

    @AfterEach
    fun tearDown() {
        Disposer.dispose(testRootDisposable)
    }

    override val inlineSourceBuilder: InlineSourceBuilder by lazy { InlineSourceBuilderImpl(testRootDisposable) }
}
