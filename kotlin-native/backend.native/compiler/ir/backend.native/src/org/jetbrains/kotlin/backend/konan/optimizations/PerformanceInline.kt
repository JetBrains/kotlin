/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.llvm.BasicLlvmHelpers
import org.jetbrains.kotlin.backend.konan.llvm.isDefinition
import org.jetbrains.kotlin.backend.konan.llvm.setFunctionAlwaysInline

// declared in Common.h
private const val PERFORMANCE_INLINE_ANNOTATION = "performance_inline"

internal fun handlePerformanceInlineAnnotation(config: KonanConfig, llvm: BasicLlvmHelpers) {
    if (!config.smallBinary) {
        val toInline = llvm.runtimeAnnotationMap[PERFORMANCE_INLINE_ANNOTATION] ?: return
        toInline.filter { it.isDefinition() }.forEach { setFunctionAlwaysInline(it) }
    }
}
