/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import java.io.File

internal fun createK1HeaderGenerator(): HeaderGenerator {
    val disposable = Disposer.newDisposable("K1Generator")
    val clazz = Class.forName("org.jetbrains.kotlin.backend.konan.testUtils.Fe10HeaderGeneratorImpl")
    val constructor = clazz.getConstructor(Disposable::class.java)
    val generator = constructor.newInstance(disposable) as HeaderGenerator

    return object : HeaderGenerator by generator {
        private var disposed = false
        override fun generateHeaders(
            root: File,
            configuration: HeaderGenerator.Configuration,
        ): ObjCHeader {
            try {
                return generator.generateHeaders(root, configuration)
            } finally {
                if (!disposed) {
                    disposeRootInWriteAction(disposable)
                    disposed = true
                }
            }
        }
    }
}