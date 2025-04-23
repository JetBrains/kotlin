/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0

object KtErrorsNoArg {
    val NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS by warning0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val NOARG_ON_INNER_CLASS_ERROR by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val NOARG_ON_LOCAL_CLASS_ERROR by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)

    init {
        RootDiagnosticRendererFactory.registerFactory(DefaultErrorMessagesNoArg)
    }
}

object DefaultErrorMessagesNoArg : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap = KtDiagnosticFactoryToRendererMap("NoArg").apply {
        put(KtErrorsNoArg.NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS, "Zero-argument constructor was not found in the superclass")
        put(KtErrorsNoArg.NOARG_ON_INNER_CLASS_ERROR, "Noarg constructor generation is not possible for inner classes")
        put(KtErrorsNoArg.NOARG_ON_LOCAL_CLASS_ERROR, "Noarg constructor generation is not possible for local classes")
    }
}
