/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object KtErrorsNoArg : KtDiagnosticsContainer() {
    val NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS by error0<KtElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val NOARG_ON_INNER_CLASS_ERROR by error0<KtElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val NOARG_ON_LOCAL_CLASS_ERROR by error0<KtElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = DefaultErrorMessagesNoArg
}

object DefaultErrorMessagesNoArg : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("NoArg") { map ->
        map.put(KtErrorsNoArg.NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS, "Zero-argument constructor was not found in the superclass")
        map.put(KtErrorsNoArg.NOARG_ON_INNER_CLASS_ERROR, "Noarg constructor generation is not possible for inner classes")
        map.put(KtErrorsNoArg.NOARG_ON_LOCAL_CLASS_ERROR, "Noarg constructor generation is not possible for local classes")
    }
}
