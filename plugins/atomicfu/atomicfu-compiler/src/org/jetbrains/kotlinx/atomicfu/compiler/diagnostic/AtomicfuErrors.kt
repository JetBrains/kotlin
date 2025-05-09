/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.psi.KtProperty

object AtomicfuErrors : KtDiagnosticsContainer() {
    val PUBLIC_ATOMICS_ARE_FORBIDDEN by error1<KtProperty, String>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val PUBLISHED_API_ATOMICS_ARE_FORBIDDEN by error1<KtProperty, String>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val ATOMIC_PROPERTIES_SHOULD_BE_VAL by error1<KtProperty, String>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = AtomicfuErrorMessages
}
