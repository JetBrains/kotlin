/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtProperty

object AtomicfuErrors {
    val PUBLIC_ATOMICS_ARE_FORBIDDEN = KtDiagnosticFactory1<String>("PUBLIC_ATOMICS_ARE_FORBIDDEN",
        Severity.ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtProperty::class)

    // This diagnostic should be upgraded to error in the next release: KT-70982
    val PUBLISHED_API_ATOMICS_ARE_FORBIDDEN = KtDiagnosticFactory1<String>("PUBLISHED_API_ATOMICS_ARE_FORBIDDEN",
        Severity.WARNING, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtProperty::class)

    val ATOMIC_PROPERTIES_SHOULD_BE_VAL = KtDiagnosticFactory1<String>("ATOMIC_PROPERTIES_SHOULD_BE_VAL",
        Severity.ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtProperty::class)

    init {
        RootDiagnosticRendererFactory.registerFactory(AtomicfuErrorMessages)
    }
}